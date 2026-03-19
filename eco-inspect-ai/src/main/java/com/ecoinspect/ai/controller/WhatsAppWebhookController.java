package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.dto.ReportRequestDTO;
import com.ecoinspect.ai.entity.Report;
import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.repository.UserRepository;
import com.ecoinspect.ai.service.ReportService;
import com.ecoinspect.ai.service.TwilioWhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WhatsAppWebhookController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final TwilioWhatsAppService twilioService;

    @PostMapping(value = "/twilio",
        consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<String> receiveTwilio(
        @RequestParam(value="Body", defaultValue="") String body,
        @RequestParam(value="From", defaultValue="") String from,
        @RequestParam(value="MessageSid", defaultValue="") String messageSid,
        @RequestParam(value="MediaUrl0", required=false) String mediaUrl) {

        log.info("=== TWILIO WEBHOOK HIT ===");
        log.info("Raw From: [{}]", from);
        log.info("Raw Body: [{}]", body);
        log.info("SID: [{}]", messageSid);
        log.info("Body blank: {}", body.isBlank());

        try {
            if (body.isBlank()) {
                return ResponseEntity.ok(
                    "<?xml version=\"1.0\"?>" +
                    "<Response><Message>" +
                    "Please send a text message." +
                    "</Message></Response>"
                );
            }

            String cleanPhone = from.replace("whatsapp:", "").trim();

            User citizen = userRepository
                .findByPhoneNumber(cleanPhone)
                .orElseGet(() -> {
                    try {
                        User u = new User();
                        u.setPhoneNumber(cleanPhone);
                        u.setFullName("Citizen " + cleanPhone);
                        u.setRole(com.ecoinspect.ai.entity.enums.UserRole.citizen);
                        u.setIsActive(true);
                        u.setCreatedAt(LocalDateTime.now());
                        return userRepository.save(u);
                    } catch (Exception e) {
                        log.error("Could not create citizen: {}", e.getMessage());
                        // Return existing first citizen as absolute fallback
                        return userRepository.findById(6)
                            .orElse(null);
                    }
                });

            ReportRequestDTO dto = new ReportRequestDTO();
            dto.setRawMessage(body);
            dto.setChannel("whatsapp");
            if (citizen != null) {
                dto.setReporterId((long) citizen.getUserId());
            }

            if (mediaUrl != null && !mediaUrl.isBlank()) {
                dto.setMediaDescription("Photo attached: " + mediaUrl);
            }

            final String phoneForCallback = cleanPhone;
            new Thread(() -> {
                try {
                    log.info("Thread started for: {}", cleanPhone);
                    log.info("DTO message: {}", dto.getRawMessage());
                    log.info("DTO channel: {}", dto.getChannel());
                    log.info("DTO reporterId: {}", dto.getReporterId());
                    
                    Report saved = reportService.submitReport(dto);
                    
                    log.info("SUCCESS - Report ID: {}", saved.getReportId());
                    
                    twilioService.sendMessage(
                        phoneForCallback,
                        "🌿 *Eco-Inspect AI*\n\n" +
                        "✅ Thank you for your report! Our AI has analyzed your message.\n\n" +
                        "📋 Your report has been logged and an environmental officer will be assigned soon.\n\n" +
                        "You will receive updates here as your case progresses. 🌱"
                    );
                } catch (Exception e) {
                    log.error("THREAD FAILED: {}", e.getMessage(), e);
                    twilioService.sendMessage(phoneForCallback, "⚠️ Sorry, we could not process your report.");
                }
            }).start();

            return ResponseEntity.ok()
                .header("Content-Type", "text/xml")
                .body("<?xml version=\"1.0\"?>" +
                    "<Response><Message>" +
                    "🌿 Eco-Inspect AI\n\n" +
                    "✅ Report received! Our AI is analyzing your message now.\n\n" +
                    "You will receive updates on your case. Thank you! 🌱" +
                    "</Message></Response>"
                );

        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                "<?xml version=\"1.0\"?>" +
                "<Response><Message>" +
                "Error occurred. Try again." +
                "</Message></Response>"
            );
        }
    }

    @GetMapping("/twilio/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Twilio webhook is ready ✅");
    }

    @GetMapping("/twilio/test")
    public ResponseEntity<?> testWebhook() {
        try {
            String testPhone = "+94771234567";
            String testMessage = 
                "Test: Illegal waste dumping near Kelani River. Chemical smell very strong.";

            User citizen = userRepository
                .findByPhoneNumber(testPhone)
                .orElseGet(() -> {
                    User u = new User();
                    u.setPhoneNumber(testPhone);
                    u.setFullName("Test Citizen");
                    u.setRole(com.ecoinspect.ai.entity.enums.UserRole.citizen);
                    u.setIsActive(true);
                    u.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });

            ReportRequestDTO dto = new ReportRequestDTO();
            dto.setRawMessage(testMessage);
            dto.setChannel("whatsapp");
            dto.setReporterId((long) citizen.getUserId());

            reportService.submitReport(dto);

            return ResponseEntity.ok(
                java.util.Map.of(
                    "status", "success",
                    "message", "Test report created and AI analyzed",
                    "phone", testPhone,
                    "reportMessage", testMessage
                )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(java.util.Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
        }
    }

    @GetMapping("/twilio/debug")
    public ResponseEntity<?> debugAiria() {
        return ResponseEntity.ok(reportService.getAllReports().stream()
                .sorted((a,b)->b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .limit(3)
                .map(r -> java.util.Map.of(
                    "report_id", r.getReportId(),
                    "summary", r.getAiAnalysis() != null ? r.getAiAnalysis().getSummary() : "null",
                    "raw_response", r.getAiAnalysis() != null ? (r.getAiAnalysis().getRawAiResponse() != null ? r.getAiAnalysis().getRawAiResponse().substring(0, Math.min(200, r.getAiAnalysis().getRawAiResponse().length())) : "null") : "null",
                    "urgency_level", r.getAiAnalysis() != null && r.getAiAnalysis().getUrgencyLevel() != null ? r.getAiAnalysis().getUrgencyLevel().toString() : "null",
                    "confidence_score", r.getAiAnalysis() != null && r.getAiAnalysis().getConfidenceScore() != null ? r.getAiAnalysis().getConfidenceScore().toString() : "null"
                )).toList());
    }
}
