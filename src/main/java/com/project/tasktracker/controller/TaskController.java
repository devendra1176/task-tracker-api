package com.project.tasktracker.controller;

import com.project.tasktracker.dto.ApiResponse;
import com.project.tasktracker.dto.PagedResponse;
import com.project.tasktracker.dto.TaskRequestDTO;
import com.project.tasktracker.dto.TaskResponseDTO;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import com.project.tasktracker.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // CREATE TASK
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDTO>> createTask(@Valid @RequestBody TaskRequestDTO dto) {

        log.info("Received request to create task with title: {}", dto.getTitle());

        TaskResponseDTO createdTask = taskService.createTask(dto);

        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(
                true,
                "Task created successfully",
                createdTask,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET ALL TASKS WITH PAGINATION
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponseDTO>>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDateTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("Received request to fetch tasks. page={}, size={}, sortBy={}, direction={}",
                page, size, sortBy, direction);

        Page<TaskResponseDTO> taskPage = taskService.getAllTasksPaginated(page, size, sortBy, direction);

        PagedResponse<TaskResponseDTO> pagedData = new PagedResponse<>(
                taskPage.getContent(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isLast()
        );

        ApiResponse<PagedResponse<TaskResponseDTO>> response = new ApiResponse<>(
                true,
                "Tasks fetched successfully",
                pagedData,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // GET TASK BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> getTaskById(@PathVariable Long id) {

        log.info("Received request to fetch task id: {}", id);

        TaskResponseDTO task = taskService.getTaskById(id);

        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(
                true,
                "Task fetched successfully",
                task,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // UPDATE TASK
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> updateTask(@PathVariable Long id,
                                                                   @Valid @RequestBody TaskRequestDTO dto) {

        log.info("Received request to update task with id: {}", id);

        TaskResponseDTO updatedTask = taskService.updateTask(id, dto);

        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(
                true,
                "Task updated successfully",
                updatedTask,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // UPDATE TASK STATUS
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> updateStatus(@PathVariable Long id,
                                                                     @RequestParam String value) {

        log.info("Received request to update status for task id: {} with value: {}", id, value);

        TaskResponseDTO updatedTask = taskService.updateStatus(id, value);

        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(
                true,
                "Task status updated successfully",
                updatedTask,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // DELETE TASK
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {

        log.info("Received request to delete task with id: {}", id);

        taskService.deleteTask(id);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Task deleted successfully",
                null,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // FILTER TASKS
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponseDTO>>> getFilteredTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDateTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority) {

        log.info("Received request to filter tasks. page={}, size={}, sortBy={}, direction={}, status={}, priority={}",
                page, size, sortBy, direction, status, priority);

        Page<TaskResponseDTO> taskPage = taskService.getFilteredTasks(page, size, sortBy, direction, status, priority);

        PagedResponse<TaskResponseDTO> pagedData = new PagedResponse<>(
                taskPage.getContent(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isLast()
        );

        ApiResponse<PagedResponse<TaskResponseDTO>> response = new ApiResponse<>(
                true,
                "Filtered tasks fetched successfully",
                pagedData,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    // SEARCH TASKS
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponseDTO>>> searchTasks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDateTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        log.info("Received request to search tasks. keyword={}, page={}, size={}, sortBy={}, direction={}",
                keyword, page, size, sortBy, direction);

        Page<TaskResponseDTO> taskPage = taskService.searchTasks(keyword, page, size, sortBy, direction);

        PagedResponse<TaskResponseDTO> pagedData = new PagedResponse<>(
                taskPage.getContent(),
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages(),
                taskPage.isLast()
        );

        ApiResponse<PagedResponse<TaskResponseDTO>> response = new ApiResponse<>(
                true,
                "Search results fetched successfully",
                pagedData,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }
}