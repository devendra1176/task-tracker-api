package com.project.tasktracker.service.impl;

import com.project.tasktracker.dto.TaskRequestDTO;
import com.project.tasktracker.dto.TaskResponseDTO;
import com.project.tasktracker.entity.Task;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import com.project.tasktracker.exception.ResourceNotFoundException;
import com.project.tasktracker.repository.TaskRepository;
import com.project.tasktracker.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    // 🔄 DTO → ENTITY
    private Task mapToEntity(TaskRequestDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDueDate(dto.getDueDate());
        return task;
    }

    // 🔄 ENTITY → DTO
    private TaskResponseDTO mapToDTO(Task task) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setDueDate(task.getDueDate());
        return dto;
    }

    // CREATE TASK
    @Override
    public TaskResponseDTO createTask(TaskRequestDTO dto) {

        Task task = mapToEntity(dto); // DTO → Entity

        Task savedTask = taskRepository.save(task);

        return mapToDTO(savedTask); // Entity → DTO
    }

    // GET ALL TASKS
    @Override
    public List<TaskResponseDTO> getAllTasks() {

        List<Task> tasks = taskRepository.findAll();

        // List<Entity> → List<DTO>
        return tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // GET TASK BY ID
    @Override
    public TaskResponseDTO getTaskById(Long id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        return mapToDTO(task);
    }

    // UPDATE TASK
    @Override
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO dto) {

        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        // Update fields
        existingTask.setTitle(dto.getTitle());
        existingTask.setDescription(dto.getDescription());
        existingTask.setStatus(dto.getStatus());
        existingTask.setPriority(dto.getPriority());
        existingTask.setDueDate(dto.getDueDate());

        Task updatedTask = taskRepository.save(existingTask);

        return mapToDTO(updatedTask);
    }

    // UPDATE STATUS
    @Override
    public TaskResponseDTO updateStatus(Long id, String value) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(TaskStatus.valueOf(value)); //JSON -> ENUM

        Task updated = taskRepository.save(task);

        return mapToDTO(updated);
    }


    // DELETE TASK
    @Override
    public void deleteTask(Long id) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        taskRepository.delete(task);
    }

    //PAGINATION
    @Override
    public Page<TaskResponseDTO> getAllTasksPaginated(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage = taskRepository.findAll(pageable);

        return taskPage.map(this::mapToDTO);
    }

    //FILTERING
    @Override
    public Page<TaskResponseDTO> getFilteredTasks(
            int page,
            int size,
            String sortBy,
            String direction,
            TaskStatus status,
            Priority priority) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage;

        if (status != null && priority != null) {
            taskPage = taskRepository.findByStatusAndPriority(status, priority, pageable);
        } else if (status != null) {
            taskPage = taskRepository.findByStatus(status, pageable);
        } else if (priority != null) {
            taskPage = taskRepository.findByPriority(priority, pageable);
        } else {
            taskPage = taskRepository.findAll(pageable);
        }

        return taskPage.map(this::mapToDTO);
    }

    //SEARCE TASK AND DESCREPTION
    @Override
    public Page<TaskResponseDTO> searchTasks(
            String keyword,
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage = taskRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        keyword,
                        keyword,
                        pageable
                );

        return taskPage.map(this::mapToDTO);
    }

}
