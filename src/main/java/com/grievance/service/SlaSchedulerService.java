package com.grievance.service;

import com.grievance.model.Grievance;
import com.grievance.repository.GrievanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * STEP 6 — SLA Deadline Scheduler
 *
 * Runs every hour via @Scheduled cron.
 * Finds all grievances where:
 *   - dueDate has passed
 *   - status is NOT Resolved or Closed
 *   - overdue flag is not already set
 *
 * Marks them overdue and sends an email alert to the student.
 *
 * SLA hours per priority come from application.properties:
 *   app.sla.high-hours   = 24   (1 day)
 *   app.sla.medium-hours = 72   (3 days)
 *   app.sla.low-hours    = 168  (7 days)
 */
@Service
public class SlaSchedulerService {

    @Autowired private GrievanceRepository    grievanceRepository;
    @Autowired private EmailNotificationService emailService;

    @Value("${app.sla.high-hours:24}")   private long highHours;
    @Value("${app.sla.medium-hours:72}") private long mediumHours;
    @Value("${app.sla.low-hours:168}")   private long lowHours;

    // ─── Called at submit time to set the SLA due date ────────────────────────
    public LocalDateTime calculateDueDate(String priority) {
        long hours = switch (priority) {
            case "High"   -> highHours;
            case "Medium" -> mediumHours;
            default       -> lowHours;   // Low
        };
        return LocalDateTime.now().plusHours(hours);
    }

    // ─── Scheduled job: runs every hour ──────────────────────────────────────
    // cron = "0 0 * * * *" means: at minute 0 of every hour
    @Scheduled(cron = "0 0 * * * *")
    public void checkOverdueGrievances() {
        List<Grievance> breached = grievanceRepository.findBreachedSLAs(LocalDateTime.now());

        if (breached.isEmpty()) {
            System.out.println("[SlaScheduler] No SLA breaches found.");
            return;
        }

        System.out.println("[SlaScheduler] Found " + breached.size() + " overdue grievance(s).");

        for (Grievance g : breached) {
            g.setOverdue(true);
            grievanceRepository.save(g);
            emailService.sendOverdueAlert(g);
            System.out.println("[SlaScheduler] Marked overdue: GR-"
                               + String.format("%04d", g.getId())
                               + " | Student: " + g.getStudentName());
        }
    }
}