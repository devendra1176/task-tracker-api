package com.project.tasktracker.dto;

import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 to 100 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @FutureOrPresent(message = "Due date must be today or future")
    private LocalDate dueDate;
}
