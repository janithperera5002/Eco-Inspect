package com.ecoinspect.ai.dto;

import com.ecoinspect.ai.entity.enums.CaseStatus;
import com.ecoinspect.ai.entity.enums.UpdateType;
import lombok.Data;

@Data
public class CaseUpdateDTO {
    private Integer officerId;
    private CaseStatus status;
    private String note;
    private UpdateType updateType;
    private String resolutionNotes;
}
