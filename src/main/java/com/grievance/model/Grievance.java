package com.grievance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grievances")
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "email")
    private String email;

    // Academic, Hostel, Fees, Infrastructure, Library, Harassment, Other
    @Column(name = "category", nullable = false)
    private String category;

    // High, Medium, Low
    @Column(name = "priority", nullable = false)
    private String priority;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    // Open, In Progress, Resolved, Closed
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // Row-level security — username of the student who submitted
    @Column(name = "owner_username")
    private String ownerUsername;

    // ── STEP 3a: SLA deadline tracking ──────────────────────────────────────
    // Set automatically at submit time based on priority (from application.properties)
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // Set by the SLA scheduler when dueDate is breached and status is not Resolved/Closed
    @Column(name = "is_overdue")
    private boolean overdue = false;

    // ── STEP 3b: File attachment ─────────────────────────────────────────────
    // Relative path stored in DB; actual file lives in app.upload.dir folder
    @Column(name = "file_path")
    private String filePath;

    // Original filename shown to the user in the UI
    @Column(name = "file_name")
    private String fileName;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Grievance() {}

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String v) { this.studentName = v; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String v) { this.studentId = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }

    public String getPriority() { return priority; }
    public void setPriority(String v) { this.priority = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime v) { this.submittedAt = v; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime v) { this.resolvedAt = v; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String v) { this.assignedTo = v; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String v) { this.ownerUsername = v; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime v) { this.dueDate = v; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean v) { this.overdue = v; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { this.filePath = v; }

    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }

}