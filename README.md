# BookHub Bot — Backend

Telegram orqali foydalanuvchilarga kitob o'qish imkonini beruvchi tizimning Spring Boot backend qismi. Admin panel orqali kitoblarni boshqarish, foydalanuvchilarning o'qish jarayonini kuzatish va reyting tizimini o'z ichiga oladi.

## Texnologiyalar

| Texnologiya | Versiya |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.7 |
| Spring Security | JWT asosida |
| PostgreSQL | Production baza |
| H2 | Local/test baza |
| Flyway | DB migratsiyalari |
| Lombok | Kod generatsiyasi |
| SpringDoc OpenAPI | Swagger UI |

---

## Loyiha tuzilmasi

```
src/main/java/com/bookhub/
├── config/          # Konfiguratsiyalar (Security, CORS, JWT, Swagger)
├── controller/      # REST API endpointlari
├── domain/          # Entity classlar
├── dto/             # Request/Response obyektlari
├── exception/       # Global xato boshqaruvi
├── mapper/          # Entity <-> DTO konvertatsiya
├── repository/      # JPA repositoriyalar
├── security/        # JWT filter
└── service/         # Biznes logika

src/main/resources/
├── application.yml          # Asosiy konfiguratsiya
├── application-local.yml    # Local muhit (H2 baza)
└── db/migration/            # Flyway SQL migratsiyalari
```

---

## Ishga tushirish

### Talablar

- Java 21+
- Maven 3.8+
- PostgreSQL (production uchun) yoki H2 (local uchun, avtomatik)

### 1. `.env` fayli yaratish

