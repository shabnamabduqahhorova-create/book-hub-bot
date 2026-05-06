package com.bookhub.service;

import com.bookhub.config.TelegramBotProperties;
import com.bookhub.domain.Book;
import com.bookhub.domain.ReadingProgress;
import com.bookhub.domain.User;
import com.bookhub.domain.enums.BotState;
import com.bookhub.domain.enums.ReadingStatus;
import com.bookhub.repository.BookRepository;
import com.bookhub.repository.ReadingProgressRepository;
import com.bookhub.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final int MAX_BOOKS_IN_MESSAGE = 10;

    private final TelegramBotProperties properties;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Map<Long, Long> pendingPageBookIds = new ConcurrentHashMap<>();
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private long updateOffset = 0;
    private boolean disabledWarningLogged = false;

    public TelegramBotService(
            TelegramBotProperties properties,
            UserRepository userRepository,
            BookRepository bookRepository,
            ReadingProgressRepository readingProgressRepository,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.readingProgressRepository = readingProgressRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.apiUrl()).build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBot() {
        if (!properties.enabled() || !properties.hasToken()) {
            logDisabledWarningOnce();
            return;
        }

        try {
            JsonNode bot = restClient.get()
                    .uri("/{token}/getMe", tokenPath())
                    .retrieve()
                    .body(JsonNode.class);
            if (bot == null || !bot.path("ok").asBoolean(false)) {
                log.warn("Telegram bot token check failed");
                return;
            }

            restClient.post()
                    .uri("/{token}/deleteWebhook", tokenPath())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram bot polling is enabled for @{}", bot.path("result").path("username").asText("unknown"));
        } catch (RestClientException ex) {
            log.warn("Telegram bot initialization failed: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.telegram.poll-delay-ms:1500}")
    public void pollUpdates() {
        if (!properties.enabled() || !properties.hasToken()) {
            logDisabledWarningOnce();
            return;
        }
        if (!polling.compareAndSet(false, true)) {
            return;
        }
        try {
            JsonNode response = restClient.get()
                    .uri("/{token}/getUpdates?offset={offset}&timeout=20", tokenPath(), updateOffset)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.path("ok").asBoolean(false)) {
                log.warn("Telegram getUpdates returned a non-ok response");
                return;
            }

            for (JsonNode update : response.path("result")) {
                updateOffset = Math.max(updateOffset, update.path("update_id").asLong() + 1);
                handleUpdate(update);
            }
        } catch (RestClientException ex) {
            log.warn("Telegram polling failed: {}", ex.getMessage());
        } finally {
            polling.set(false);
        }
    }

    private void handleUpdate(JsonNode update) {
        if (update.hasNonNull("callback_query")) {
            handleCallback(update.path("callback_query"));
            return;
        }

        JsonNode message = update.path("message");
        if (message.isMissingNode() || message.path("chat").path("id").isMissingNode()) {
            return;
        }

        long chatId = message.path("chat").path("id").asLong();
        User user = upsertUser(message.path("from"));
        String lang = normalizeLanguage(user.getLanguage());
        String text = message.path("text").asText("").trim();

        if (user.getBotState() == BotState.AWAITING_SEARCH && !text.startsWith("/")) {
            user.setBotState(BotState.NONE);
            userRepository.save(user);
            sendBooks(chatId, bookRepository.search(text), lang);
            return;
        }

        if (user.getBotState() == BotState.AWAITING_PAGE && !text.startsWith("/")) {
            saveCurrentPage(chatId, user, text, lang);
            return;
        }

        if (text.startsWith("/start")) {
            user.setBotState(BotState.NONE);
            userRepository.save(user);
            sendMessage(chatId, tr(lang, "start"), mainMenu(lang));
        } else if (text.startsWith("/help") || matches(text, "help") || matches(text, "commands")) {
            sendMessage(chatId, tr(lang, "commands_text"), mainMenu(lang));
        } else if (text.startsWith("/books") || matches(text, "books")) {
            sendBooks(chatId, bookRepository.search(null), lang);
        } else if (matches(text, "my_books")) {
            sendMyBooks(chatId, user, lang);
        } else if (matches(text, "search")) {
            user.setBotState(BotState.AWAITING_SEARCH);
            userRepository.save(user);
            sendMessage(chatId, tr(lang, "search_prompt"), mainMenu(lang));
        } else if (matches(text, "language")) {
            user.setBotState(BotState.AWAITING_LANGUAGE);
            userRepository.save(user);
            sendMessage(chatId, tr(lang, "language_prompt"), languageMenu());
        } else if (setLanguageIfSelected(user, text)) {
            sendMessage(chatId, tr(user.getLanguage(), "language_saved"), mainMenu(user.getLanguage()));
        } else {
            sendMessage(chatId, tr(lang, "unknown"), mainMenu(lang));
        }
    }

    private void handleCallback(JsonNode callback) {
        String callbackId = callback.path("id").asText();
        String data = callback.path("data").asText("");
        long chatId = callback.path("message").path("chat").path("id").asLong();
        User user = upsertUser(callback.path("from"));
        String lang = normalizeLanguage(user.getLanguage());

        if (data.startsWith("lang:")) {
            user.setLanguage(normalizeLanguage(data.substring("lang:".length())));
            user.setBotState(BotState.NONE);
            userRepository.save(user);
            answerCallback(callbackId);
            sendMessage(chatId, tr(user.getLanguage(), "language_saved"), mainMenu(user.getLanguage()));
        } else if (data.startsWith("page:")) {
            answerCallback(callbackId);
            startPageInput(chatId, user, parseId(data, "page:"), lang);
        } else if (data.startsWith("pdf:")) {
            answerCallback(callbackId);
            sendPdf(chatId, parseId(data, "pdf:"), lang);
        } else if (data.startsWith("done:")) {
            answerCallback(callbackId);
            markFinished(chatId, user, parseId(data, "done:"), lang);
        }
    }

    private User upsertUser(JsonNode from) {
        long telegramUserId = from.path("id").asLong();
        User user = userRepository.findByTelegramUserId(telegramUserId).orElseGet(User::new);
        user.setTelegramUserId(telegramUserId);
        user.setUsername(blankToNull(from.path("username").asText(null)));
        user.setFirstName(defaultIfBlank(from.path("first_name").asText(null), "Telegram user"));
        user.setLastName(blankToNull(from.path("last_name").asText(null)));
        if (user.getLanguage() == null || user.getLanguage().isBlank()) {
            user.setLanguage(normalizeLanguage(from.path("language_code").asText("uz")));
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(OffsetDateTime.now());
        }
        return userRepository.save(user);
    }

    private void sendBooks(long chatId, List<Book> books, String lang) {
        if (books.isEmpty()) {
            sendMessage(chatId, tr(lang, "no_books"), mainMenu(lang));
            return;
        }

        int count = Math.min(books.size(), MAX_BOOKS_IN_MESSAGE);
        for (int i = 0; i < count; i++) {
            sendBook(chatId, books.get(i), lang);
        }
        if (books.size() > MAX_BOOKS_IN_MESSAGE) {
            sendMessage(chatId, tr(lang, "more_books").formatted(books.size() - MAX_BOOKS_IN_MESSAGE), mainMenu(lang));
        }
    }

    private void sendBook(long chatId, Book book, String lang) {
        String caption = bookCaption(book, lang) + "\n\n" + pdfStatus(book, lang);
        Map<String, Object> actions = bookActions(book, lang);
        String cover = book.getCoverImagePath();
        if (cover == null || cover.isBlank()) {
            sendMessage(chatId, caption, actions);
            return;
        }

        try {
            if (cover.startsWith("http://") || cover.startsWith("https://")) {
                restClient.post()
                        .uri("/{token}/sendPhoto", tokenPath())
                        .body(Map.of("chat_id", chatId, "photo", cover, "caption", caption, "reply_markup", actions))
                        .retrieve()
                        .toBodilessEntity();
                return;
            }

            Path coverPath = fileStorageService.toAbsolutePath(cover);
            if (Files.exists(coverPath)) {
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("chat_id", String.valueOf(chatId));
                body.add("caption", caption);
                body.add("photo", new FileSystemResource(coverPath));
                body.add("reply_markup", toJson(actions));
                restClient.post()
                        .uri("/{token}/sendPhoto", tokenPath())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                return;
            }
        } catch (RestClientException ex) {
            log.warn("Telegram sendPhoto failed: {}", ex.getMessage());
        }
        sendMessage(chatId, caption, actions);
    }

    private void sendMyBooks(long chatId, User user, String lang) {
        List<ReadingProgress> progresses = readingProgressRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        if (progresses.isEmpty()) {
            sendMessage(chatId, tr(lang, "no_my_books"), mainMenu(lang));
            return;
        }

        sendMessage(chatId, tr(lang, "my_books_title"), mainMenu(lang));
        for (ReadingProgress progress : progresses) {
            sendBook(chatId, progress.getBook(), lang);
            sendMessage(chatId, progressText(progress, lang), bookActions(progress.getBook(), lang));
        }
    }

    @Scheduled(cron = "${app.telegram.reminder-cron:0 0 9 * * *}", zone = "${app.telegram.reminder-zone:Asia/Tashkent}")
    public void sendDailyReminders() {
        if (!properties.enabled() || !properties.hasToken()) {
            return;
        }
        for (ReadingProgress progress : readingProgressRepository.findByStatusOrderByUpdatedAtDesc(ReadingStatus.READING)) {
            User user = progress.getUser();
            String lang = normalizeLanguage(user.getLanguage());
            sendMessage(user.getTelegramUserId(), tr(lang, "daily_reminder").formatted(
                    progress.getBook().getTitle(),
                    progress.getCurrentPage()
            ), mainMenu(lang));
        }
    }

    private void startPageInput(long chatId, User user, Long bookId, String lang) {
        if (bookId == null || !bookRepository.existsById(bookId)) {
            sendMessage(chatId, tr(lang, "book_not_found"), mainMenu(lang));
            return;
        }
        pendingPageBookIds.put(user.getTelegramUserId(), bookId);
        user.setBotState(BotState.AWAITING_PAGE);
        userRepository.save(user);
        sendMessage(chatId, tr(lang, "page_prompt"), mainMenu(lang));
    }

    private void saveCurrentPage(long chatId, User user, String text, String lang) {
        Long bookId = pendingPageBookIds.get(user.getTelegramUserId());
        if (bookId == null) {
            user.setBotState(BotState.NONE);
            userRepository.save(user);
            sendMessage(chatId, tr(lang, "book_not_found"), mainMenu(lang));
            return;
        }

        int page;
        try {
            page = Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            sendMessage(chatId, tr(lang, "page_invalid"), mainMenu(lang));
            return;
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null || page < 1 || page > book.getTotalPages()) {
            sendMessage(chatId, tr(lang, "page_range").formatted(book == null ? 0 : book.getTotalPages()), mainMenu(lang));
            return;
        }

        ReadingProgress progress = readingProgressRepository.findByUserIdAndBookId(user.getId(), bookId).orElseGet(ReadingProgress::new);
        fillProgress(progress, user, book, page);
        readingProgressRepository.save(progress);

        pendingPageBookIds.remove(user.getTelegramUserId());
        user.setBotState(BotState.NONE);
        userRepository.save(user);
        sendMessage(chatId, tr(lang, "page_saved").formatted(book.getTitle(), page), mainMenu(lang));
    }

    private void markFinished(long chatId, User user, Long bookId, String lang) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            sendMessage(chatId, tr(lang, "book_not_found"), mainMenu(lang));
            return;
        }
        ReadingProgress progress = readingProgressRepository.findByUserIdAndBookId(user.getId(), bookId).orElseGet(ReadingProgress::new);
        fillProgress(progress, user, book, book.getTotalPages());
        readingProgressRepository.save(progress);
        sendMessage(chatId, tr(lang, "marked_finished").formatted(book.getTitle()), mainMenu(lang));
    }

    private void fillProgress(ReadingProgress progress, User user, Book book, int page) {
        OffsetDateTime now = OffsetDateTime.now();
        progress.setUser(user);
        progress.setBook(book);
        progress.setCurrentPage(page);
        progress.setProgressPercent(BigDecimal.valueOf(page)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(book.getTotalPages()), 2, RoundingMode.HALF_UP));
        progress.setStatus(page == book.getTotalPages() ? ReadingStatus.COMPLETED : ReadingStatus.READING);
        if (progress.getCreatedAt() == null) {
            progress.setCreatedAt(now);
        }
        progress.setUpdatedAt(now);
    }

    private void sendPdf(long chatId, Long bookId, String lang) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            sendMessage(chatId, tr(lang, "book_not_found"), mainMenu(lang));
            return;
        }
        if (book.getPdfFilePath() == null || book.getPdfFilePath().isBlank()) {
            sendMessage(chatId, tr(lang, "no_pdf"), mainMenu(lang));
            return;
        }
        try {
            Path pdf = fileStorageService.toAbsolutePath(book.getPdfFilePath());
            if (!Files.exists(pdf)) {
                sendMessage(chatId, tr(lang, "no_pdf"), mainMenu(lang));
                return;
            }
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("chat_id", String.valueOf(chatId));
            body.add("caption", book.getTitle());
            body.add("document", new FileSystemResource(pdf));
            restClient.post()
                    .uri("/{token}/sendDocument", tokenPath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Telegram sendDocument failed: {}", ex.getMessage());
        }
    }

    private void sendMessage(long chatId, String text, Map<String, Object> replyMarkup) {
        Map<String, Object> body = replyMarkup == null
                ? Map.of("chat_id", chatId, "text", text)
                : Map.of("chat_id", chatId, "text", text, "reply_markup", replyMarkup);
        try {
            restClient.post()
                    .uri("/{token}/sendMessage", tokenPath())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Telegram sendMessage failed: {}", ex.getMessage());
        }
    }

    private void answerCallback(String callbackId) {
        try {
            restClient.post()
                    .uri("/{token}/answerCallbackQuery", tokenPath())
                    .body(Map.of("callback_query_id", callbackId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Telegram answerCallbackQuery failed: {}", ex.getMessage());
        }
    }

    private Map<String, Object> mainMenu(String lang) {
        return Map.of(
                "resize_keyboard", true,
                "keyboard", List.of(
                        List.of(Map.of("text", tr(lang, "books")), Map.of("text", tr(lang, "my_books"))),
                        List.of(Map.of("text", tr(lang, "search")), Map.of("text", tr(lang, "language"))),
                        List.of(Map.of("text", tr(lang, "commands")), Map.of("text", tr(lang, "help")))
                )
        );
    }

    private Map<String, Object> languageMenu() {
        return Map.of("inline_keyboard", List.of(
                List.of(
                        Map.of("text", "O'zbek", "callback_data", "lang:uz"),
                        Map.of("text", "\u0420\u0443\u0441\u0441\u043a\u0438\u0439", "callback_data", "lang:ru"),
                        Map.of("text", "English", "callback_data", "lang:en")
                )
        ));
    }

    private Map<String, Object> bookActions(Book book, String lang) {
        return Map.of("inline_keyboard", List.of(
                List.of(Map.of("text", tr(lang, "download_pdf"), "callback_data", "pdf:" + book.getId())),
                List.of(
                        Map.of("text", tr(lang, "save_page"), "callback_data", "page:" + book.getId()),
                        Map.of("text", tr(lang, "mark_finished"), "callback_data", "done:" + book.getId())
                )
        ));
    }

    private String bookCaption(Book book, String lang) {
        String genre = book.getGenre() == null || book.getGenre().isBlank() ? "" : "\n" + tr(lang, "genre") + ": " + book.getGenre();
        return "%s\n%s: %s%s\n%s: %d".formatted(
                book.getTitle(),
                tr(lang, "author"),
                book.getAuthor(),
                genre,
                tr(lang, "pages"),
                book.getTotalPages()
        );
    }

    private String progressText(ReadingProgress progress, String lang) {
        return tr(lang, "progress_reminder").formatted(
                progress.getBook().getTitle(),
                progress.getCurrentPage(),
                progress.getBook().getTotalPages()
        );
    }

    private String pdfStatus(Book book, String lang) {
        return book.getPdfFilePath() == null || book.getPdfFilePath().isBlank()
                ? tr(lang, "no_pdf")
                : tr(lang, "pdf_available");
    }

    private boolean matches(String text, String key) {
        String normalized = normalizeText(text);
        return normalized.equals(normalizeText(tr("uz", key)))
                || normalized.equals(normalizeText(tr("ru", key)))
                || normalized.equals(normalizeText(tr("en", key)));
    }

    private boolean setLanguageIfSelected(User user, String text) {
        String normalized = normalizeText(text);
        String language = switch (normalized) {
            case "uz", "ozbek", "o'zbek", "uzbek" -> "uz";
            case "ru", "\u0440\u0443\u0441\u0441\u043a\u0438\u0439", "russian" -> "ru";
            case "en", "english" -> "en";
            default -> null;
        };
        if (language == null) {
            return false;
        }
        user.setLanguage(language);
        user.setBotState(BotState.NONE);
        userRepository.save(user);
        return true;
    }

    private String tr(String lang, String key) {
        return switch (normalizeLanguage(lang)) {
            case "ru" -> trRu(key);
            case "en" -> trEn(key);
            default -> trUz(key);
        };
    }

    private String trUz(String key) {
        return switch (key) {
            case "start" -> "BookHub botiga xush kelibsiz.";
            case "help", "commands_text" -> "Buyruqlar:\n/start\n/help\nKitoblar\nMening kitoblarim\nQidiruv\nTilni o'zgartirish\nBuyruqlar";
            case "books" -> "Kitoblar";
            case "my_books" -> "Mening kitoblarim";
            case "search" -> "Qidiruv";
            case "language" -> "Tilni o'zgartirish";
            case "commands" -> "Buyruqlar";
            case "search_prompt" -> "Kitob nomi, muallifi yoki janrini yuboring.";
            case "language_prompt" -> "Tilni tanlang.";
            case "language_saved" -> "Til saqlandi.";
            case "no_books" -> "Kitoblar topilmadi.";
            case "no_my_books" -> "Sizda boshlangan kitoblar yo'q.";
            case "my_books_title" -> "Mening kitoblarim:";
            case "more_books" -> "Yana %d ta kitob bor.";
            case "unknown" -> "Menyudan buyruq tanlang.";
            case "author" -> "Muallif";
            case "genre" -> "Janr";
            case "pages" -> "Sahifalar";
            case "download_pdf" -> "PDF";
            case "save_page" -> "Sahifani saqlash";
            case "mark_finished" -> "Tugatdim";
            case "no_pdf" -> "PDF mavjud emas.";
            case "pdf_available" -> "PDF mavjud.";
            case "page_prompt" -> "Hozirgi sahifa raqamini yuboring.";
            case "page_invalid" -> "Raqam yuboring.";
            case "page_range" -> "Sahifa 1 va %d oralig'ida bo'lishi kerak.";
            case "page_saved" -> "%s: %d-sahifa saqlandi.";
            case "marked_finished" -> "%s tugatilgan deb belgilandi.";
            case "book_not_found" -> "Kitob topilmadi.";
            case "progress_reminder" -> "%s: siz %d/%d sahifada to'xtagansiz.";
            case "daily_reminder" -> "Eslatma: %s, saqlangan sahifa %d.";
            default -> key;
        };
    }

    private String trEn(String key) {
        return switch (key) {
            case "start" -> "Welcome to BookHub.";
            case "help", "commands_text" -> "Commands:\n/start\n/help\nBooks\nMy books\nSearch\nChange language\nCommands";
            case "books" -> "Books";
            case "my_books" -> "My books";
            case "search" -> "Search";
            case "language" -> "Change language";
            case "commands" -> "Commands";
            case "search_prompt" -> "Send a book title, author, or genre.";
            case "language_prompt" -> "Choose a language.";
            case "language_saved" -> "Language saved.";
            case "no_books" -> "No books found.";
            case "no_my_books" -> "You have no started books yet.";
            case "my_books_title" -> "My books:";
            case "more_books" -> "%d more books.";
            case "unknown" -> "Choose a command from the menu.";
            case "author" -> "Author";
            case "genre" -> "Genre";
            case "pages" -> "Pages";
            case "download_pdf" -> "PDF";
            case "save_page" -> "Save page";
            case "mark_finished" -> "Finished";
            case "no_pdf" -> "PDF is not available.";
            case "pdf_available" -> "PDF is available.";
            case "page_prompt" -> "Send your current page number.";
            case "page_invalid" -> "Send a number.";
            case "page_range" -> "Page must be between 1 and %d.";
            case "page_saved" -> "%s: page %d saved.";
            case "marked_finished" -> "%s marked as finished.";
            case "book_not_found" -> "Book not found.";
            case "progress_reminder" -> "%s: you stopped at %d/%d.";
            case "daily_reminder" -> "Reminder: %s, page %d.";
            default -> key;
        };
    }

    private String trRu(String key) {
        return switch (key) {
            case "start" -> "\u0414\u043e\u0431\u0440\u043e \u043f\u043e\u0436\u0430\u043b\u043e\u0432\u0430\u0442\u044c \u0432 BookHub.";
            case "help", "commands_text" -> "\u041a\u043e\u043c\u0430\u043d\u0434\u044b:\n/start\n/help\n\u041a\u043d\u0438\u0433\u0438\n\u041c\u043e\u0438 \u043a\u043d\u0438\u0433\u0438\n\u041f\u043e\u0438\u0441\u043a\n\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u044f\u0437\u044b\u043a\n\u041a\u043e\u043c\u0430\u043d\u0434\u044b";
            case "books" -> "\u041a\u043d\u0438\u0433\u0438";
            case "my_books" -> "\u041c\u043e\u0438 \u043a\u043d\u0438\u0433\u0438";
            case "search" -> "\u041f\u043e\u0438\u0441\u043a";
            case "language" -> "\u0418\u0437\u043c\u0435\u043d\u0438\u0442\u044c \u044f\u0437\u044b\u043a";
            case "commands" -> "\u041a\u043e\u043c\u0430\u043d\u0434\u044b";
            case "search_prompt" -> "\u0412\u0432\u0435\u0434\u0438\u0442\u0435 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435, \u0430\u0432\u0442\u043e\u0440\u0430 \u0438\u043b\u0438 \u0436\u0430\u043d\u0440 \u043a\u043d\u0438\u0433\u0438.";
            case "language_prompt" -> "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u044f\u0437\u044b\u043a.";
            case "language_saved" -> "\u042f\u0437\u044b\u043a \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d.";
            case "no_books" -> "\u041a\u043d\u0438\u0433\u0438 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u044b.";
            case "no_my_books" -> "\u0423 \u0432\u0430\u0441 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442 \u043d\u0430\u0447\u0430\u0442\u044b\u0445 \u043a\u043d\u0438\u0433.";
            case "my_books_title" -> "\u041c\u043e\u0438 \u043a\u043d\u0438\u0433\u0438:";
            case "more_books" -> "\u0415\u0449\u0435 %d \u043a\u043d\u0438\u0433.";
            case "unknown" -> "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u0443 \u0438\u0437 \u043c\u0435\u043d\u044e.";
            case "author" -> "\u0410\u0432\u0442\u043e\u0440";
            case "genre" -> "\u0416\u0430\u043d\u0440";
            case "pages" -> "\u0421\u0442\u0440\u0430\u043d\u0438\u0446\u044b";
            case "download_pdf" -> "PDF";
            case "save_page" -> "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0443";
            case "mark_finished" -> "\u0417\u0430\u043a\u043e\u043d\u0447\u0438\u043b";
            case "no_pdf" -> "PDF \u043d\u0435\u0442.";
            case "pdf_available" -> "PDF \u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d.";
            case "page_prompt" -> "\u041e\u0442\u043f\u0440\u0430\u0432\u044c\u0442\u0435 \u0442\u0435\u043a\u0443\u0449\u0443\u044e \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0443.";
            case "page_invalid" -> "\u041e\u0442\u043f\u0440\u0430\u0432\u044c\u0442\u0435 \u0447\u0438\u0441\u043b\u043e.";
            case "page_range" -> "\u0421\u0442\u0440\u0430\u043d\u0438\u0446\u0430 \u0434\u043e\u043b\u0436\u043d\u0430 \u0431\u044b\u0442\u044c 1-%d.";
            case "page_saved" -> "%s: \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430 %d \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0430.";
            case "marked_finished" -> "%s \u043e\u0442\u043c\u0435\u0447\u0435\u043d\u0430 \u043a\u0430\u043a \u0437\u0430\u043a\u043e\u043d\u0447\u0435\u043d\u043d\u0430\u044f.";
            case "book_not_found" -> "\u041a\u043d\u0438\u0433\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430.";
            case "progress_reminder" -> "%s: \u0432\u044b \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u043b\u0438\u0441\u044c \u043d\u0430 %d/%d.";
            case "daily_reminder" -> "\u041d\u0430\u043f\u043e\u043c\u0438\u043d\u0430\u043d\u0438\u0435: %s, \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430 %d.";
            default -> key;
        };
    }

    private void logDisabledWarningOnce() {
        if (!disabledWarningLogged) {
            log.info("Telegram bot polling is disabled. Set TELEGRAM_BOT_TOKEN and APP_TELEGRAM_ENABLED=true to enable it.");
            disabledWarningLogged = true;
        }
    }

    private String tokenPath() {
        return "bot" + properties.botToken();
    }

    private Long parseId(String data, String prefix) {
        try {
            return Long.parseLong(data.substring(prefix.length()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            return "uz";
        }
        String normalized = language.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ru", "en" -> normalized;
            default -> "uz";
        };
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
