package com.ecom.Backend.controller;

import com.ecom.Backend.entity.Product;
import com.ecom.Backend.services.ProductServices;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatClient chatClient;
    private final ProductServices productServices;

    @Autowired
    public ChatController(ChatModel chatModel, ProductServices productServices) {
        this.chatClient = ChatClient.create(chatModel);
        this.productServices = productServices;
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String reply, String status) {}

    // Tool definition class containing @Tool annotated methods for Spring AI 2.0
    public static class ProductTools {
        private final ProductServices productServices;

        public ProductTools(ProductServices productServices) {
            this.productServices = productServices;
        }

        @Tool(description = "Get the list of all products in the e-commerce catalog database.")
        public List<Product> getAllProducts() {
            System.out.println(">>> [AI Native Tool Call] getAllProducts invoked.");
            List<Product> results = productServices.getAllProducts();
            System.out.println(">>> [AI Native Tool Call] Returned " + results.size() + " products from database.");
            return results;
        }
    }

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
            String systemInstruction = """
                    You are a helpful, direct E-Commerce Assistant.
                    
                    You have access to a tool named `getAllProducts` to fetch all products from the catalog database. Use this tool whenever the user asks to find, search, filter, recommend, or inquire about products.
                    
                    Respond to the user in a natural, conversational chat style.
                    
                    Rules:
                    1. When the user asks about products (e.g. by price such as 'under 50k', specifications, features, categories, or names), call `getAllProducts` to retrieve the entire catalog.
                    2. Analyze the retrieved products against the user's request:
                       - Interpret budget queries correctly (e.g., '50k' refers to 50000.0, '29k' refers to 29000.0, etc.).
                       - Search name, category, description, and specifications/attributes to find matches.
                    3. Answer the user's question directly and conversationally with standard text/markdown chat. Do not format the response with strict database catalog result styling or markdown cards unless requested.
                    4. Suggest only products that exist in the database catalog. If no products match the user's request, politely inform them.
                    """;

            String reply = chatClient.prompt()
                    .system(systemInstruction)
                    .user(message)
                    .tools(new ProductTools(productServices))
                    .call()
                    .content();

            return reply != null ? reply : "### Error\n\nNo response received from the AI model.";
        } catch (Exception e) {
            e.printStackTrace();
            return "### Error\n\nFailed to generate response: " + e.getMessage();
        }
    }
}