Loyiha papkasining **bir yuqorisida** (ya'ni `../.env`) `.env` faylini yarating:

```properties
JWT_SECRET=maxfiy-kalit-bu-yerda
JWT_ISSUER=BookHub
JWT_AUDIENCE=BookHubAdmin
SERVER_PORT=7172
ADMIN_USERNAME=admin
ADMIN_PASSWORD=Admin123!
ADMIN_ROLE=Admin
APP_TELEGRAM_ENABLED=true
TELEGRAM_BOT_TOKEN=your-telegram-bot-token
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000
```

> **Eslatma:** `APP_TELEGRAM_ENABLED=false` qilib qo'ysangiz bot ishlamaydi, lekin REST API ishlayveradi.

### 2. Local rejimda ishga tushirish (H2 baza)

```bash
mvn spring-boot:run
```

`local` profil avtomatik faollashadi va H2 in-memory bazadan foydalanadi.

### 3. PostgreSQL bilan ishga tushirish

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

`.env` faylda quyidagi qatorlarni ham qo'shing:

```properties
DB_URL=jdbc:postgresql://localhost:5432/bookhub
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

---

## Docker orqali ishga tushirish

```bash
# Image yaratish
docker build -t bookhub-backend .

# Ishga tushirish
docker run -p 7172:8080 \
  -e JWT_SECRET=your-secret \
  -e TELEGRAM_BOT_TOKEN=your-token \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/bookhub \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your_password \
  bookhub-backend
```

---

## Swagger UI — API hujjatlari

Server ishga tushgandan so'ng quyidagi manzilda to'liq API hujjatlarini ko'rish mumkin:

```
http://localhost:7172/swagger-ui.html
```

### Swagger orqali so'rov yuborish

1. **Brauzerda oching:** `http://localhost:7172/swagger-ui.html`
2. Yuqori o'ng burchakdagi **`Authorize`** tugmasini bosing
3. `/api/auth/login` endpointi orqali token oling:
   ```json
   {
     "username": "admin",
     "password": "Admin123!"
   }
   ```
4. Olingan `token` qiymatini **`Authorize`** oynasiga `Bearer <token>` formatida kiriting
5. Endi barcha himoyalangan endpointlarga so'rov yuborishingiz mumkin

> **Muhim:** Server `http://` rejimida ishlaydi. Brauzerda `https://localhost:7172` emas, `http://localhost:7172` manzilini ishlating.

---

## API Endpointlari

### Autentifikatsiya (`/api/auth`)

| Method | Endpoint | Tavsif | Himoya |
|--------|----------|--------|--------|
| `POST` | `/api/auth/login` | Admin tizimga kirish | Ochiq |
| `POST` | `/api/auth/register` | Yangi admin qo'shish | JWT |

### Kitoblar (`/api/books`)

| Method | Endpoint | Tavsif | Himoya |
|--------|----------|--------|--------|
| `GET` | `/api/books` | Barcha kitoblar (`?search=` bilan qidirish) | Ochiq |
| `GET` | `/api/books/{id}` | Bitta kitob | Ochiq |
| `GET` | `/api/books/{id}/download` | PDF yuklab olish | Ochiq |
| `POST` | `/api/books/{bookId}/rating` | Kitobga baho berish | JWT |
| `GET` | `/api/books/{bookId}/ratings` | Kitob reytingi statistikasi | Ochiq |

### Admin — Kitob boshqaruvi (`/api/admin/books`)

| Method | Endpoint | Tavsif | Himoya |
|--------|----------|--------|--------|
| `POST` | `/api/admin/books` | Yangi kitob qo'shish (multipart) | JWT |
| `PUT` | `/api/admin/books/{id}` | Kitobni tahrirlash (multipart) | JWT |
| `DELETE` | `/api/admin/books/{id}` | Kitobni o'chirish | JWT |

### Foydalanuvchilar (`/api/users`)

| Method | Endpoint | Tavsif | Himoya |
|--------|----------|--------|--------|
| `GET` | `/api/users` | Barcha foydalanuvchilar | JWT |
| `GET` | `/api/users/{id}/reading-progress` | Foydalanuvchi o'qish tarixi | JWT |

### O'qish jarayoni (`/api/reading-progress`)

| Method | Endpoint | Tavsif | Himoya |
|--------|----------|--------|--------|
| `GET` | `/api/reading-progress` | Barcha yozuvlar | JWT |
| `GET` | `/api/reading-progress/user/{userId}` | Foydalanuvchi bo'yicha | JWT |
| `POST` | `/api/reading-progress` | Yangi yozuv | JWT |
| `PUT` | `/api/reading-progress/{id}` | Yangilash | JWT |

### Dashboard (`/api/dashboard`)

| Method | Endpoint | Tavsif | Himoya |
|--------|----------|--------|--------|
| `GET` | `/api/dashboard/statistics` | Umumiy statistika | JWT |
| `GET` | `/api/dashboard/most-read-books` | Ko'p o'qilgan kitoblar | JWT |
| `GET` | `/api/dashboard/highest-rated-books` | Eng yuqori reytingli kitoblar | JWT |

---

## Muhit o'zgaruvchilari

| O'zgaruvchi | Default | Tavsif |
|---|---|---|
| `SERVER_PORT` | `7172` | Server porti |
| `JWT_SECRET` | *(majburiy)* | JWT imzolash kaliti (min 32 belgi) |
| `JWT_ISSUER` | `BookHub` | JWT chiqaruvchi |
| `JWT_AUDIENCE` | `BookHubAdmin` | JWT maqsadli auditoriya |
| `JWT_EXPIRY_MINUTES` | `120` | Token amal qilish muddati |
| `DB_URL` | H2 (local) | PostgreSQL ulanish URL |
| `DB_USERNAME` | `postgres` | Baza foydalanuvchisi |
| `DB_PASSWORD` | `root` | Baza paroli |
| `ADMIN_USERNAME` | `admin` | Boshlang'ich admin nomi |
| `ADMIN_PASSWORD` | `Admin123!` | Boshlang'ich admin paroli |
| `APP_TELEGRAM_ENABLED` | `false` | Telegram botni yoqish/o'chirish |
| `TELEGRAM_BOT_TOKEN` | *(bo'sh)* | BotFather dan olingan token |
| `CORS_ALLOWED_ORIGINS` | `localhost:5173, ...` | Ruxsat berilgan frontendlar |
| `STORAGE_ROOT` | `./storage` | Fayllar saqlanadigan papka |
| `MULTIPART_MAX_FILE_SIZE` | `30MB` | Maksimal fayl hajmi |

---

## Ma'lumotlar bazasi

Migratsiyalar Flyway orqali avtomatik bajariladi:

| Versiya | Fayl | Tavsif |
|---|---|---|
| V1 | `V1__initial_schema.sql` | Asosiy jadvallar: users, books, reading_progresses, book_ratings |
| V2 | `V2__add_book_genre.sql` | Kitoblarga `genre` ustuni qo'shildi |

---

## Xavfsizlik

- Barcha so'rovlar JWT Bearer token orqali himoyalanadi
- Ochiq endpointlar: `GET /api/books/**`, `POST /api/auth/login`, Swagger UI
- Fayllar `/uploads/**` orqali statik tarzda xizmat qiladi
- Parollar BCrypt bilan shifrlangan
