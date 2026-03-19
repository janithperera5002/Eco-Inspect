package com.ecoinspect.ai.repository;

import com.ecoinspect.ai.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

import com.ecoinspect.ai.entity.enums.CaseStatus;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {
    Optional<Case> findByCaseReference(String caseReference);
    List<Case> findByAssignedTo_UserId(Integer userId);
    List<Case> findByReport_ReportId(Integer reportId);

    long countByStatusIn(List<CaseStatus> statuses);
    long countByStatusAndClosedAtAfter(CaseStatus status, LocalDateTime date);
    long countByStatus(CaseStatus status);
    long countByAssignedTo_UserId(Integer userId);
    long countByAssignedTo_UserIdAndStatus(Integer userId, CaseStatus status);
    long countByReport_Region_RegionIdAndStatus(Integer regionId, CaseStatus status);
}
