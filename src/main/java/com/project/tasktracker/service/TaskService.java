package com.project.tasktracker.service;

import com.project.tasktracker.dto.TaskRequestDTO;
import com.project.tasktracker.dto.TaskResponseDTO;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TaskService {

    TaskResponseDTO createTask(TaskRequestDTO dto);

    List<TaskResponseDTO> getAllTasks();

    TaskResponseDTO getTaskById(Long id);

    TaskResponseDTO updateTask(Long id, TaskRequestDTO dto);

    TaskResponseDTO updateStatus(Long id, String value);

    void deleteTask(Long id);

    Page<TaskResponseDTO> getAllTasksPaginated(
            int page,
            int size,
            String sortBy,
            String direction
    );

    Page<TaskResponseDTO> getFilteredTasks(
            int page,
            int size,
            String sortBy,
            String direction,
            TaskStatus status,
            Priority priority
    );

    Page<TaskResponseDTO> searchTasks(
            String keyword,
            int page,
            int size,
            String sortBy,
            String direction
    );

}
