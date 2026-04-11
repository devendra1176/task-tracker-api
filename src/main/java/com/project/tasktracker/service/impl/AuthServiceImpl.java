package com.project.tasktracker.service.impl;

import com.project.tasktracker.dto.AuthResponseDTO;
import com.project.tasktracker.dto.LoginRequestDTO;
import com.project.tasktracker.dto.SignupRequestDTO;
import com.project.tasktracker.entity.User;
import com.project.tasktracker.repository.UserRepository;
import com.project.tasktracker.service.AuthService;
import com.project.tasktracker.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Override
    public String signup(SignupRequestDTO dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists with this email");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        userRepository.save(user);

        return "User registered successfully";
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()
                )
        );

        String token = jwtService.generateToken(dto.getEmail());

        return new AuthResponseDTO(token, "Login successful");
    }
}