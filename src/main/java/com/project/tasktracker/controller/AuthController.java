package com.project.tasktracker.controller;

import com.project.tasktracker.dto.ApiResponse;
import com.project.tasktracker.dto.AuthResponseDTO;
import com.project.tasktracker.dto.LoginRequestDTO;
import com.project.tasktracker.dto.SignupRequestDTO;
import com.project.tasktracker.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody SignupRequestDTO dto) {
        log.info("Received signup request for email: {}", dto.getEmail());
        String result = authService.signup(dto);

        ApiResponse<String> response = new ApiResponse<>(
                true,
                result,
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO dto) {
        log.info("Received login request for email: {}", dto.getEmail());
        AuthResponseDTO authData = authService.login(dto);

        ApiResponse<AuthResponseDTO> response = new ApiResponse<>(
                true,
                "Login successful",
                authData,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

}