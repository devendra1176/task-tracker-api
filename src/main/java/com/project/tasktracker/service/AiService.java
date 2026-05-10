package com.project.tasktracker.service;

public interface AiService {

    String generateTaskSummary();

    String askAiAboutTasks(String userPrompt);
}