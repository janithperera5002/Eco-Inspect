package com.ecoinspect.ai.repository;

import com.ecoinspect.ai.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Integer> {
    java.util.Optional<AiAnalysis> findByReport_ReportId(Long reportId);
    long countByUrgencyLevel(com.ecoinspect.ai.entity.enums.UrgencyLevel level);
    long countByCategory_CategoryId(Integer categoryId);
}
