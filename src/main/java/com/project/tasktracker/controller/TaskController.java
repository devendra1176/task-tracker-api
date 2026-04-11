package com.project.tasktracker.controller;

import com.project.tasktracker.dto.TaskRequestDTO;
import com.project.tasktracker.dto.TaskResponseDTO;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import com.project.tasktracker.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    //  1. CREATE TASK
    @PostMapping
    public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO dto) {
        return taskService.createTask(dto);
    }

    // 2. GET ALL TASKS
    @GetMapping
    public ResponseEntity<Page<TaskResponseDTO>> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        return ResponseEntity.ok(
                taskService.getAllTasksPaginated(page, size, sortBy, direction)
        );
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
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    // 6. UPDATE TASK (FULL UPDATE)
    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO dto) {
        return taskService.updateTask(id, dto);
    }


    // 7. UPDATE STATUS
    @PatchMapping("/{id}/status")
    public TaskResponseDTO updateStatus(@PathVariable Long id,
                                        @RequestParam String value) {
        return taskService.updateStatus(id, value);
    }

    //  8. DELETE TASK
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok("Task deleted successfully");
    }
    
}
