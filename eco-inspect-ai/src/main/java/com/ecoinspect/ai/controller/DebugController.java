package com.ecoinspect.ai.controller;

import com.ecoinspect.ai.service.AiriaIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final AiriaIntegrationService airiaIntegrationService;

    // Allows POSTing an arbitrary custom response string directly for testing
    @PostMapping("/airia-raw")
    public ResponseEntity<String> testExtractJsonPost(@RequestBody String rawPayload) {
        String extracted = airiaIntegrationService.extractJsonFromAiriaResponse(rawPayload);
        return ResponseEntity.ok(extracted);
    }
    
    // For browser testing: executes a series of tests through the extraction function implicitly
    @GetMapping("/airia-raw")
    public ResponseEntity<Map<String, String>> testExtractJsonGet() {
        Map<String, String> results = new LinkedHashMap<>();
        
        String plainJson = "{\"summary\": \"Test Plain\", \"category_id\": 1, \"urgency_level\": \"high\"}";
        results.put("1_plain_json", airiaIntegrationService.extractJsonFromAiriaResponse(plainJson));
        
        String markdownJson = "```json\n{\"summary\": \"Test Markdown\"}\n```";
        results.put("2_markdown_wrapped", airiaIntegrationService.extractJsonFromAiriaResponse(markdownJson));
        
        String outputWrapped = "{\"output\": \"```json\\n{\\\"summary\\\": \\\"Test Output\\\"}\\n```\"}";
        results.put("3_output_wrapped", airiaIntegrationService.extractJsonFromAiriaResponse(outputWrapped));
        
        String contentWrapped = "{\"content\":[{\"text\":\"```json\\n{\\\"summary\\\": \\\"Test Content Array\\\"}\\n```\"}]}";
        results.put("4_content_array_wrapped", airiaIntegrationService.extractJsonFromAiriaResponse(contentWrapped));
        
        return ResponseEntity.ok(results);
    }
}
