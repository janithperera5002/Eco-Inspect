package com.ecoinspect.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportRequestDTO {
    private String rawMessage;
    private Double latitude;
    private Double longitude;
    private String addressText;
    private String channel;
    private Long reporterId;
    private Long regionId;
    private String mediaDescription;
}
