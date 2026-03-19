package com.ecoinspect.ai.repository;

import com.ecoinspect.ai.entity.ViolationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationCategoryRepository extends JpaRepository<ViolationCategory, Integer> {
}
