package com.project.tasktracker.service.impl;

import com.project.tasktracker.entity.Task;
import com.project.tasktracker.entity.User;
import com.project.tasktracker.exception.ResourceNotFoundException;
import com.project.tasktracker.repository.TaskRepository;
import com.project.tasktracker.repository.UserRepository;
import com.project.tasktracker.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {

    private static final int AI_TASK_LIMIT = 20;
    private static final int MAX_RESPONSE_LENGTH = 500; // Safety limit for response length
    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private final ChatClient chatClient;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public AiServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }


    private String calculateTimeRemaining(LocalDate dueDate) {
        if (dueDate == null) return "No due date";

        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, dueDate);

        if (days < 0) return "⚠️ Overdue by " + Math.abs(days) + " day" + (Math.abs(days) > 1 ? "s" : "");
        if (days == 0) return "🔥 Due TODAY";
        if (days == 1) return "⚠️ Due tomorrow plan tonight!";
        if (days <= 3) return "🚨 Due in " + days + " days";
        if (days <= 7) return "Due in " + days + " days";

        long weeks = days / 7;
        if (weeks <= 4) return "Due in ~" + weeks + " week" + (weeks > 1 ? "s" : "");

        long months = days / 30;
        return "Due in ~" + months + " month" + (months > 1 ? "s" : "");
    }

    /**
     * Calculate exact hours/minutes remaining for urgent tasks
     */
    /**
     * Calculate exact hours/minutes remaining using BOTH dueDate AND dueTime
     */
    private String calculateExactTimeRemaining(Task task) {
        if (task.getDueDate() == null) return "";

        // Combine dueDate + dueTime into exact deadline
        LocalTime time = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23, 59);
        LocalDateTime deadline = LocalDateTime.of(task.getDueDate(), time);
        LocalDateTime now = LocalDateTime.now();

        long hoursLeft = ChronoUnit.HOURS.between(now, deadline);
        long minutesLeft = ChronoUnit.MINUTES.between(now, deadline) % 60;
        long totalMinutesLeft = ChronoUnit.MINUTES.between(now, deadline);

        // Task is overdue
        if (totalMinutesLeft < 0) {
            long overdueHours = Math.abs(hoursLeft);
            long overdueMinutes = Math.abs(minutesLeft);
            if (overdueHours > 0) {
                return "OVERDUE by " + overdueHours + "h " + overdueMinutes + "m 🚨";
            }
            return "OVERDUE by " + overdueMinutes + "m 🚨";
        }

        // Due very soon (< 1 hour)
        if (hoursLeft == 0 && minutesLeft > 0) {
            return "Due in " + minutesLeft + "m 🚨";
        }

        // Due within 3 hours - URGENT
        if (hoursLeft < 3) {
            return "Due in " + hoursLeft + "h " + minutesLeft + "m 🔥";
        }

        // Due today
        if (hoursLeft < 24) {
            return "Due in " + hoursLeft + "h ⏰";
        }

        // Due tomorrow
        if (hoursLeft < 48) {
            return "Due in ~" + hoursLeft + "h";
        }

        return ""; // Not urgent enough for exact time
    }

    @Override
    public String generateTaskSummary() {
        User currentUser = getCurrentUser();
        List<Task> tasks = getUserTasks(currentUser);

        if (tasks.isEmpty()) {
            return "📭 You do not have any tasks yet. Start by creating your first task! 🚀";
        }

        String taskData = buildTaskContext(tasks);

        String prompt = """
                You are an AI productivity assistant inside a task tracker application.
                
                CONTEXT:
                - You can read the user's task data below.
                - You are strictly advisory only.
                - You CANNOT create, update, complete, delete, reschedule, or modify any task.
                - Use "Exact Time" field for urgent tasks (due today/tomorrow).
                
                YOUR JOB:
                - Analyze the user's tasks and give a short productivity summary.
                - Point out what looks most important or urgent.
                - Suggest the most sensible next step.
                - Stay grounded only in the given task data.
                
                FORMATTING:
                - Use emojis to make text engaging: 📊🎯✅️💡🚀📅⏰
                - Return plain text only (no markdown, bold, italics, headings).
                - Keep response concise (under 150 words).
                - Write naturally like a helpful assistant.
                
                ENDING RULE (Strictly Follow):
                - Identify the single most urgent incomplete task (based on Priority + Time Left).
                - Add a final line at the very end of your response in this exact format:
                "⏰Next up: [Task Name] ([Exact Time or Time Left])"
                - If "Exact Time" field has a value (e.g., "Due in 5h 23m"), USE IT.
                - If "Exact Time" is empty, fall back to "Time Left" field.
                - After that line, add a friendly closing: "Need help planning? Just ask! 💬"
                - EXCEPTION: If ALL tasks are marked 'DONE', skip the urgency line and just say: "🎉 All caught up! Great job."
                User Tasks:
                %s
                """.formatted(taskData);
        String response = chatClient.prompt(prompt).call().content();

        // Safety: Handle null response from AI
        if (response == null) {
            log.warn("AI returned null response for summary");
            return "🤔 I couldn't generate a summary right now. Please try again!";
        }

        log.debug("Raw AI summary (first 200 chars): {}",
                response.substring(0, Math.min(200, response.length())));

        return cleanAiText(response);
    }

    @Override
    public String askAiAboutTasks(String userPrompt) {
        User currentUser = getCurrentUser();
        List<Task> tasks = getUserTasks(currentUser);
        String taskData = tasks.isEmpty() ? "No tasks yet." : buildTaskContext(tasks);

        String prompt = """
                You are a helpful AI assistant inside a productivity app.
                
                CAPABILITIES:
                ✅ You CAN:
                - Help with task-related questions (planning, prioritization, breaking down work)
                - Generate practice questions (MCQs, quick quizzes) based on task topics
                - Answer general knowledge questions (science, business, studies, current affairs)
                - Offer study techniques, time-management tips, or motivation
                - Have friendly, supportive conversations
                
                ❌ You CANNOT:
                - Create, edit, complete, delete, or modify any task in the app
                - Claim that you performed any action in the application
                - Access user's personal data beyond their task list
                - Generate harmful, illegal, or inappropriate content
                
                BEHAVIOR RULES:
                - If user asks for an app action, gently guide them to use app controls.
                - If question is unrelated to tasks, still help if it's safe and useful.
                - Keep responses fresh — avoid repeating the same phrases.
                - If unsure, ask a clarifying question instead of guessing.
                
                LANGUAGE RULE (IMPORTANT):
                - Detect the user's language from their question (Hindi, Hinglish, English, etc.)
                - ALWAYS respond in the SAME language the user used
                - If user writes in Hinglish (Hindi+English mix), reply in natural Hinglish
                - If user writes in Hindi, reply in Hindi
                - If user writes in English, reply in English
                - Match the user's tone (casual, formal, friendly)
                
                TONE:
                - Be warm, encouraging, and practical.
                - Use emojis naturally to add warmth: 💬🎯✅💡🚀📅🧠🎉
                - Keep responses concise (under 250 words) but helpful.
                
                User Tasks Context:
                %s
                
                User Question:
                %s
                """.formatted(taskData, userPrompt);

        String response = chatClient.prompt(prompt).call().content();

        // Safety: Handle null response from AI
        if (response == null) {
            log.warn("AI returned null response for question: {}", userPrompt);
            return "🤔 I couldn't process that right now. Please try again!";
        }

        return cleanAiText(response);
    }

    private List<Task> getUserTasks(User currentUser) {
        return taskRepository
                .findByUser(currentUser, PageRequest.of(0, AI_TASK_LIMIT))
                .getContent();
    }

    private String buildTaskContext(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");

        for (Task t : tasks) {
            String dueDateStr = (t.getDueDate() != null)
                    ? t.getDueDate().format(fmt)
                    : "Not set";

            String dueTimeStr = (t.getDueTime() != null)
                    ? " at " + t.getDueTime().format(timeFmt)
                    : "";

            String timeRemaining = (t.getDueDate() != null)
                    ? calculateTimeRemaining(t.getDueDate())
                    : "No deadline";

            String exactTime = (t.getDueDate() != null)
                    ? calculateExactTimeRemaining(t)
                    : "";


            String description = safeValue(t.getDescription());
            // Truncate if too long (AI context limit)
            if (description.length() > 200) {
                description = description.substring(0, 197) + "...";
            }

            sb.append(String.format("""
                            [Task] %s
                            Description: %s
                            Status: %s | Priority: %s
                            Due Date: %s%s
                            Time Left: %s
                            Exact Time: %s
                            -------------------
                            """,
                    t.getTitle(),
                    description,  // ✅ Yeh add kiya
                    t.getStatus() != null ? t.getStatus() : "Not set",
                    t.getPriority() != null ? t.getPriority() : "Not set",
                    dueDateStr,
                    dueTimeStr,
                    timeRemaining,
                    exactTime
            ));
        }
        return sb.toString();
    }

    private String cleanAiText(String text) {
        if (text == null || text.isBlank()) {
            return "🤔 I could not generate a response right now. Please try again.";
        }

        String cleaned = text;

        // 1. Remove thinking/reasoning blocks (multiline, case-insensitive)
        // Handles: <think>, <thinking>, <reason>, etc.
        cleaned = cleaned.replaceAll("(?is)<(think|thinking|reason|thought|reasoning|internal)\\b[^>]*>.*?</\\1>", "");

        // 2. Fallback: Remove orphaned opening/closing tags
        cleaned = cleaned.replaceAll("(?i)</?(think|thinking|reason|thought|reasoning|internal)\\b[^>]*>", "");

        // 3. Remove markdown formatting but keep content
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1")  // **bold**
                .replaceAll("\\*(.*?)\\*", "$1")                  // *italic*
                .replaceAll("__(.*?)__", "$1")                    // __underline__
                .replaceAll("_(.*?)_", "$1")                      // _italic_
                .replaceAll("`([^`]*)`", "$1");                   // `code`

        // 4. Remove bullet markers but preserve emoji bullets
        cleaned = cleaned.replaceAll("(?m)^\\s*[-•]\\s*(?![🎯✅💡🚀📅⏰📊🎉📌⚠️🧠🔥🚨🧩⏱️])", "");

        // 5. Remove numbered list markers (1. 2. 3.)
        cleaned = cleaned.replaceAll("(?m)^\\s*\\d+\\.\\s*", "");

        // 6. Clean up excessive newlines (max 2 consecutive)
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // 7. Apply length limit for safety
        if (cleaned.length() > MAX_RESPONSE_LENGTH) {
            cleaned = cleaned.substring(0, MAX_RESPONSE_LENGTH - 3) + "...";
        }

        // 8. Final trim + fallback if empty
        cleaned = cleaned.trim();
        if (cleaned.isBlank()) {
            return "✨ Here's a quick thought: Focus on one small step forward. You've got this! 💪";
        }

        return cleaned;
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