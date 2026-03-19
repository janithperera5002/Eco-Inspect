package com.ecoinspect.ai.entity;

import com.ecoinspect.ai.entity.enums.UpdateType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "update_id")
    private Integer updateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Case incidentCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User officer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false)
    private UpdateType updateType = UpdateType.comment;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
