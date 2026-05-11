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

    private static final int AI_TASK_LIMIT = 20;

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
        List<Task> tasks = getUserTasks(currentUser);

        if (tasks.isEmpty()) {
            return "You do not have any tasks yet. Start by creating your first task.";
        }

        String taskData = buildTaskContext(tasks);

        String prompt = """
                You are an AI productivity assistant inside a task tracker application.
                
                Context:
                - You can read the user's task data.
                - You are strictly advisory only.
                - You cannot create, update, complete, delete, reschedule, or modify any task.
                
                Your job:
                - Analyze the user's tasks and give a short productivity summary.
                - Point out what looks most important or urgent.
                - Suggest the most sensible next step.
                - Stay grounded only in the given task data.
                - Do not invent tasks, deadlines, or changes.
                - Do not claim that you performed any action in the application.
                
                Output rules:
                - Return plain text only.
                - Do not use Markdown.
                - Do not use bold, italics, headings, bullets, numbered lists, stars, underscores, or code formatting.
                - Do not write labels like Overall Summary, Most Urgent Tasks, or Motivational Tip.
                - Write naturally like a helpful assistant inside a productivity app.
                - Keep the response concise, clear, and readable.
                
                User Tasks:
                %s
                """.formatted(taskData);

        String response = chatClient
                .prompt(prompt)
                .call()
                .content();

        return cleanAiText(response);
    }

    @Override
    public String askAiAboutTasks(String userPrompt) {
        User currentUser = getCurrentUser();
        List<Task> tasks = getUserTasks(currentUser);

        if (tasks.isEmpty()) {
            return "You do not have any tasks yet. Please create some tasks first, then I can help you analyze them.";
        }

        String taskData = buildTaskContext(tasks);

        String prompt = """
                You are an AI productivity assistant inside a task tracker application.
                
                Context:
                - You can read the user's task data.
                - You can help the user understand priorities, deadlines, workload, and next steps.
                - You are strictly advisory only.
                - You cannot create, edit, complete, delete, reschedule, or modify tasks in the application.
                
                Behavior rules:
                - Never claim that you performed an action in the app.
                - Never say that a task was updated, completed, deleted, or changed by you.
                - If the user asks for an action, clearly explain that you cannot do it directly and suggest the correct next step using the app controls.
                - Only answer using the provided task context.
                - Do not invent tasks, deadlines, or status changes.
                - If the request is unrelated to task management or productivity, politely guide the user back to task-related help.
                
                Output rules:
                - Return plain text only.
                - Do not use Markdown.
                - Do not use bold, italics, headings, numbered lists, stars, underscores, or code formatting.
                - Avoid unnecessary bullet formatting.
                - Do not write headings like Summary or AI Response.
                - Keep the answer clear, helpful, natural, and concise.
                
                User Tasks:
                %s
                
                User Question:
                %s
                """.formatted(taskData, userPrompt);

        String response = chatClient
                .prompt(prompt)
                .call()
                .content();

        return cleanAiText(response);
    }

    private List<Task> getUserTasks(User currentUser) {
        return taskRepository
                .findByUser(currentUser, PageRequest.of(0, AI_TASK_LIMIT))
                .getContent();
    }

    private String buildTaskContext(List<Task> tasks) {
        StringBuilder taskData = new StringBuilder();

        for (Task task : tasks) {
            taskData.append("Title: ")
                    .append(safeValue(task.getTitle()))
                    .append("\n");

            taskData.append("Description: ")
                    .append(safeValue(task.getDescription()))
                    .append("\n");

            taskData.append("Status: ")
                    .append(task.getStatus() != null ? task.getStatus() : "Not set")
                    .append("\n");

            taskData.append("Priority: ")
                    .append(task.getPriority() != null ? task.getPriority() : "Not set")
                    .append("\n");

            taskData.append("Due Date: ")
                    .append(task.getDueDate() != null ? task.getDueDate() : "Not set")
                    .append("\n\n");
        }

        return taskData.toString().trim();
    }

    private String cleanAiText(String text) {
        if (text == null || text.isBlank()) {
            return "I could not generate a response right now. Please try again.";
        }

        return text
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("__(.*?)__", "$1")
                .replaceAll("_(.*?)_", "$1")
                .replaceAll("`([^`]*)`", "$1")
                .replaceAll("(?m)^\\s*[-•]\\s*", "")
                .replaceAll("(?m)^\\s*\\d+\\.\\s*", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
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