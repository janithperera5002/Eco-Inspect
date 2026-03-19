package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.entity.enums.CaseStatus;
import com.ecoinspect.ai.repository.CaseRepository;
import com.ecoinspect.ai.repository.ReportRepository;
import com.ecoinspect.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final ReportRepository reportRepository;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Integer userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            long casesAssigned = caseRepository.countByAssignedTo_UserId(userId);
            long casesResolved = caseRepository.countByAssignedTo_UserIdAndStatus(userId, CaseStatus.closed);

            return ResponseEntity.ok(Map.of(
                "userId",        user.getUserId(),
                "fullName",      user.getFullName(),
                "email",         user.getEmail(),
                "phone",         user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                "role",          user.getRole().toString(),
                "isActive",      user.getIsActive(),
                "createdAt",     user.getCreatedAt().toString(),
                "casesAssigned", casesAssigned,
                "casesResolved", casesResolved
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Integer userId,
            @RequestBody Map<String, String> body) {
        try {
            User user = userRepository.findById(userId).orElseThrow();
            if (body.containsKey("fullName")) {
                user.setFullName(body.get("fullName"));
            }
            if (body.containsKey("phone")) {
                user.setPhoneNumber(body.get("phone"));
            }
            return ResponseEntity.ok(userRepository.save(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(Map.of("message", "No account found with that email address"));
            }
            return ResponseEntity.ok(Map.of(
                "message", "If an account exists for " + email + ", a reset link has been sent.",
                "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String newPassword = body.get("newPassword");

            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.status(400)
                    .body(Map.of("message", "Password must be at least 6 characters"));
            }

            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPasswordHash(newPassword);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully",
                "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}
