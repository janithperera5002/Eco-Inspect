package com.ecoinspect.ai.repository;

import com.ecoinspect.ai.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecoinspect.ai.entity.enums.UrgencyLevel;

@Repository
public interface ReportRepository extends JpaRepository<Report, Integer> {
    long countByAiAnalysis_UrgencyLevel(UrgencyLevel level);
    long countByStatus(com.ecoinspect.ai.entity.enums.ReportStatus status);
    long countByRegion_RegionId(Integer regionId);
}
