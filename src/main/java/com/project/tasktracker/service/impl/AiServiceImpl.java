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

import java.time.*;
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

    /**
     * Returns current time in IST (India Standard Time)
     */
    private String getCurrentISTTime() {
        return ZonedDateTime
                .now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a (zzz)"));
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

    private String calculateExactTimeRemaining(Task task) {
        if (task.getDueDate() == null) return "";

        // ✅ CRITICAL: Use IST timezone for BOTH now and deadline
        ZoneId ist = ZoneId.of("Asia/Kolkata");

        LocalTime time = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23, 59);

        LocalDateTime deadline = LocalDateTime.of(task.getDueDate(), time);

        LocalDateTime nowIST = LocalDateTime.now(ist);

        long hoursLeft = ChronoUnit.HOURS.between(nowIST, deadline);
        long minutesLeft = ChronoUnit.MINUTES.between(nowIST, deadline) % 60;
        long totalMinutesLeft = ChronoUnit.MINUTES.between(nowIST, deadline);

        // Task is overdue
        if (totalMinutesLeft < 0) {
            long overdueHours = Math.abs(hoursLeft);
            long overdueMinutes = Math.abs(minutesLeft);
            if (overdueHours > 0) {
                return "OVERDUE by " + overdueHours + "h " + overdueMinutes + "m 🚨";
            }
            return "OVERDUE by " + overdueMinutes + "m 🚨";
        }

        if (hoursLeft == 0 && minutesLeft > 0) {
            return "Due in " + minutesLeft + "m 🚨";
        }

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
                
                ⏰ TIMEZONE CONTEXT (CRITICAL):
                - Current time in India (IST): %s
                - ALL time calculations MUST use IST (GMT+5:30)
                - Use "Exact Time" field from task data — it's pre-calculated in IST
                
                CONTEXT:
                - You can read the user's task data below.
                - You are strictly advisory only — CANNOT modify tasks.
                - Stay grounded only in the given task data.
                
                YOUR JOB:
                1. Acknowledge completed tasks first (celebrate wins ✅)
                2. Highlight what needs attention now (priority + time left)
                3. Suggest 1-2 practical next steps
                4. Keep tone supportive, not alarming
                
                FORMATTING RULES:
                - Use emojis naturally: 📊🎯✅💡🚀📅⏰
                - Plain text only (no markdown, bold, italics)
                - Concise: under 120 words
                - Natural, human tone
                
                ENDING RULE (Strictly Follow):
                - If ALL tasks are DONE → "🎉 All caught up! Great job."
                - Otherwise → Find the single most urgent incomplete task and add:
                  "⏰ Next up: [Task Name] ([Exact Time or Time Left])
                  Need help planning? Just ask! 💬"
                
                User Tasks:
                %s
                """.formatted(getCurrentISTTime(), taskData);
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
                You are a helpful, multilingual AI assistant inside a productivity app.
                
                ⏰ TIMEZONE CONTEXT (CRITICAL):
                - Current time in India (IST): %s
                - ALL time references MUST use IST (GMT+5:30)
                - If task is overdue in IST, say "OVERDUE" not "due soon"
                
                🌐 LANGUAGE RULE (IMPORTANT):
                - Detect user's language from their question
                - ALWAYS respond in the SAME language (Hindi/Hinglish/English)
                - Match user's tone (casual, formal, friendly)
                - Examples:
                  * User: "GST ke bare me bta" → Reply in Hinglish
                  * User: "What should I do first?" → Reply in English
                  * User: "आज क्या प्लान है?" → Reply in Hindi
                
                ✅ CAPABILITIES:
                - Task planning, prioritization, breaking down work
                - Generate practice questions (MCQs/quizzes) based on task topics
                - Answer general knowledge (science, business, studies, etc.)
                - Study techniques, time-management tips, motivation
                - Friendly conversations
                
                ❌ LIMITATIONS:
                - CANNOT create, edit, complete, delete, or modify tasks
                - CANNOT claim you performed actions in the app
                - CANNOT access personal data beyond task list
                - If user asks for app action → guide them to use app controls
                
                BEHAVIOR RULES:
                - Keep responses fresh — avoid repeating phrases
                - If unsure, ask a clarifying question
                - If question unrelated to tasks → still help if safe/useful
                - Be warm, practical, encouraging
                
                FORMATTING:
                - Use emojis naturally: 💬🎯✅💡🚀📅🧠🎉
                - Plain text only (no markdown/bold/italics)
                - Concise: under 200 words
                
                User Tasks Context:
                %s
                
                User Question:
                %s
                """.formatted(getCurrentISTTime(), taskData, userPrompt);

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
                    description,
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

        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1")  // **bold**
                .replaceAll("\\*(.*?)\\*", "$1")                  // *italic*
                .replaceAll("__(.*?)__", "$1")                    // __underline__
                .replaceAll("_(.*?)_", "$1")                      // _italic_
                .replaceAll("`([^`]*)`", "$1");                   // `code`

        // 4. Remove bullet markers but preserve emoji bullets
        cleaned = cleaned.replaceAll("(?m)^\\s*[-•]\\s*(?![🎯✅💡🚀📅⏰📊🎉📌⚠️🧠🔥🚨🧩])", "");

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