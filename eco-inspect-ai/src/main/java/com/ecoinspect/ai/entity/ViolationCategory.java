package com.ecoinspect.ai.entity;

import com.ecoinspect.ai.entity.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "violation_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViolationCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 255)
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_default")
    private Severity severityDefault = Severity.medium;

    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AiAnalysis> aiAnalyses;
}
