package com.project.tasktracker.service.impl;

import com.project.tasktracker.dto.LoginRequestDTO;
import com.project.tasktracker.dto.SignupRequestDTO;
import com.project.tasktracker.entity.User;
import com.project.tasktracker.repository.UserRepository;
import com.project.tasktracker.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    // SIGNUP LOGIC
    @Override
    public String signup(SignupRequestDTO dto) {

        // 1. Check if user already exists
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists with this email");
        }

        // 2. Create new user
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());

        // ⚠️ password abhi plain store ho raha hai (next step me encrypt karenge)
        user.setPassword(dto.getPassword());

        // 3. Save to DB
        userRepository.save(user);

        return "User registered successfully";
    }

    // LOGIN LOGIC
    @Override
    public String login(LoginRequestDTO dto) {

        // 1. Find user by email
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Match password
        if (!user.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return "Login successful";
    }
}