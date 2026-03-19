package com.ecoinspect.ai.entity;

import com.ecoinspect.ai.entity.enums.UrgencyLevel;
import com.ecoinspect.ai.entity.enums.Sentiment;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "ai_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Integer analysisId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonBackReference
    private Report report;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private ViolationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level")
    private UrgencyLevel urgencyLevel;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(columnDefinition = "JSON")
    private String keywords;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    @Column(name = "media_analysis", columnDefinition = "TEXT")
    private String mediaAnalysis;

    @Column(name = "location_verified")
    private Boolean locationVerified = false;

    @Column(name = "raw_ai_response", columnDefinition = "JSON")
    private String rawAiResponse;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "processed_at", updatable = false)
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}
