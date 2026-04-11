package com.project.tasktracker.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponseDTO {

    private String token;
    private String message;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, String message) {
        this.token = token;
        this.message = message;
    }
}