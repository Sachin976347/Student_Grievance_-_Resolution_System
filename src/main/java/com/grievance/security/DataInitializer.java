package com.grievance.security;

import com.grievance.model.User;
import com.grievance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    // FIX: @Lazy breaks the circular dependency chain:
    //      DataInitializer -> PasswordEncoder -> SecurityConfig
    //      -> UserDetailsServiceImpl -> UserRepository (cycle)
    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.admin.email}")    private String adminEmail;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {

            // FIX: Build the User object using setters instead of the
            //      all-args constructor so the code is immune to any
            //      future constructor signature changes.
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEmail(adminEmail);
            admin.setRole("ROLE_ADMIN");
            admin.setFullName("System Administrator");

            userRepository.save(admin);

            System.out.println("===========================================");
            System.out.println("  Default admin account created");
            System.out.println("  Username : " + adminUsername);
            System.out.println("  Password : " + adminPassword);
            System.out.println("===========================================");
        }
    }
}