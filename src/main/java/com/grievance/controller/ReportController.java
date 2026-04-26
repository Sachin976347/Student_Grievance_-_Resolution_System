package com.grievance.controller;

import com.grievance.repository.GrievanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

/**
 * STEP 5 — Analytics / Reports Controller  (ADMIN only)
 *
 * All endpoints secured via SecurityConfig — ROLE_ADMIN only.
 *
 * GET /api/reports/summary          — key metrics for the admin dashboard
 * GET /api/reports/monthly          — grievances submitted per month (6 months)
 * GET /api/reports/resolution-time  — avg resolution hours per category
 * GET /api/reports/authority        — per-authority resolution performance
 * GET /api/reports/overdue          — all currently overdue grievances
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired private GrievanceRepository grievanceRepository;

    // ─── GET /api/reports/summary ─────────────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        long total    = grievanceRepository.count();
        long resolved = grievanceRepository.countByStatus("Resolved");
        long overdue  = grievanceRepository.findByOverdueTrue().size();

        summary.put("total",           total);
        summary.put("open",            grievanceRepository.countByStatus("Open"));
        summary.put("inProgress",      grievanceRepository.countByStatus("In Progress"));
        summary.put("resolved",        resolved);
        summary.put("closed",          grievanceRepository.countByStatus("Closed"));
        summary.put("overdue",         overdue);
        summary.put("highPriority",    grievanceRepository.countByPriority("High"));
        summary.put("resolutionRate",  total > 0
                ? Math.round((double) resolved / total * 100) : 0);

        // Category breakdown
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : grievanceRepository.countByEachCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        summary.put("byCategory", byCategory);

        return ResponseEntity.ok(summary);
    }

    // ─── GET /api/reports/monthly ─────────────────────────────────────────────
    @GetMapping("/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthly() {
        LocalDateTime since = LocalDateTime.now().minusMonths(6);
        List<Object[]> rows = grievanceRepository.countByMonth(since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            int month = ((Number) row[0]).intValue();
            int year  = ((Number) row[1]).intValue();
            entry.put("month", Month.of(month).name().substring(0, 3)); // "JAN", "FEB" …
            entry.put("year",  year);
            entry.put("count", ((Number) row[2]).longValue());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // ─── GET /api/reports/resolution-time ────────────────────────────────────
    @GetMapping("/resolution-time")
    public ResponseEntity<List<Map<String, Object>>> getResolutionTime() {
        List<Object[]> rows = grievanceRepository.avgResolutionHoursByCategory();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("category", row[0]);
            // Convert hours to days (1 decimal place)
            double hours = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            entry.put("avgHours", Math.round(hours * 10.0) / 10.0);
            entry.put("avgDays",  Math.round(hours / 24.0 * 10.0) / 10.0);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // ─── GET /api/reports/authority ───────────────────────────────────────────
    @GetMapping("/authority")
    public ResponseEntity<List<Map<String, Object>>> getAuthorityPerformance() {
        List<Object[]> rows = grievanceRepository.authorityPerformance();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            String authority  = (String) row[0];
            long   total      = ((Number) row[1]).longValue();
            long   resolvedCt = ((Number) row[2]).longValue();

            entry.put("authority",      authority);
            entry.put("total",          total);
            entry.put("resolved",       resolvedCt);
            entry.put("resolutionRate", total > 0
                    ? Math.round((double) resolvedCt / total * 100) : 0);
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // ─── GET /api/reports/overdue ─────────────────────────────────────────────
    @GetMapping("/overdue")
    public ResponseEntity<List<Map<String, Object>>> getOverdue() {
        var overdueList = grievanceRepository.findByOverdueTrue();
        List<Map<String, Object>> result = new ArrayList<>();

        for (var g : overdueList) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id",          g.getId());
            entry.put("studentName", g.getStudentName());
            entry.put("category",    g.getCategory());
            entry.put("priority",    g.getPriority());
            entry.put("status",      g.getStatus());
            entry.put("submittedAt", g.getSubmittedAt());
            entry.put("dueDate",     g.getDueDate());
            entry.put("assignedTo",  g.getAssignedTo());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }
}