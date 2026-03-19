package com.ecoinspect.ai.service;

import com.ecoinspect.ai.dto.CaseCreateDTO;
import com.ecoinspect.ai.dto.CaseUpdateDTO;
import com.ecoinspect.ai.entity.Case;
import com.ecoinspect.ai.entity.CaseUpdate;
import com.ecoinspect.ai.entity.Report;
import com.ecoinspect.ai.entity.User;
import com.ecoinspect.ai.entity.enums.CaseStatus;
import com.ecoinspect.ai.repository.CaseRepository;
import com.ecoinspect.ai.repository.CaseUpdateRepository;
import com.ecoinspect.ai.repository.ReportRepository;
import com.ecoinspect.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseUpdateRepository caseUpdateRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public Case createCase(CaseCreateDTO dto) {
        log.info("Creating a new case for report ID: {}", dto.getReportId());
        
        Report report = reportRepository.findById(dto.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found with ID: " + dto.getReportId()));
                
        User assignedBy = userRepository.findById(dto.getAssignedById())
                .orElseThrow(() -> new IllegalArgumentException("User not found (assignedBy)"));

        User assignedTo = null;
        if (dto.getAssignedToId() != null) {
            assignedTo = userRepository.findById(dto.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("Officer not found (assignedTo)"));
        }

        Case newCase = new Case();
        newCase.setReport(report);
        newCase.setAssignedBy(assignedBy);
        newCase.setAssignedTo(assignedTo);
        newCase.setPriority(dto.getPriority());
        newCase.setCaseReference("CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        newCase.setStatus(CaseStatus.open);

        Case savedCase = caseRepository.save(newCase);
        log.info("Successfully created Case with Reference: {}", savedCase.getCaseReference());
        
        return savedCase;
    }

    public Case getCaseById(Integer caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));
    }

    public List<Case> getAllCases() {
        return caseRepository.findAll();
    }

    @Transactional
    public Case updateCaseStatus(Integer caseId, CaseUpdateDTO dto) {
        Case currentCase = getCaseById(caseId);
        
        User officer = userRepository.findById(dto.getOfficerId())
                .orElseThrow(() -> new IllegalArgumentException("Officer not found"));

        if (dto.getStatus() != null) {
            currentCase.setStatus(dto.getStatus());
            if (dto.getStatus() == CaseStatus.closed) {
                currentCase.setClosedAt(LocalDateTime.now());
                if (dto.getResolutionNotes() != null) {
                    currentCase.setResolutionNotes(dto.getResolutionNotes());
                }
            }
        }

        // Maintain audit trail via CaseUpdate
        CaseUpdate update = new CaseUpdate();
        update.setIncidentCase(currentCase);
        update.setOfficer(officer);
        update.setNote(dto.getNote());
        if (dto.getUpdateType() != null) {
            update.setUpdateType(dto.getUpdateType());
        }
        
        caseUpdateRepository.save(update);
        return caseRepository.save(currentCase);
    }

    @Transactional
    public Case assignOfficer(Integer caseId, Integer officerId, Integer assignerId) {
        Case currentCase = getCaseById(caseId);
        
        User assignedTo = userRepository.findById(officerId)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found"));
                
        User assignedBy = userRepository.findById(assignerId)
                .orElseThrow(() -> new IllegalArgumentException("Assigner not found"));

        currentCase.setAssignedTo(assignedTo);
        
        // Audit log
        CaseUpdate update = new CaseUpdate();
        update.setIncidentCase(currentCase);
        update.setOfficer(assignedBy);
        update.setNote("Case assigned to officer " + assignedTo.getFullName());
        update.setUpdateType(com.ecoinspect.ai.entity.enums.UpdateType.status_change);
        
        caseUpdateRepository.save(update);
        return caseRepository.save(currentCase);
    }
}
