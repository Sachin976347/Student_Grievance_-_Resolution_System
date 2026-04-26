package com.grievance.service;

import com.grievance.model.Grievance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * STEP 5 — Email Notification Service
 *
 * Sends transactional emails to students when:
 *   - Their grievance is submitted (acknowledgement)
 *   - Status changes (In Progress, Resolved, Closed)
 *   - Grievance becomes overdue (alert)
 *
 * Controlled by app.mail.enabled in application.properties.
 * Set to false in dev to avoid needing real SMTP credentials.
 */
@Service
public class EmailNotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:noreply@institution.edu}")
    private String fromAddress;

    // ─── Send grievance acknowledgement on submission ──────────────────────────
    public void sendSubmissionAck(Grievance g) {
        if (!mailEnabled || g.getEmail() == null || g.getEmail().isBlank()) return;

        String subject = "Grievance Received — GR-" + String.format("%04d", g.getId());
        String body = String.format(
            "Dear %s,\n\n" +
            "Your grievance has been received and assigned ID: GR-%04d\n\n" +
            "Category : %s\n" +
            "Priority  : %s\n" +
            "Due Date  : %s\n\n" +
            "You can track the status by logging into the Grievance Portal.\n\n" +
            "Regards,\nGrievance Cell",
            g.getStudentName(), g.getId(),
            g.getCategory(), g.getPriority(),
            g.getDueDate() != null ? g.getDueDate().toLocalDate().toString() : "TBD"
        );
        send(g.getEmail(), subject, body);
    }

    // ─── Send status update email ──────────────────────────────────────────────
    public void sendStatusUpdate(Grievance g) {
        if (!mailEnabled || g.getEmail() == null || g.getEmail().isBlank()) return;

        String subject = "Grievance GR-" + String.format("%04d", g.getId())
                       + " Status Updated: " + g.getStatus();
        String body = String.format(
            "Dear %s,\n\n" +
            "Your grievance (GR-%04d) status has been updated.\n\n" +
            "New Status : %s\n" +
            "Assigned To: %s\n" +
            "Remarks    : %s\n\n" +
            "Log in to the portal to view full details.\n\n" +
            "Regards,\nGrievance Cell",
            g.getStudentName(), g.getId(),
            g.getStatus(),
            g.getAssignedTo() != null ? g.getAssignedTo() : "—",
            g.getRemarks()    != null ? g.getRemarks()    : "—"
        );
        send(g.getEmail(), subject, body);
    }

    // ─── Send overdue alert ────────────────────────────────────────────────────
    public void sendOverdueAlert(Grievance g) {
        if (!mailEnabled || g.getEmail() == null || g.getEmail().isBlank()) return;

        String subject = "Action Required — Grievance GR-"
                       + String.format("%04d", g.getId()) + " is Overdue";
        String body = String.format(
            "Dear %s,\n\n" +
            "Your grievance (GR-%04d) has exceeded the resolution deadline.\n\n" +
            "Category : %s\n" +
            "Priority  : %s\n" +
            "Due Date  : %s\n" +
            "Status    : %s\n\n" +
            "Please contact the grievance cell for an update.\n\n" +
            "Regards,\nGrievance Cell",
            g.getStudentName(), g.getId(),
            g.getCategory(), g.getPriority(),
            g.getDueDate() != null ? g.getDueDate().toLocalDate().toString() : "—",
            g.getStatus()
        );
        send(g.getEmail(), subject, body);
    }

    // ─── Private helper ────────────────────────────────────────────────────────
    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            System.out.println("[EmailService] Sent '" + subject + "' to " + to);
        } catch (Exception e) {
            // Log but never crash the main flow due to email failure
            System.err.println("[EmailService] Failed to send email to " + to
                               + " — " + e.getMessage());
        }
    }
}