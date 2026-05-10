package com.project.tasktracker.controller;

import com.project.tasktracker.dto.AiRequestDTO;
import com.project.tasktracker.dto.ApiResponse;
import com.project.tasktracker.service.AiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @GetMapping("/tasks/summary")
    public ApiResponse<String> generateTaskSummary() {

        String summary = aiService.generateTaskSummary();

        return new ApiResponse<>(
                true,
                "AI task summary generated successfully",
                summary,
                null
        );
    }

    @PostMapping("/tasks/ask")
    public ApiResponse<String> askAiAboutTasks(
            @Valid @RequestBody AiRequestDTO request
    ) {

        String response = aiService.askAiAboutTasks(request.getPrompt());

        return new ApiResponse<>(
                true,
                "AI response generated successfully",
                response,
                null
        );
    }
}