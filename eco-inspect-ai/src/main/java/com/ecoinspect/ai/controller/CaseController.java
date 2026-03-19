package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.entity.Case;
import com.ecoinspect.ai.entity.CaseUpdate;
import com.ecoinspect.ai.entity.Report;
import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.entity.enums.CasePriority;
import com.ecoinspect.ai.entity.enums.CaseStatus;
import com.ecoinspect.ai.entity.enums.UpdateType;
import com.ecoinspect.ai.repository.CaseRepository;
import com.ecoinspect.ai.repository.CaseUpdateRepository;
import com.ecoinspect.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CaseController {

    private final CaseRepository caseRepository;
    private final CaseUpdateRepository caseUpdateRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Case> cases = caseRepository.findAll(pageable);
            return ResponseEntity.ok(Map.of(
                "content",       cases.getContent(),
                "totalElements", cases.getTotalElements(),
                "totalPages",    cases.getTotalPages(),
                "page",          page
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCaseById(@PathVariable Integer id) {
        return caseRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCase(@RequestBody Map<String, Object> body) {
        try {
            Case newCase = new Case();

            Integer reportId = Integer.valueOf(body.get("reportId").toString());
            Report report = new Report();
            report.setReportId(reportId);
            newCase.setReport(report);

            String priority = body.getOrDefault("priority", "medium").toString();
            newCase.setPriority(CasePriority.valueOf(priority));

            newCase.setStatus(CaseStatus.open);
            newCase.setDueDate(LocalDate.now().plusDays(7));

            Case saved = caseRepository.save(newCase);
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            Case c = caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case not found"));
            c.setStatus(CaseStatus.valueOf(body.get("status")));
            return ResponseEntity.ok(caseRepository.save(c));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/updates")
    public ResponseEntity<?> addUpdate(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            CaseUpdate update = new CaseUpdate();

            Case c = new Case();
            c.setCaseId(id);
            update.setIncidentCase(c);

            // officer is required — default to first admin user if not provided
            String officerIdStr = body.getOrDefault("officerId", "1");
            User officer = new User();
            officer.setUserId(Integer.parseInt(officerIdStr));
            update.setOfficer(officer);

            update.setNote(body.get("note"));

            String updateTypeStr = body.getOrDefault("updateType", "comment");
            update.setUpdateType(UpdateType.valueOf(updateTypeStr));

            return ResponseEntity.status(201).body(caseUpdateRepository.save(update));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<?> getTimeline(@PathVariable Integer id) {
        try {
            List<CaseUpdate> updates = caseUpdateRepository
                .findByIncidentCase_CaseIdOrderByCreatedAtDesc(id);
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveCase(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            Case c = caseRepository.findById(id).orElseThrow();
            c.setStatus(CaseStatus.closed);
            c.setResolutionNotes(body.get("resolutionNotes"));
            c.setClosedAt(LocalDateTime.now());
            return ResponseEntity.ok(caseRepository.save(c));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}
