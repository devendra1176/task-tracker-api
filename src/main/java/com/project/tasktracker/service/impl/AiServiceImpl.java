package com.project.tasktracker.service.impl;

import com.project.tasktracker.entity.Task;
import com.project.tasktracker.entity.User;
import com.project.tasktracker.exception.ResourceNotFoundException;
import com.project.tasktracker.repository.TaskRepository;
import com.project.tasktracker.repository.UserRepository;
import com.project.tasktracker.service.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    public AiServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String generateTaskSummary() {

        User currentUser = getCurrentUser();

        // Fetch latest 20 tasks only, to avoid sending too much data to AI
        List<Task> tasks = taskRepository
                .findByUser(currentUser, PageRequest.of(0, 20))
                .getContent();

        if (tasks.isEmpty()) {
            return "You do not have any tasks yet. Start by creating your first task.";
        }

        StringBuilder taskData = new StringBuilder();

        for (Task task : tasks) {
            taskData.append("Title: ").append(task.getTitle()).append("\n")
                    .append("Description: ").append(task.getDescription()).append("\n")
                    .append("Status: ").append(task.getStatus()).append("\n")
                    .append("Priority: ").append(task.getPriority()).append("\n")
                    .append("Due Date: ").append(task.getDueDate()).append("\n\n");
        }

        String prompt = """
                You are a productivity assistant inside a task tracker app.
                
                Analyze the user's tasks and give:
                1. Short overall summary
                2. Most urgent tasks
                3. Suggested execution order
                4. One motivational productivity tip
                
                Keep the response practical, clear, and concise.
                
                User tasks:
                %s
                """.formatted(taskData.toString());

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }

    @Override
    public String askAiAboutTasks(String userPrompt) {

        User currentUser = getCurrentUser();

        List<Task> tasks = taskRepository
                .findByUser(currentUser, PageRequest.of(0, 20))
                .getContent();

        if (tasks.isEmpty()) {
            return "You do not have any tasks yet. Please create some tasks first, then I can help you analyze them.";
        }

        StringBuilder taskData = new StringBuilder();

        for (Task task : tasks) {
            taskData.append("Title: ").append(task.getTitle()).append("\n")
                    .append("Description: ").append(task.getDescription()).append("\n")
                    .append("Status: ").append(task.getStatus()).append("\n")
                    .append("Priority: ").append(task.getPriority()).append("\n")
                    .append("Due Date: ").append(task.getDueDate()).append("\n\n");
        }

        String prompt = """
                You are an AI productivity assistant inside a task tracker application.
                
                You will receive:
                1. The logged-in user's task data
                2. The user's question
                
                Your job:
                - Answer only using the given task context
                - Be practical and concise
                - Do not reveal internal reasoning
                - Do not include <think> tags
                - Do not make up tasks that are not present
                - If the user's question is unrelated to tasks, politely guide them back to task/productivity help
                
                User Tasks:
                %s
                
                User Question:
                %s
                """.formatted(taskData.toString(), userPrompt);

        return chatClient
                .prompt(prompt)
                .call()
                .content()
                .replaceAll("(?s)<think>.*?</think>", "")
                .trim();
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}