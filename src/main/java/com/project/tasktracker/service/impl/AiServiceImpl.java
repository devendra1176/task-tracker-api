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
    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    // Response length limits (characters, ~4-5 chars per word)
    private static final int MAX_SUMMARY_LENGTH = 3200;   // ~800 words
    private static final int MAX_ASK_LENGTH = 4000;        // ~1000 words

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

    /**
     * Calculates human-friendly time remaining (for display)
     */
    private String toHumanReadableTime(Task task) {
        if (task.getDueDate() == null) return "No specific deadline.";

        ZoneId ist = ZoneId.of("Asia/Kolkata");
        LocalTime time = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23, 59);
        LocalDateTime deadline = LocalDateTime.of(task.getDueDate(), time);
        LocalDateTime now = LocalDateTime.now(ist);

        Duration diff = Duration.between(now, deadline);

        if (diff.isNegative()) {
            long days = Math.abs(diff.toDays());
            if (days == 0) return "Overdue since today.";
            return "Overdue by " + days + " day(s).";
        }

        long hours = diff.toHours();

        if (hours < 1) return "due in less than an hour.";
        if (hours < 24) return "due later today.";
        if (hours < 48) return "due tomorrow.";

        long days = diff.toDays();
        if (days <= 3) return "due in " + days + " days.";
        return "due in about " + (days / 7 + 1) + " week(s).";
    }

    /**
     * Calculates exact time remaining (for precise display)
     */
    private String calculateExactTimeRemaining(Task task) {
        if (task.getDueDate() == null) return "";

        ZoneId ist = ZoneId.of("Asia/Kolkata");
        LocalTime time = task.getDueTime() != null ? task.getDueTime() : LocalTime.of(23, 59);
        LocalDateTime deadline = LocalDateTime.of(task.getDueDate(), time);
        LocalDateTime nowIST = LocalDateTime.now(ist);

        long hoursLeft = ChronoUnit.HOURS.between(nowIST, deadline);
        long minutesLeft = ChronoUnit.MINUTES.between(nowIST, deadline) % 60;
        long totalMinutesLeft = ChronoUnit.MINUTES.between(nowIST, deadline);

        if (totalMinutesLeft < 0) {
            long overdueHours = Math.abs(hoursLeft);
            long overdueMinutes = Math.abs(minutesLeft);
            return overdueHours > 0
                    ? "OVERDUE by " + overdueHours + "hours" + overdueMinutes + "minutes"
                    : "OVERDUE by " + overdueMinutes + "minutes";
        }

        if (hoursLeft == 0 && minutesLeft > 0) return "Due in " + minutesLeft + "minutes";
        if (hoursLeft < 3) return "Due in " + hoursLeft + "hours" + minutesLeft + "minutes";
        if (hoursLeft < 24) return "Due in " + hoursLeft + "hours";
        if (hoursLeft < 48) return "Due in ~" + hoursLeft + "hours";

        return "";
    }

    /**
     * Suggests Productivity Techniques based on Task Description & Urgency
     */
    private String suggestProductivityTechnique(Task task, Duration timeLeft) {
        String desc = task.getDescription() != null ? task.getDescription().toLowerCase() : "";

        if (desc.contains("study") || desc.contains("learn") || desc.contains("read")) {
            return "Try the Feynman Technique: Learn it by explaining it simply.";
        }
        if (desc.contains("report") || desc.contains("write") || desc.contains("essay")) {
            return "Use Time Blocking: Set aside a dedicated slot for deep work.";
        }
        if (task.getPriority() != null && task.getPriority().name().equals("HIGH")
                && !timeLeft.isNegative() && timeLeft.toHours() < 4) {
            return "Apply 'Eat That Frog': Tackle the hardest part first, right now.";
        }
        if (task.getPriority() != null && task.getPriority().name().equals("LOW")
                && !timeLeft.isNegative() && timeLeft.toHours() < 1) {
            return "Use the 2-Minute Rule: If it takes less than 2 mins, do it NOW.";
        }
        return "Break this task into smaller, manageable steps.";
    }

    /**
     * Suggests Next Logical Step based on Description
     */
    private String suggestNextStep(String description) {
        if (description == null) return "Review your progress.";
        String desc = description.toLowerCase();

        if (desc.contains("study") || desc.contains("learn"))
            return "Take a practice quiz or teach someone else to solidify it.";
        if (desc.contains("meeting") || desc.contains("call"))
            return "Send a follow-up email or meeting notes.";
        if (desc.contains("write") || desc.contains("report"))
            return "Proofread your draft or share it for feedback.";
        if (desc.contains("bug") || desc.contains("fix"))
            return "Test the fix thoroughly in a staging environment.";

        return "What is the smallest version of this you can finish in 15 minutes?";
    }

    @Override
    public String generateTaskSummary() {
        User currentUser = getCurrentUser();
        List<Task> tasks = getUserTasks(currentUser);

        if (tasks.isEmpty()) {
            return "📭 You do not have any tasks yet. Start by creating your first task! 🚀";
        }

        String taskData = buildTaskContext(tasks);

        // Prompt bound: ~600 words for AI generation
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
                3. Suggest 1-2 practical next steps using the "Recommended Technique" field
                4. Keep tone supportive, not alarming
                
                FORMATTING RULES:
                - Use emojis naturally: 📊🎯✅💡🚀📅⏰
                - Plain text only (no markdown, bold, italics)
                - Concise: under 600 words for generation
                - Natural, human tone
                
                ENDING RULE (Strictly Follow):
                - If ALL tasks are DONE → "🎉 All caught up! Great job."
                - Otherwise → Find the single most urgent incomplete task and add:
                  "⏰ Next up: [Task Name] ([Exact Time or Time Left])
                  Need help planning? Just ask! 💬"
                - ALWAYS end with a short, relevant motivational or work-related quote 
                  that fits the user's current task situation.
                
                - User Tasks:
                  %s
                """.formatted(getCurrentISTTime(), taskData);

        String response = chatClient.prompt(prompt).call().content();

        if (response == null) {
            log.warn("AI returned null response for summary");
            return "🤔 I couldn't generate a summary right now. Please try again!";
        }

        return cleanAiText(response, MAX_SUMMARY_LENGTH);
    }

    @Override
    public String askAiAboutTasks(String userPrompt) {
        User currentUser = getCurrentUser();
        List<Task> tasks = getUserTasks(currentUser);
        String taskData = tasks.isEmpty() ? "No tasks yet." : buildTaskContext(tasks);

        // Prompt bound: ~800 words for AI generation
        String prompt = """
                You are a helpful, multilingual AI assistant inside a productivity app.
                
                ⏰ TIMEZONE CONTEXT (CRITICAL):
                - Current time in India (IST): %s
                - ALL time references MUST use IST (GMT+5:30)
                - If task is overdue in IST, say "OVERDUE" not "due soon"
                
                🌐 LANGUAGE RULE (IMPORTANT):
                - Detect user's language from their question (If you can't detect language prefer English as default language)
                - ALWAYS respond in the SAME language (Hindi/Hinglish/English)
                - Match user's tone (casual, formal, friendly)
                
                ✅ CAPABILITIES:
                - Task planning, prioritization, breaking down work
                - Generate practice questions based on task topics
                - Answer general knowledge, study techniques, time-management tips
                - Friendly conversations with contextual advice
                
                ❌ LIMITATIONS:
                - CANNOT create, edit, complete, delete, or modify tasks
                - CANNOT claim you performed actions in the app
                - If user asks for app action → guide them to use app controls
                
                BEHAVIOR RULES:
                - Keep responses fresh — avoid repeating phrases
                - If unsure, ask a clarifying question
                - Be warm, practical, encouraging
                
                FORMATTING:
                - Use emojis naturally: 💬🎯✅💡🚀📅🧠🎉⭐️⏳
                - Plain text only (no markdown/bold/italics)
                - Concise: under 800 words for generation
                
                User Tasks Context:
                %s
                
                User Question:
                %s
                """.formatted(getCurrentISTTime(), taskData, userPrompt);

        String response = chatClient.prompt(prompt).call().content();

        if (response == null) {
            log.warn("AI returned null response for question: {}", userPrompt);
            return "🤔 I couldn't process that right now. Please try again!";
        }

        return cleanAiText(response, MAX_ASK_LENGTH);
    }

    private List<Task> getUserTasks(User currentUser) {
        return taskRepository
                .findByUser(currentUser, PageRequest.of(0, AI_TASK_LIMIT))
                .getContent();
    }

    private String buildTaskContext(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (Task t : tasks) {
            String description = safeValue(t.getDescription());
            if (description.length() > 450) {
                description = description.substring(0, 450) + "...";
            }

            // ✅ FIX: Actually call the helper methods here
            String humanTime = (t.getDueDate() != null) ? toHumanReadableTime(t) : "No deadline";

            Duration timeLeft = Duration.ZERO;
            if (t.getDueDate() != null) {
                ZoneId ist = ZoneId.of("Asia/Kolkata");
                LocalTime time = t.getDueTime() != null ? t.getDueTime() : LocalTime.of(23, 59);
                LocalDateTime deadline = LocalDateTime.of(t.getDueDate(), time);
                timeLeft = Duration.between(LocalDateTime.now(ist), deadline);
            }
            String technique = suggestProductivityTechnique(t, timeLeft);
            String nextStep = suggestNextStep(t.getDescription());

            sb.append(String.format("""
                            [Task] %s
                            Description: %s
                            Status: %s | Priority: %s
                            Due Date: %s
                            Exact Time Remaining: %s
                            Human Time Estimate: %s
                            Recommended Technique: %s
                            Probable Next Step: %s
                            -------------------
                            """,
                    t.getTitle(),
                    description,
                    t.getStatus() != null ? t.getStatus() : "Not set",
                    t.getPriority() != null ? t.getPriority() : "Not set",
                    t.getDueDate() != null ? t.getDueDate().format(fmt) : "Not set",
                    t.getDueDate() != null ? calculateExactTimeRemaining(t) : "No deadline",
                    humanTime,
                    technique,
                    nextStep
            ));
        }
        return sb.toString();
    }

    /**
     * Clean AI response text with configurable max length
     */
    private String cleanAiText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "🤔 I could not generate a response right now. Please try again.";
        }

        String cleaned = text;

        // 1. Remove thinking/reasoning blocks
        cleaned = cleaned.replaceAll("(?is)<(think|thinking|reason|thought|reasoning|internal)\\b[^>]*>.*?</\\1>", "");
        cleaned = cleaned.replaceAll("(?i)</?(think|thinking|reason|thought|reasoning|internal)\\b[^>]*>", "");

        // 2. Remove markdown formatting
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("__(.*?)__", "$1")
                .replaceAll("_(.*?)_", "$1")
                .replaceAll("`([^`]*)`", "$1");

        // 3. Remove bullet markers but preserve emoji bullets
        cleaned = cleaned.replaceAll("(?m)^\\s*[-•]\\s*(?![🎯✅💡🚀📅⏰📊🎉📌⚠️🧠🔥🧩⭐])", "");

        // 4. Remove numbered list markers
        cleaned = cleaned.replaceAll("(?m)^\\s*\\d+\\.\\s*", "");

        // 5. Clean up excessive newlines
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        // 6. Apply length limit
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength - 3) + "...";
        }

        // 7. Final trim + fallback
        cleaned = cleaned.trim();
        if (cleaned.isBlank()) {
            return "✨ Here's a quick thought: Focus on one small step forward. You've got this! 💪";
        }

        return cleaned;
    }

    // Overload for backward compatibility
    private String cleanAiText(String text) {
        return cleanAiText(text, MAX_SUMMARY_LENGTH);
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