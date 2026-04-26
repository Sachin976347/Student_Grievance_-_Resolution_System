package com.grievance.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Lazy @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean public JwtAuthFilter jwtAuthFilter() { return new JwtAuthFilter(); }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration c)
            throws Exception { return c.getAuthenticationManager(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // ── Public ────────────────────────────────────────────────────
                .requestMatchers("/", "/index.html", "/favicon.ico",
                                 "/static/**", "/*.js", "/*.css").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // ── File upload/download — owner or admin (checked in controller)
                .requestMatchers("/api/files/**").hasAnyRole("STUDENT","ADMIN")

                // ── Analytics reports — ADMIN only ────────────────────────────
                .requestMatchers("/api/reports/**").hasRole("ADMIN")

                // ── Grievance submit — STUDENTS ONLY ──────────────────────────
                // Admin cannot submit grievances — only students can
                .requestMatchers(HttpMethod.POST, "/api/grievances").hasRole("STUDENT")

                // ── Student scoped (own data, read-only) ─────────────────────
                .requestMatchers(HttpMethod.GET, "/api/grievances/mine").hasAnyRole("STUDENT","ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/grievances/mine/**").hasAnyRole("STUDENT","ADMIN")

                // ── Admin full access ─────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/grievances/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/grievances").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/grievances/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/grievances/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .headers(h -> h.addHeaderWriter(new XFrameOptionsHeaderWriter(
                    XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}