package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.TokenUsageService;

/** Aggregate statistics endpoints defined under the "Global Statistics" tag of the YAML specification. */
@RestController
@RequestMapping("/api/v1/global")
public class GlobalStatsController {

    private final TokenUsageService tokenUsageService;

    public GlobalStatsController(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    /** {@code GET /api/v1/global/stats} - cumulative token usage since server start. */
    @GetMapping("/stats")
    public GlobalStatsResponse stats() {
        return tokenUsageService.snapshot();
    }
}
