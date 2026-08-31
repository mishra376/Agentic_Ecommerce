package com.ecom.Backend.controller;

import com.ecom.Backend.ai.MasterOrchestratorAgent;
import com.ecom.Backend.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thin REST controller for the AI chat endpoint.
 * Delegates all intelligence to the {@link MasterOrchestratorAgent}.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final MasterOrchestratorAgent masterAgent;

    @Autowired
    public ChatController(MasterOrchestratorAgent masterAgent) {
        this.masterAgent = masterAgent;
    }

    // DTO for a single message in conversation history
    public record HistoryMessage(String sender, String text) {}

    // Request / response DTOs
    public record ChatRequest(String message, List<HistoryMessage> history) {}
    public record ChatResponse(String reply, String status) {}

    @PostMapping
    public ChatResponse handleChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            return new ChatResponse("I didn't receive any message. How can I help you today?", "success");
        }

        String userMessage = request.message().trim();
        List<HistoryMessage> history = request.history() != null ? request.history() : List.of();

        // Convert controller DTOs to orchestrator DTOs
        List<MasterOrchestratorAgent.HistoryMessage> orchestratorHistory = history.stream()
                .map(h -> new MasterOrchestratorAgent.HistoryMessage(h.sender(), h.text()))
                .toList();

        Long userId = userDetails != null ? userDetails.getId() : null;
        String reply = masterAgent.chat(userMessage, orchestratorHistory, userId);

        return new ChatResponse(reply, "success");
    }
}