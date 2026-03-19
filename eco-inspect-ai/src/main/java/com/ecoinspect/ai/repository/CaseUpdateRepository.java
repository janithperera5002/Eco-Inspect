package com.ecoinspect.ai.repository;

import com.ecoinspect.ai.entity.CaseUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseUpdateRepository extends JpaRepository<CaseUpdate, Integer> {
    List<CaseUpdate> findByIncidentCase_CaseId(Integer caseId);
    List<CaseUpdate> findByIncidentCase_CaseIdOrderByCreatedAtDesc(Integer caseId);
}
