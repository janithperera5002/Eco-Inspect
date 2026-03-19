package com.ecoinspect.ai.dto;

import com.ecoinspect.ai.entity.enums.CasePriority;
import lombok.Data;

@Data
public class CaseCreateDTO {
    private Integer reportId;
    private Integer assignedById;
    private CasePriority priority;
    // Optional
    private Integer assignedToId;
}
