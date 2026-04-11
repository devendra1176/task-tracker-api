package com.project.tasktracker.service;

import com.project.tasktracker.dto.LoginRequestDTO;
import com.project.tasktracker.dto.SignupRequestDTO;

public interface AuthService {
    String signup(SignupRequestDTO dto);

    String login(LoginRequestDTO dto);
}
