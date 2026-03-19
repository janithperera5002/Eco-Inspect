package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.entity.Report;
import com.ecoinspect.ai.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecoinspect.ai.repository.AiAnalysisRepository;
import com.ecoinspect.ai.repository.ReportRepository;
import com.ecoinspect.ai.entity.AiAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Map;
import java.util.Optional;
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final ReportRepository reportRepository;

    
    @GetMapping
    public ResponseEntity<?> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(
                page, size, 
                Sort.by("submittedAt").descending()
            );
            Page<Report> reports = reportRepository.findAll(pageable);
            
            return ResponseEntity.ok(Map.of(
                "content", reports.getContent(),
                "totalElements", reports.getTotalElements(),
                "totalPages", reports.getTotalPages(),
                "page", page
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable Integer id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<?> getAnalysis(@PathVariable Long id) {
        try {
            Optional<AiAnalysis> analysis = 
                aiAnalysisRepository.findByReport_ReportId(id);
            
            if (analysis.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(Map.of("message", "Analysis not found for report " + id));
            }
            
            AiAnalysis a = analysis.get();
            
            return ResponseEntity.ok(Map.of(
                "analysisId",      a.getAnalysisId(),
                "reportId",        id,
                "summary",         a.getSummary() != null ? a.getSummary() : "",
                "category",        a.getCategory() != null ? a.getCategory().getName() : "other",
                "urgencyLevel",    a.getUrgencyLevel() != null ? a.getUrgencyLevel().toString() : "medium",
                "confidenceScore", a.getConfidenceScore() != null ? a.getConfidenceScore() : 0,
                "keywords",        a.getKeywords() != null ? a.getKeywords() : "",
                "sentiment",       a.getSentiment() != null ? a.getSentiment().toString() : "neutral",
                "mediaAnalysis",   a.getMediaAnalysis() != null ? a.getMediaAnalysis() : "",
                "processedAt",     a.getProcessedAt() != null ? a.getProcessedAt().toString() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> createReport(@RequestBody com.ecoinspect.ai.dto.ReportRequestDTO dto) {
        log.info("Received report request: {}", dto);
        try {
            Report saved = reportService.submitReport(dto);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(saved);
        } catch (Exception e) {
            log.error("Error creating report: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // Simple test endpoint — hit this first to confirm
    // the server is reachable at all
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Eco-Inspect API is running");
    }
}