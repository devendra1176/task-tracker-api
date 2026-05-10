package com.project.tasktracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiRequestDTO {

    @NotBlank(message = "Prompt cannot be blank")
    @Size(max = 500, message = "Prompt cannot exceed 500 characters")
    private String prompt;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}