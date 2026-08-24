package com.ecom.Backend.controller;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatModel chatModel;

    @Autowired
    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String reply, String status) {}

    @PostMapping
    public ChatResponse handleChat(@RequestBody ChatRequest request) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            return new ChatResponse("I didn't receive any message. How can I help you today?", "success");
        }

        String userMessage = request.message().trim();
        String reply = generateAiResponse(userMessage);
        return new ChatResponse(reply, "success");
    }

    private String generateAiResponse(String message) {
        try {
            String systemInstruction = "You are a helpful AI Assistant for our E-Commerce platform. " +
                    "You help users understand our system features and guide them. " +
                    "Here is some information about our system modules:\n\n" +
                    "1. **Users** (`/api/users`)\n" +
                    "   - Register new user accounts via `POST /api/users/register`.\n" +
                    "   - Retrieve user profiles by email, phone, or ID.\n" +
                    "2. **Merchants** (`/api/merchants`)\n" +
                    "   - Register online stores via `POST /api/merchants/register`.\n" +
                    "   - Query shop information using domains.\n" +
                    "3. **Products** (`/api/products`)\n" +
                    "   - Manage active listings, prices, and categories via `GET /api/products`.\n\n" +
                    "Please answer the user's question, keeping answers professional and formatted in Markdown. " +
                    "If the user asks general questions outside of E-Commerce, feel free to answer them politely as well.";

            SystemMessage systemMessage = new SystemMessage(systemInstruction);
            UserMessage userMessage = new UserMessage(message);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            if (response != null && response.getResult() != null && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            return "### Error\n\nNo response received from Gemini AI.";
        } catch (Exception e) {
            e.printStackTrace();
            String detailedError = e.getMessage();
            Throwable cause = e.getCause();
            while (cause != null) {
                detailedError += " | Cause: " + cause.getMessage();
                cause = cause.getCause();
            }
            return "### Error\n\nFailed to generate response from Gemini AI: " + detailedError;
        }
    }
}
