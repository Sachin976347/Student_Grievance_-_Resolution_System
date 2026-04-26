package com.grievance.controller;

import com.grievance.model.Grievance;
import com.grievance.repository.GrievanceRepository;
import com.grievance.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

/**
 * STEP 4 — File Upload & Download Controller
 *
 * POST /api/files/upload/{grievanceId}  — attach a file to a grievance
 * GET  /api/files/download/{grievanceId} — download the attached file
 *
 * Access rules:
 *   - Upload: owner of the grievance OR admin
 *   - Download: owner of the grievance OR admin
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired private FileStorageService    fileStorageService;
    @Autowired private GrievanceRepository   grievanceRepository;

    // ─── POST /api/files/upload/{grievanceId} ─────────────────────────────────
    @PostMapping("/upload/{grievanceId}")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable Long grievanceId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        Grievance g = grievanceRepository.findById(grievanceId).orElse(null);
        if (g == null) return ResponseEntity.notFound().build();

        // Only the owner or an admin can attach files
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !auth.getName().equals(g.getOwnerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Delete old file if a previous one exists
            if (g.getFilePath() != null) {
                fileStorageService.delete(g.getFilePath());
            }

            String storedPath = fileStorageService.store(file);
            g.setFilePath(storedPath);
            g.setFileName(file.getOriginalFilename());
            grievanceRepository.save(g);

            return ResponseEntity.ok(Map.of(
                "message",  "File uploaded successfully.",
                "fileName", file.getOriginalFilename(),
                "filePath", storedPath
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        }
    }

    // ─── GET /api/files/download/{grievanceId} ────────────────────────────────
    @GetMapping("/download/{grievanceId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long grievanceId,
            Authentication auth) {

        Grievance g = grievanceRepository.findById(grievanceId).orElse(null);
        if (g == null || g.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        // Access check
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !auth.getName().equals(g.getOwnerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path filePath = fileStorageService.load(g.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            String disposition = "attachment; filename=\""
                               + (g.getFileName() != null ? g.getFileName() : g.getFilePath())
                               + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}