package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.entity.Region;
import com.ecoinspect.ai.entity.ViolationCategory;
import com.ecoinspect.ai.entity.enums.CaseStatus;
import com.ecoinspect.ai.entity.enums.ReportStatus;
import com.ecoinspect.ai.entity.enums.UrgencyLevel;
import com.ecoinspect.ai.repository.AiAnalysisRepository;
import com.ecoinspect.ai.repository.CaseRepository;
import com.ecoinspect.ai.repository.RegionRepository;
import com.ecoinspect.ai.repository.ReportRepository;
import com.ecoinspect.ai.repository.ViolationCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final ReportRepository reportRepository;
    private final CaseRepository caseRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final RegionRepository regionRepository;
    private final ViolationCategoryRepository categoryRepository;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            long totalReports = reportRepository.count();
            long pendingReview = reportRepository.countByStatus(ReportStatus.pending_review);
            long criticalIncidents = aiAnalysisRepository.countByUrgencyLevel(UrgencyLevel.critical);
            long resolvedCases = caseRepository.countByStatus(CaseStatus.closed);
            long activeCases = caseRepository.countByStatusIn(Arrays.asList(
                CaseStatus.open,
                CaseStatus.under_investigation,
                CaseStatus.action_taken
            ));

            return ResponseEntity.ok(Map.of(
                "totalReports",      totalReports,
                "pendingReview",     pendingReview,
                "criticalIncidents", criticalIncidents,
                "resolvedCases",     resolvedCases,
                "activeCases",       activeCases
            ));
        } catch (Exception e) {
            log.error("Analytics error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "totalReports",      0,
                "pendingReview",     0,
                "criticalIncidents", 0,
                "resolvedCases",     0,
                "activeCases",       0
            ));
        }
    }

    @GetMapping("/urgency")
    public ResponseEntity<?> getUrgencyBreakdown() {
        try {
            long critical = aiAnalysisRepository.countByUrgencyLevel(UrgencyLevel.critical);
            long high = aiAnalysisRepository.countByUrgencyLevel(UrgencyLevel.high);
            long medium = aiAnalysisRepository.countByUrgencyLevel(UrgencyLevel.medium);
            long low = aiAnalysisRepository.countByUrgencyLevel(UrgencyLevel.low);
            long total = critical + high + medium + low;

            return ResponseEntity.ok(Map.of(
                "critical", critical,
                "high",     high,
                "medium",   medium,
                "low",      low,
                "total",    total,
                "criticalPct", total > 0 ? Math.round(critical * 100.0 / total) : 0,
                "highPct",  total > 0 ? Math.round(high * 100.0 / total) : 0,
                "mediumPct", total > 0 ? Math.round(medium * 100.0 / total) : 0,
                "lowPct",   total > 0 ? Math.round(low * 100.0 / total) : 0
            ));
        } catch (Exception e) {
            log.error("Urgency breakdown error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/by-region")
    public ResponseEntity<?> getByRegion() {
        try {
            List<Region> regions = regionRepository.findAll();
            
            List<Map<String, Object>> result = regions.stream().map(r -> {
                long count = reportRepository.countByRegion_RegionId(r.getRegionId());
                long resolved = 0;
                try {
                    resolved = caseRepository.countByReport_Region_RegionIdAndStatus(r.getRegionId(), CaseStatus.closed);
                } catch (Exception ignored) {}
                
                return Map.<String, Object>of(
                    "regionId",   r.getRegionId(),
                    "regionName", r.getName(),
                    "total",      count,
                    "resolved",   resolved
                );
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("By region error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getByCategory() {
        try {
            List<ViolationCategory> categories = categoryRepository.findAll();
            
            List<Map<String, Object>> result = categories.stream().map(c -> {
                long count = aiAnalysisRepository.countByCategory_CategoryId(c.getCategoryId());
                return Map.<String, Object>of(
                    "categoryId",   c.getCategoryId(),
                    "categoryName", c.getName(),
                    "count",        count
                );
            }).filter(m -> (Long) m.get("count") > 0)
            .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("By category error: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}
