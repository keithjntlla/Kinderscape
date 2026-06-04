package com.enrollment.system;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.enrollment.system.model")
@EnableJpaRepositories("com.enrollment.system.repository")
public class SpringBootApp {
    // Spring Boot application context
    // Note: This is started by EnrollmentSystemApplication
}