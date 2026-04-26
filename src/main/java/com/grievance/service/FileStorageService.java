package com.grievance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * STEP 4 — File / Document Upload Service
 *
 * Responsibilities:
 *   - Validate file type (PDF, JPG, PNG, DOCX only)
 *   - Generate a unique filename to prevent collisions
 *   - Save the file to the upload directory (app.upload.dir)
 *   - Return the stored relative path for DB persistence
 */
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // Allowed MIME types — students can attach docs, images, or PDFs
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    // ─── Store a file, return relative path ────────────────────────────────────
    public String store(MultipartFile file) throws IOException {
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Build a unique filename: UUID + original extension
        String originalName  = StringUtils.cleanPath(file.getOriginalFilename());
        String extension     = getExtension(originalName);
        String uniqueName    = UUID.randomUUID().toString() + extension;

        Path targetPath = uploadPath.resolve(uniqueName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueName;  // stored in Grievance.filePath
    }

    // ─── Load a stored file as Path ────────────────────────────────────────────
    public Path load(String filename) {
        return Paths.get(uploadDir).resolve(filename).normalize();
    }

    // ─── Delete a stored file ─────────────────────────────────────────────────
    public void delete(String filename) {
        try {
            Path path = load(filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("[FileStorageService] Could not delete file: " + filename);
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds 10 MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed: PDF, JPG, PNG, DOC, DOCX.");
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }
}