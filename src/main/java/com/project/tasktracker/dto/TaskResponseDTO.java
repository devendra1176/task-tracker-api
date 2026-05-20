package com.project.tasktracker.dto;

import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private String dueDateTimeDisplay;
    private boolean isOverdue;
}
