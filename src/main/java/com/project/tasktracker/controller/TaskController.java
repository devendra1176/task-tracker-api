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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    //  1. CREATE TASK
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponseDTO>> createTask(@Valid @RequestBody TaskRequestDTO dto) {

        TaskResponseDTO createdTask = taskService.createTask(dto);

        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(
                true,
                "Task created successfully",
                createdTask,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. GET ALL TASKS
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponseDTO>>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

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

    //3. Get Filtered Task
    @GetMapping("/filter")
    public ResponseEntity<Page<TaskResponseDTO>> getFilteredTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority) {
        log.info("Received request to fetch tasks. page={}, size={}, sortBy={}, direction={}",
                page, size, sortBy, direction);

        return ResponseEntity.ok(
                taskService.getFilteredTasks(page, size, sortBy, direction, status, priority)
        );
    }

    //4. Search Task and description
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponseDTO>> searchTasks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ResponseEntity.ok(
                taskService.searchTasks(keyword, page, size, sortBy, direction)
        );
    }

    // 5. GET TASK BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> getTaskById(@PathVariable Long id) {

        TaskResponseDTO task = taskService.getTaskById(id);

        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(
                true,
                "Task fetched successfully",
                task,
                LocalDateTime.now()
        );
        log.info("Received request to fetch Task  id : {}", id);
        return ResponseEntity.ok(response);
    }

    // 6. UPDATE TASK (FULL UPDATE)
    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO dto) {
        log.info("Received request to update task with id: {}", id);
        return taskService.updateTask(id, dto);
    }


    // 7. UPDATE STATUS
    @PatchMapping("/{id}/status")
    public TaskResponseDTO updateStatus(@PathVariable Long id,
                                        @RequestParam String value) {
        log.info("Received request to update status for task id: {} with value: {}", id, value);
        return taskService.updateStatus(id, value);
    }

    //  8. DELETE TASK

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


}
