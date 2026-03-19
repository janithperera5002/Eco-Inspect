package com.ecoinspect.ai.service;

import com.ecoinspect.ai.dto.AiriaResponseDto;
import com.ecoinspect.ai.entity.AiAnalysis;
import com.ecoinspect.ai.entity.Report;
import com.ecoinspect.ai.entity.ViolationCategory;
import com.ecoinspect.ai.entity.enums.UrgencyLevel;
import com.ecoinspect.ai.repository.AiAnalysisRepository;
import com.ecoinspect.ai.repository.ViolationCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiriaIntegrationService {

    @Value("${airia.api.url}")
    private String apiUrl;

    @Value("${airia.agent.id}")
    private String agentId;

    @Value("${airia.api.key}")
    private String apiKey;

    private final AiAnalysisRepository aiAnalysisRepository;
    private final ViolationCategoryRepository violationCategoryRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Analyzes a report using the Airia API.
     *
     * @param report the report to analyze
     * @param mediaDescription the description of attached media (if any)
     * @return the saved AiAnalysis object
     */
    public AiAnalysis analyzeReport(Report report, String mediaDescription) {
        // URL is the full endpoint — agentId is now embedded in airia.api.url
        String url = apiUrl;
        log.info("Calling Airia API at: {}", url);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("CRITICAL: Airia API Key is null or empty!");
        } else {
            log.debug("API Key loaded successfully, starts with: {}", apiKey.substring(0, Math.min(apiKey.length(), 4)));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey.trim());
        headers.set("Authorization", "Bearer " + apiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String finalInput = report.getRawMessage();
        if (mediaDescription != null && !mediaDescription.isBlank()) {
            finalInput += "\n[Attached Media Description]: " + mediaDescription;
        }

        // Airia v2 PipelineExecution request format — PascalCase keys required
        Map<String, Object> body = Map.of(
            "UserInput", finalInput,
            "Asynchronous", false
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        AiAnalysis aiAnalysis = new AiAnalysis();
        // 6. Saving: Set the Report relationship
        aiAnalysis.setReport(report);
        aiAnalysis.setModelVersion("Airia-v2-Live");

        try {
            long startTime = System.currentTimeMillis();
            
            log.info("Calling Airia v2 API...");
            log.info("URL: {}", url);
            log.info("Message: {}", 
                finalInput != null ? finalInput.substring(0, Math.min(100, finalInput.length())) : "NULL");

            // 1. API Call
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            long processingTime = System.currentTimeMillis() - startTime;

            aiAnalysis.setProcessingTimeMs((int) processingTime);
            String responseBody = response.getBody();
            
            log.info("Airia Status: {}", response.getStatusCode());
            log.info("Airia Raw Response: {}", responseBody);
            
            // Always store the raw full response immediately
            aiAnalysis.setRawAiResponse(responseBody != null ? responseBody.substring(0, Math.min(responseBody.length(), 2000)) : "null");
            
            if (responseBody != null) {
                try {
                    // 1. Fully Robust Extraction of Result String
                    String cleanedJson = extractJsonFromAiriaResponse(responseBody);
                    log.debug("Cleaned Result JSON: {}", cleanedJson);
                    
                    // 2. Update raw_ai_response (Safely storing only the cleaned JSON)
                    aiAnalysis.setRawAiResponse(cleanedJson);
                    
                    // 3. Parse into DTO using Jackson
                    JsonNode finalData = objectMapper.readTree(cleanedJson);
                    
                    // Handle potential nested wrapper object inside the cleaned JSON (e.g. {"analysis": { ... }})
                    if (finalData.has("analysis") && finalData.get("analysis").isObject()) {
                        finalData = finalData.get("analysis");
                    } else if (finalData.has("result") && finalData.get("result").isObject()) {
                        finalData = finalData.get("result");
                    } else if (finalData.has("output") && finalData.get("output").isObject()) {
                        finalData = finalData.get("output");
                    }
                    
                    AiriaResponseDto dto = objectMapper.treeToValue(finalData, AiriaResponseDto.class);
                    log.info("Mapped JSON to DTO object: {}", dto);
                    
                    // 6. Data Mapping to Entity
                    if (dto.getSummary() != null) {
                        aiAnalysis.setSummary(dto.getSummary());
                        log.info("Extracted summary: {}", aiAnalysis.getSummary());
                    } else {
                        log.warn("Summary field was null in inner JSON");
                        aiAnalysis.setSummary("AI extraction missing summary");
                    }
                    
                    if (dto.getUrgencyLevel() != null) {
                        try {
                            aiAnalysis.setUrgencyLevel(UrgencyLevel.valueOf(dto.getUrgencyLevel().toLowerCase()));
                            log.info("Extracted urgency_level: {}", aiAnalysis.getUrgencyLevel());
                        } catch (IllegalArgumentException e) {
                            log.warn("Unknown urgency level from AI: {}", dto.getUrgencyLevel());
                        }
                    } else {
                        log.warn("UrgencyLevel field was null in inner JSON");
                    }

                    if (dto.getConfidenceScore() != null) {
                        aiAnalysis.setConfidenceScore(BigDecimal.valueOf(dto.getConfidenceScore()));
                        log.info("Extracted confidence_score: {}", aiAnalysis.getConfidenceScore());
                    } else {
                        log.warn("ConfidenceScore field was null in inner JSON");
                    }
                    
                    if (dto.getCategoryId() != null) {
                        violationCategoryRepository.findById(dto.getCategoryId()).ifPresentOrElse(
                            c -> {
                                aiAnalysis.setCategory(c);
                                log.info("Linked Violation Category ID: {}", c.getCategoryId());
                            },
                            () -> log.warn("Violation Category ID {} not found in database", dto.getCategoryId())
                        );
                    } else {
                        log.warn("Extracted category_id is missing or null!");
                    }

                    // 1. Keywords mapping (Must be valid JSON for the database column)
                    if (dto.getKeywords() != null && !dto.getKeywords().isEmpty()) {
                        try {
                            aiAnalysis.setKeywords(objectMapper.writeValueAsString(dto.getKeywords()));
                        } catch (Exception e) {
                            aiAnalysis.setKeywords("[]");
                        }
                    } else {
                        aiAnalysis.setKeywords("[]");
                    }

                    // 2. Sentiment mapping
                    if (dto.getSentiment() != null) {
                        try {
                            aiAnalysis.setSentiment(com.ecoinspect.ai.entity.enums.Sentiment.valueOf(dto.getSentiment().toLowerCase().trim()));
                        } catch (IllegalArgumentException e) {
                            aiAnalysis.setSentiment(com.ecoinspect.ai.entity.enums.Sentiment.neutral);
                        }
                    } else {
                        aiAnalysis.setSentiment(com.ecoinspect.ai.entity.enums.Sentiment.neutral);
                    }

                    // 3. Media analysis mapping
                    aiAnalysis.setMediaAnalysis(dto.getMediaAnalysis() != null ? dto.getMediaAnalysis() : "");

                    // 4. Location verified mapping
                    aiAnalysis.setLocationVerified(report.getLatitude() != null && report.getLongitude() != null);

                    log.info("Successfully mapped all fields for report ID: {}", report.getReportId());
                // 7. Safety: Robust try-catch
                } catch (com.fasterxml.jackson.core.JsonProcessingException jsonException) {
                    log.error("Failed to parse cleaned JSON inside result. Raw body: {}", responseBody, jsonException);
                    aiAnalysis.setSummary("AI Analysis Result JSON Parse Error: " + jsonException.getMessage());
                } catch (Exception parseException) {
                    log.error("Failed to parse AI output. Raw body: {}", responseBody, parseException);
                    // Ensure at least a failure message is saved
                    aiAnalysis.setSummary("AI Analysis Parse Error: " + parseException.getMessage());
                }
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Airia HTTP CLIENT ERROR: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            aiAnalysis.setSummary("AI Analysis Failed (HTTP Error): HTTP " + e.getStatusCode() + " - " + 
                (e.getResponseBodyAsString().length() > 200 ? e.getResponseBodyAsString().substring(0, 200) : e.getResponseBodyAsString()));
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Airia HTTP SERVER ERROR: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            aiAnalysis.setSummary("AI Analysis Failed (Server Error): " + e.getStatusCode());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Airia CONNECTION ERROR: {}", e.getMessage());
            aiAnalysis.setSummary("AI Analysis Failed (Cannot connect to Airia API)");
        } catch (Exception e) {
            log.error("Airia UNEXPECTED ERROR: {}", e.getMessage(), e);
            aiAnalysis.setSummary("AI Analysis Failed (Unexpected Error): " + e.getMessage());
        }

        // 6. Saving: set processed_at to LocalDateTime.now(), and finally call aiAnalysisRepository.save(aiAnalysis)
        aiAnalysis.setProcessedAt(java.time.LocalDateTime.now());
        return aiAnalysisRepository.save(aiAnalysis);
    }

    /**
     * Extracts pure JSON from an Airia response handling multiple wrapper formats:
     * - plain JSON
     * - JSON wrapped in ```json ... ``` markdown
     * - JSON nested inside {"output": "..."} or {"content":[{"text":"..."}]}
     * - JSON nested inside {"result": "..."}
     */
    public String extractJsonFromAiriaResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "{}";
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String rawResult = responseBody;
            
            // 1. Check for various wrapper formats
            if (rootNode.has("result")) {
                rawResult = rootNode.get("result").isTextual() ? 
                            rootNode.get("result").asText() : rootNode.get("result").toString();
            } else if (rootNode.has("output")) {
                rawResult = rootNode.get("output").isTextual() ? 
                            rootNode.get("output").asText() : rootNode.get("output").toString();
            } else if (rootNode.has("content") && rootNode.get("content").isArray() && rootNode.get("content").size() > 0) {
                JsonNode firstContent = rootNode.get("content").get(0);
                if (firstContent.has("text")) {
                    rawResult = firstContent.get("text").asText();
                }
            }

            // 2. Clean markdown blocks
            rawResult = rawResult.replace("```json", "").replace("```", "").trim();

            // 3. Extract using { } bounds to isolate the JSON object
            int startIndex = rawResult.indexOf('{');
            int endIndex = rawResult.lastIndexOf('}');
            
            if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                return rawResult.substring(startIndex, endIndex + 1);
            }
            
            return rawResult;
        } catch (Exception e) {
            log.warn("Failed to parse response body as JSON tree, falling back to string extraction", e);
            // Fallback: just clean string
            String cleaned = responseBody.replace("```json", "").replace("```", "").trim();
            int startIndex = cleaned.indexOf('{');
            int endIndex = cleaned.lastIndexOf('}');
            if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                return cleaned.substring(startIndex, endIndex + 1);
            }
            return cleaned;
        }
    }
}
