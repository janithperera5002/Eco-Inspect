package com.ecoinspect.ai.config;

import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.entity.enums.UserRole;
import com.ecoinspect.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed test officer account if not already present
        seedUserIfAbsent(
                "officer@ecoinspect.com",
                "password",
                "Test Officer",
                "+94771234567",
                UserRole.officer);

        seedUserIfAbsent(
                "test@test.com",
                "1234",
                "Test User",
                "+94779999999",
                UserRole.officer);

        seedUserIfAbsent(
                "admin@ecoinspect.com",
                "admin123",
                "Admin User",
                "+94770000000",
                UserRole.admin);
    }

    private void seedUserIfAbsent(String email, String rawPassword, String fullName,
            String phone, UserRole role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(rawPassword); // Storing as plain text for simple login
            user.setFullName(fullName);
            user.setPhoneNumber(phone);
            user.setRole(role);
            user.setIsActive(true);
            userRepository.save(user);
            log.info("Seeded user: {}", email);
        } else {
            log.info("User already exists: {}", email);
        }
    }
}
