package com.grievance.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIX: increased length to 60 (was 60, safe)
    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false)
    private String password;   // BCrypt-hashed — no length cap needed

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /**
     * ROLE_ADMIN   — full access: view/edit/delete all grievances, dashboard
     * ROLE_STUDENT — restricted: submit, view own submissions only
     *
     * FIX: length widened from 20 to 30 to safely accommodate any future
     *      role names. "ROLE_STUDENT" = 12 chars, "ROLE_ADMIN" = 10 chars,
     *      both well within 30.
     */
    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "full_name", length = 100)
    private String fullName;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public User() {}

    // Convenience constructor — used in AuthController and DataInitializer
    public User(String username, String password, String email,
                String role, String fullName) {
        this.username = username;
        this.password = password;
        this.email    = email;
        this.role     = role;
        this.fullName = fullName;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long   getId()       { return id; }
    public void   setId(Long id){ this.id = id; }

    public String getUsername()              { return username; }
    public void   setUsername(String u)      { this.username = u; }

    public String getPassword()              { return password; }
    public void   setPassword(String p)      { this.password = p; }

    public String getEmail()                 { return email; }
    public void   setEmail(String e)         { this.email = e; }

    public String getRole()                  { return role; }
    public void   setRole(String r)          { this.role = r; }

    public String getFullName()              { return fullName; }
    public void   setFullName(String n)      { this.fullName = n; }
}