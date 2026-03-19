package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {

        String email = body.get("email");
        String password = body.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401)
                .body(Map.of("message", "Invalid email or password"));
        }

        User user = userOpt.get();

        // Simple password check (direct match for plain-text passwords)
        if (!password.equals(user.getPasswordHash())) {
            return ResponseEntity.status(401)
                .body(Map.of("message", "Invalid email or password"));
        }

        return ResponseEntity.ok(Map.of(
            "token", "eco-token-" + user.getUserId(),
            "userId", user.getUserId(),
            "fullName", user.getFullName(),
            "email", user.getEmail(),
            "role", user.getRole().toString()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("eco-token-")) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid token"));
        }
        try {
            Integer userId = Integer.parseInt(authHeader.replace("eco-token-", ""));
            User user = userRepository.findById(userId).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "token", authHeader,
                "userId", user.getUserId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid token"));
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
