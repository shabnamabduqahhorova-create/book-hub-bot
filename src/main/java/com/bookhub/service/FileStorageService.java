package com.bookhub.service;

import com.bookhub.config.StorageProperties;
import com.bookhub.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final String BOOKS_FOLDER = "uploads/books";
    private static final String COVERS_FOLDER = "uploads/covers";

    private final Path rootPath;

    public FileStorageService(StorageProperties properties) {
        this.rootPath = Path.of(properties.rootPath()).toAbsolutePath().normalize();
    }

    public String saveBookFile(MultipartFile file) {
        return save(file, BOOKS_FOLDER);
    }

    public String saveCover(MultipartFile file) {
        return save(file, COVERS_FOLDER);
    }

    public void delete(String publicPath) {
        if (publicPath == null || publicPath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(toAbsolutePath(publicPath));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete stored file.");
        }
    }

    public Path toAbsolutePath(String publicPath) {
        String clean = publicPath == null ? "" : publicPath.replace('\\', '/').replaceFirst("^/+", "");
        Path resolved = rootPath.resolve(clean).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path.");
        }
        return resolved;
    }

    private String save(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + (extension == null ? "" : "." + extension);
        Path destinationFolder = rootPath.resolve(folder).normalize();
        Path destination = destinationFolder.resolve(fileName).normalize();
        try {
            Files.createDirectories(destinationFolder);
            file.transferTo(destination);
            return "/" + folder + "/" + fileName;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save uploaded file.");
        }
    }
}
