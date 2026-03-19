package com.ecoinspect.ai.service;

import com.ecoinspect.ai.entity.AiAnalysis;
import com.ecoinspect.ai.entity.Report;
import com.ecoinspect.ai.repository.AiAnalysisRepository;
import com.ecoinspect.ai.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecoinspect.ai.repository.UserRepository;
import com.ecoinspect.ai.repository.RegionRepository;
import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.entity.Region;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final AiriaIntegrationService airiaIntegrationService;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;

    /**
     * Submits a new report and triggers AI analysis.
     *
     * @param dto the report request to process and save
     * @return the saved report with AI analysis attached
     */
    
    public java.util.List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    public Report getReportById(Integer id) {
        return reportRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Report not found"));
    }

    @Transactional
    public Report submitReport(com.ecoinspect.ai.dto.ReportRequestDTO dto) {
        if (dto.getRawMessage() == null || dto.getRawMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("rawMessage is required and cannot be empty");
        }

        Report report = new Report();
        report.setRawMessage(dto.getRawMessage());
        
        if (dto.getLatitude() != null) {
            report.setLatitude(java.math.BigDecimal.valueOf(dto.getLatitude()));
        }
        if (dto.getLongitude() != null) {
            report.setLongitude(java.math.BigDecimal.valueOf(dto.getLongitude()));
        }
        report.setAddressText(dto.getAddressText());

        if (dto.getReporterId() != null) {
            User reporter = userRepository.findById(dto.getReporterId().intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
            report.setReporter(reporter);
        } else {
            // Fix: the Report entity requires a reporter (nullable = false).
            // If the client omits reporterId, we default to user ID 1.
            User reporter = userRepository.findById(1).orElseGet(() -> {
                User newUser = new User();
                newUser.setFullName("default_web_user");
                newUser.setPhoneNumber("+0000000000");
                newUser.setRole(com.ecoinspect.ai.entity.enums.UserRole.citizen);
                return userRepository.save(newUser);
            });
            report.setReporter(reporter);
        }

        if (dto.getRegionId() != null) {
            Region region = regionRepository.findById(dto.getRegionId().intValue()).orElse(null);
            report.setRegion(region);
        }

        if (dto.getChannel() != null) {
            try {
                report.setChannel(com.ecoinspect.ai.entity.enums.ReportChannel.valueOf(dto.getChannel().toLowerCase()));
            } catch (IllegalArgumentException e) {
                report.setChannel(com.ecoinspect.ai.entity.enums.ReportChannel.whatsapp);
            }
        } else {
            report.setChannel(com.ecoinspect.ai.entity.enums.ReportChannel.whatsapp);
        }

        Report savedReport = reportRepository.save(report);
        log.info("Report saved with ID: {}", savedReport.getReportId());

        String mediaDesc = dto.getMediaDescription() != null ? dto.getMediaDescription() : "";

        log.info("Triggering Airia analysis for report ID: {}", savedReport.getReportId());
        // Since the method is expecting a single Report parameter, let's call it and see if we need to modify AiriaIntegrationService.
        // Wait, AiriaIntegrationService.analyzeReport only accepts (Report report). We must change it to accept mediaDesc or just append it.
        // If the user says "Pass mediaDesc to the AI service instead of dto.getMediaDescription() directly." 
        // Then we must change AiriaIntegrationService.analyzeReport(Report report, String mediaDesc)
        AiAnalysis aiAnalysis = airiaIntegrationService.analyzeReport(savedReport, mediaDesc);
        aiAnalysisRepository.save(aiAnalysis);
        
        savedReport.setAiAnalysis(aiAnalysis);
        log.info("AI analysis saved — urgency: {}, confidence: {}", aiAnalysis.getUrgencyLevel(), aiAnalysis.getConfidenceScore());

        savedReport.setStatus(com.ecoinspect.ai.entity.enums.ReportStatus.pending_review);
        log.info("Report processing complete. Status: pending_review");

        return savedReport;
    }
}
