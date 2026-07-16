package com.thinkerscave.shared.storage;

import com.thinkerscave.shared.exceptions.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Local-disk file storage with size, MIME, filename, and path-traversal guards.
 * Cloud object storage can replace this later without changing controllers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private final FileUploadProperties properties;
    private Path rootLocation;

    @PostConstruct
    void init() throws IOException {
        rootLocation = Paths.get(properties.getBaseDir()).toAbsolutePath().normalize();
        Files.createDirectories(rootLocation);
    }

    public String store(MultipartFile file, String prefix) throws IOException {
        validate(file);
        String safeName = sanitizeFilename(file.getOriginalFilename());
        String storedName = prefix + "_" + UUID.randomUUID().toString().replace("-", "") + "_" + safeName;
        Path destination = rootLocation.resolve(storedName).normalize().toAbsolutePath();
        if (!destination.startsWith(rootLocation)) {
            throw new IOException("Cannot store file outside upload directory");
        }
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination.toString();
    }

    public Resource loadAsResource(String absolutePath) {
        try {
            if (!StringUtils.hasText(absolutePath) || absolutePath.contains("..")) {
                throw new BadRequestException("Invalid document path");
            }
            Path path = Paths.get(absolutePath).normalize().toAbsolutePath();
            // Prefer files under the configured upload root; allow legacy absolute paths
            // stored before upload hardening (must still exist and be readable).
            if (!path.startsWith(rootLocation)) {
                if (!Files.isRegularFile(path)) {
                    throw new BadRequestException("Invalid document path");
                }
                log.warn("Serving legacy upload path outside configured root (migrate to {})", rootLocation);
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("Document file not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid document path");
        }
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException("File exceeds maximum allowed size");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || properties.getAllowedContentTypes().stream()
                .noneMatch(allowed -> allowed.equalsIgnoreCase(contentType))) {
            throw new BadRequestException("File type is not allowed");
        }
    }

    public String sanitizeFilename(String original) {
        if (!StringUtils.hasText(original)) {
            return "file";
        }
        String name = Paths.get(original).getFileName().toString();
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 100) {
            name = name.substring(name.length() - 100);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    public Path getRootLocation() {
        return rootLocation;
    }
}
