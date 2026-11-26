package com.construction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Construction Intelligence Platform Backend.
 * Connects Builders, Contractors, and Clients with role-based access,
 * project management, media storage, inventory tracking, financial summaries,
 * and AI-ready insights.
 */
@SpringBootApplication
@EnableScheduling
public class ConstructionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConstructionApplication.class, args);
    }
}
