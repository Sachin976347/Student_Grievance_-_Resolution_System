package com.grievance.repository;

import com.grievance.model.Grievance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    // ─── By student ───────────────────────────────────────────────────────────
    List<Grievance> findByStudentId(String studentId);
    List<Grievance> findByOwnerUsername(String ownerUsername);
    List<Grievance> findByOwnerUsernameAndStatus(String ownerUsername, String status);
    long countByOwnerUsername(String ownerUsername);

    // ─── By status / category / priority ─────────────────────────────────────
    List<Grievance> findByStatus(String status);
    List<Grievance> findByCategory(String category);
    List<Grievance> findByPriority(String priority);
    List<Grievance> findByCategoryAndStatus(String category, String status);

    // ─── Dashboard counts ─────────────────────────────────────────────────────
    long countByStatus(String status);
    long countByCategory(String category);
    long countByPriority(String priority);

    // ─── SLA / overdue ────────────────────────────────────────────────────────
    List<Grievance> findByOverdueTrue();

    @Query("SELECT g FROM Grievance g WHERE g.dueDate < :now " +
           "AND g.status NOT IN ('Resolved','Closed') AND g.overdue = false")
    List<Grievance> findBreachedSLAs(@Param("now") LocalDateTime now);

    // ─── Pending ordered by priority (admin view) ─────────────────────────────
    @Query("SELECT g FROM Grievance g WHERE g.status IN ('Open','In Progress') ORDER BY " +
           "CASE g.priority WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 ELSE 3 END, g.submittedAt ASC")
    List<Grievance> findPendingOrderedByPriority();

    // ─── Admin dashboard aggregations ─────────────────────────────────────────
    @Query("SELECT g.category, COUNT(g) FROM Grievance g GROUP BY g.category")
    List<Object[]> countByEachCategory();

    @Query("SELECT g.status, COUNT(g) FROM Grievance g GROUP BY g.status")
    List<Object[]> countByEachStatus();

    // ─── Analytics: grievances per month (last 6 months) ─────────────────────
    @Query("SELECT FUNCTION('MONTH', g.submittedAt), FUNCTION('YEAR', g.submittedAt), COUNT(g) " +
           "FROM Grievance g WHERE g.submittedAt >= :since GROUP BY " +
           "FUNCTION('YEAR', g.submittedAt), FUNCTION('MONTH', g.submittedAt) " +
           "ORDER BY FUNCTION('YEAR', g.submittedAt), FUNCTION('MONTH', g.submittedAt)")
    List<Object[]> countByMonth(@Param("since") LocalDateTime since);

    // ─── Analytics: avg resolution time per category ──────────────────────────
    @Query("SELECT g.category, AVG(FUNCTION('TIMESTAMPDIFF', HOUR, g.submittedAt, g.resolvedAt)) " +
           "FROM Grievance g WHERE g.resolvedAt IS NOT NULL GROUP BY g.category")
    List<Object[]> avgResolutionHoursByCategory();

    // ─── Analytics: authority performance ────────────────────────────────────
    @Query("SELECT g.assignedTo, COUNT(g), " +
           "SUM(CASE WHEN g.status='Resolved' THEN 1 ELSE 0 END) " +
           "FROM Grievance g WHERE g.assignedTo IS NOT NULL GROUP BY g.assignedTo")
    List<Object[]> authorityPerformance();

    // ─── Student-scoped aggregations ──────────────────────────────────────────
    @Query("SELECT g.category, COUNT(g) FROM Grievance g WHERE g.ownerUsername = :username GROUP BY g.category")
    List<Object[]> countByEachCategoryForUser(@Param("username") String username);
} 