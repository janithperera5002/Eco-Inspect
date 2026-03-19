package com.ecoinspect.ai.repository;

import com.ecoinspect.ai.entity.ReportMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportMediaRepository extends JpaRepository<ReportMedia, Integer> {
}
