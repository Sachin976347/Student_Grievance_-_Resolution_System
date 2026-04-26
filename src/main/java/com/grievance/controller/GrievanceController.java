package com.grievance.controller;

import com.grievance.model.Grievance;
import com.grievance.repository.GrievanceRepository;
import com.grievance.service.EmailNotificationService;
import com.grievance.service.SlaSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/grievances")
@CrossOrigin(origins = "*")
public class GrievanceController {

    @Autowired
    private GrievanceRepository grievanceRepository;
    @Autowired
    private SlaSchedulerService slaService;
    @Autowired
    private EmailNotificationService emailService;

    // ══════════════════════════════════════════════════════════════════
    // ADMIN-ONLY ENDPOINTS
    // ══════════════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<List<Grievance>> getAllGrievances() {
        return ResponseEntity.ok(grievanceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grievance> getById(@PathVariable Long id) {
        return grievanceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Grievance>> getByStudentId(@PathVariable String studentId) {
        return ResponseEntity.ok(grievanceRepository.findByStudentId(studentId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Grievance>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(grievanceRepository.findByStatus(status));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Grievance>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(grievanceRepository.findByCategory(category));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Grievance>> getPending() {
        return ResponseEntity.ok(grievanceRepository.findPendingOrderedByPriority());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Grievance>> getOverdue() {
        return ResponseEntity.ok(grievanceRepository.findByOverdueTrue());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", grievanceRepository.count());
        summary.put("open", grievanceRepository.countByStatus("Open"));
        summary.put("inProgress", grievanceRepository.countByStatus("In Progress"));
        summary.put("resolved", grievanceRepository.countByStatus("Resolved"));
        summary.put("closed", grievanceRepository.countByStatus("Closed"));
        summary.put("highPriority", grievanceRepository.countByPriority("High"));
        summary.put("overdue", grievanceRepository.findByOverdueTrue().size());

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : grievanceRepository.countByEachCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        summary.put("byCategory", byCategory);
        return ResponseEntity.ok(summary);
    }

    // PUT — update status, assign, priority, remarks (ADMIN only)
    @PutMapping("/{id}")
    public ResponseEntity<Grievance> updateGrievance(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {

        return grievanceRepository.findById(id).map(g -> {
            boolean statusChanged = false;

            if (updates.containsKey("status")) {
                String newStatus = updates.get("status");
                if (!newStatus.equals(g.getStatus())) {
                    statusChanged = true;
                    g.setStatus(newStatus);
                    if ("Resolved".equals(newStatus)) {
                        g.setResolvedAt(LocalDateTime.now());
                        g.setOverdue(false); // clear overdue flag on resolution
                    }
                }
            }
            if (updates.containsKey("assignedTo"))
                g.setAssignedTo(updates.get("assignedTo"));
            if (updates.containsKey("priority"))
                g.setPriority(updates.get("priority"));
            if (updates.containsKey("remarks"))
                g.setRemarks(updates.get("remarks"));

            Grievance saved = grievanceRepository.save(g);

            // Send email notification on status change
            if (statusChanged)
                emailService.sendStatusUpdate(saved);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE (ADMIN only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrievance(@PathVariable Long id) {
        if (!grievanceRepository.existsById(id))
            return ResponseEntity.notFound().build();
        grievanceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════════
    // STUDENT ONLY — submit grievance
    // Admin is blocked at SecurityConfig level (hasRole STUDENT only).
    // The extra @PreAuthorize below is a defence-in-depth double check.
    // ══════════════════════════════════════════════════════════════════

    // POST — only ROLE_STUDENT can submit a grievance
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Grievance> submitGrievance(
            @RequestBody Grievance grievance,
            Authentication auth) {

        grievance.setStatus("Open");
        grievance.setSubmittedAt(LocalDateTime.now());
        grievance.setOwnerUsername(auth.getName());

        // ── SLA due date based on priority ──
        grievance.setDueDate(slaService.calculateDueDate(grievance.getPriority()));

        Grievance saved = grievanceRepository.save(grievance);

        // ── Acknowledgement email to student ──
        emailService.sendSubmissionAck(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ══════════════════════════════════════════════════════════════════
    // STUDENT-SCOPED (/mine/*) — read-only, own data only
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/mine")
    public ResponseEntity<List<Grievance>> getMyGrievances(Authentication auth) {
        return ResponseEntity.ok(
                grievanceRepository.findByOwnerUsername(auth.getName()));
    }

    @GetMapping("/mine/dashboard")
    public ResponseEntity<Map<String, Object>> getMyDashboard(Authentication auth) {
        String username = auth.getName();
        List<Grievance> mine = grievanceRepository.findByOwnerUsername(username);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", mine.size());
        summary.put("open", mine.stream().filter(g -> "Open".equals(g.getStatus())).count());
        summary.put("inProgress", mine.stream().filter(g -> "In Progress".equals(g.getStatus())).count());
        summary.put("resolved", mine.stream().filter(g -> "Resolved".equals(g.getStatus())).count());
        summary.put("closed", mine.stream().filter(g -> "Closed".equals(g.getStatus())).count());
        summary.put("overdue", mine.stream().filter(Grievance::isOverdue).count());

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : grievanceRepository.countByEachCategoryForUser(username)) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        summary.put("byCategory", byCategory);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/mine/{id}")
    public ResponseEntity<Grievance> getMyGrievanceById(
            @PathVariable Long id, Authentication auth) {
        return grievanceRepository.findById(id)
                .filter(g -> auth.getName().equals(g.getOwnerUsername()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }
}