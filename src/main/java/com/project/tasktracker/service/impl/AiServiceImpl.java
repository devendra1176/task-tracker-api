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
                You are an AI productivity assistant inside a task tracker application.
                
                You are strictly advisory only.
                You cannot create, update, complete, delete, reschedule, or modify any task.
                
                Your job:
                - Analyze the user's tasks and provide a short productivity summary
                - Suggest what seems most important or urgent
                - Give practical next-step guidance
                - Stay grounded only in the given task data
                - Do not invent tasks or changes
                - Do not claim that you performed any action in the application
                
                Output rules:
                - Return plain text only
                - Do not use Markdown
                - Do not use bold, italics, headings, bullets, numbered lists, stars, underscores, or code formatting
                - Do not write labels like "Overall Summary" or "Most Urgent Tasks"
                - Write naturally like a helpful assistant inside a productivity app
                - Keep the response concise, clear, and readable
                
                User Tasks:
                %s
                """.formatted(taskData.toString());

        return chatClient
                .prompt(prompt)
                .call()
                .content()
                .replaceAll("(?s)<think>.*?</think>", "")
                .trim();
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
                
                Context:
                - You can read the user's task data.
                - You can help the user understand priorities, deadlines, workload, and next steps.
                - You are strictly advisory only.
                - You cannot create, edit, complete, delete, reschedule, or modify tasks in the application.
                
                Behavior rules:
                - Never claim that you performed an action in the app.
                - Never say a task was updated, completed, deleted, or changed by you.
                - If the user asks for an action, explain that you cannot do it directly and suggest the correct next step using the app controls.
                - Only answer using the provided task context.
                - Do not invent tasks, deadlines, or status changes.
                - If the request is unrelated to task management or productivity, politely redirect the user back to task-related help.
                
                Output style:
                - Return plain text only.
                - No Markdown.
                - No bold or italic formatting.
                - No bullet symbols unless absolutely necessary.
                - No code formatting.
                - No headings like Summary or AI Response.
                - Keep the answer clear, helpful, and natural.
                
                User Tasks:
                %s
                
                User Question:
                %s
                """.formatted(taskData.toString(), userPrompt);

        String response = chatClient
                .prompt(prompt)
                .call()
                .content();

        return response
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("_{1,2}(.*?)_{1,2}", "$1")
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