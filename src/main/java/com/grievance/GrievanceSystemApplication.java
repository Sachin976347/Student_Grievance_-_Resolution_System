package com.grievance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // Required for SlaSchedulerService @Scheduled cron to fire
public class GrievanceSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrievanceSystemApplication.class, args);
    }
}