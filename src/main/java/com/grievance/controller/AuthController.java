package com.grievance.controller;

import com.grievance.model.User;
import com.grievance.repository.UserRepository;
import com.grievance.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    // FIX: @Lazy on AuthenticationManager breaks the circular dependency:
    //      AuthController -> AuthenticationManager -> SecurityConfig
    //      -> UserDetailsServiceImpl -> UserRepository (cycle).
    @Lazy
    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepository;

    // FIX: @Lazy on PasswordEncoder for the same reason.
    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    // ─── POST /api/auth/register ──────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> body) {

        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "").trim();
        String email    = body.getOrDefault("email",    "").trim();
        String fullName = body.getOrDefault("fullName", "").trim();

        Map<String, Object> resp = new LinkedHashMap<>();

        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            resp.put("error", "Username, password and email are required.");
            return ResponseEntity.badRequest().body(resp);
        }
        if (password.length() < 6) {
            resp.put("error", "Password must be at least 6 characters.");
            return ResponseEntity.badRequest().body(resp);
        }
        if (userRepository.existsByUsername(username)) {
            resp.put("error", "Username already taken.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }
        if (userRepository.existsByEmail(email)) {
            resp.put("error", "Email already registered.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }

        // FIX: use setters instead of all-args constructor —
        //      immune to constructor signature changes and clearer to read.
        User student = new User();
        student.setUsername(username);
        student.setPassword(passwordEncoder.encode(password));
        student.setEmail(email);
        student.setRole("ROLE_STUDENT");   // self-registered users are always students
        student.setFullName(fullName.isBlank() ? username : fullName);

        userRepository.save(student);

        resp.put("message", "Registration successful. You can now log in.");
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body) {

        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "").trim();

        Map<String, Object> resp = new LinkedHashMap<>();

        if (username.isBlank() || password.isBlank()) {
            resp.put("error", "Username and password are required.");
            return ResponseEntity.badRequest().body(resp);
        }

        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            String      token   = jwtUtils.generateToken(authentication);
            UserDetails ud      = (UserDetails) authentication.getPrincipal();
            String      role    = ud.getAuthorities().iterator().next().getAuthority();
            String      fullName = userRepository.findByUsername(username)
                                       .map(User::getFullName)
                                       .orElse(username);

            resp.put("token",    token);
            resp.put("username", username);
            resp.put("role",     role);        // "ROLE_ADMIN" | "ROLE_STUDENT"
            resp.put("fullName", fullName);
            return ResponseEntity.ok(resp);

        } catch (BadCredentialsException e) {
            resp.put("error", "Invalid username or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
    }
}