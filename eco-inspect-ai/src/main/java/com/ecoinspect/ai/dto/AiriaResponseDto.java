package com.ecoinspect.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiriaResponseDto {

    private String summary;

    @JsonProperty("category_id")
    private Integer categoryId;

    private String category;

    @JsonProperty("urgency_level")
    private String urgencyLevel;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    private List<String> keywords;

    private String sentiment;

    @JsonProperty("media_analysis")
    private String mediaAnalysis;
}
