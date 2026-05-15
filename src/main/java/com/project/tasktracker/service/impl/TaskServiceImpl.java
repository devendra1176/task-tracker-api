package com.project.tasktracker.service.impl;

import com.project.tasktracker.dto.TaskRequestDTO;
import com.project.tasktracker.dto.TaskResponseDTO;
import com.project.tasktracker.entity.Task;
import com.project.tasktracker.entity.User;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import com.project.tasktracker.exception.ResourceNotFoundException;
import com.project.tasktracker.repository.TaskRepository;
import com.project.tasktracker.repository.UserRepository;
import com.project.tasktracker.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    // DTO → ENTITY
    private Task mapToEntity(TaskRequestDTO dto) {
        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDueDate(dto.getDueDate());
        task.setDueTime(dto.getDueTime());
        return task;
    }

    // ENTITY → DTO
    private TaskResponseDTO mapToDTO(Task task) {

        TaskResponseDTO dto = new TaskResponseDTO();

        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setDueDate(task.getDueDate());
        dto.setDueTime(task.getDueTime());
        return dto;
    }

    @Override
    public TaskResponseDTO createTask(TaskRequestDTO dto) {

        User currentUser = getCurrentUser();
        log.info("Creating task for user: {}", currentUser.getEmail());

        Task task = mapToEntity(dto);

        task.setUser(currentUser);

        Task savedTask = taskRepository.save(task);

        log.info("Task created successfully with id: {} for user: {}", savedTask.getId(), currentUser.getEmail());

        return mapToDTO(savedTask);
    }


    // GET ALL TASKS
    @Override
    public List<TaskResponseDTO> getAllTasks() {

        User currentUser = getCurrentUser();

        List<Task> tasks = taskRepository.findByUser(currentUser);

        return tasks.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // GET TASK BY ID
    @Override
    public TaskResponseDTO getTaskById(Long id) {

        User currentUser = getCurrentUser();
        log.info("Fetching task id: {} for user : {}", id, currentUser.getEmail());

        Task task = taskRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        return mapToDTO(task);
    }

    // UPDATE TASK
    @Override
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO dto) {

        User currentUser = getCurrentUser();
        log.info("Updating task id: {} for user: {}", id, currentUser.getEmail());

        Task existingTask = taskRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        existingTask.setTitle(dto.getTitle());
        existingTask.setDescription(dto.getDescription());
        existingTask.setStatus(dto.getStatus());
        existingTask.setPriority(dto.getPriority());
        existingTask.setDueDate(dto.getDueDate());
        existingTask.setDueTime(dto.getDueTime());
        Task updatedTask = taskRepository.save(existingTask);

        log.info("Task updated successfully. id: {}, user: {}", updatedTask.getId(), currentUser.getEmail());

        return mapToDTO(updatedTask);
    }

    // UPDATE STATUS
    @Override
    public TaskResponseDTO updateStatus(Long id, String value) {

        User currentUser = getCurrentUser();
        log.info("Updating Status with id: {} for user: {}", id, currentUser.getEmail());

        Task task = taskRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(TaskStatus.valueOf(value));

        Task updated = taskRepository.save(task);
        log.info("Status updated successfully. id= {}, user= {}", id, currentUser.getEmail());

        return mapToDTO(updated);
    }


    // DELETE TASK
    @Override
    public void deleteTask(Long id) {

        User currentUser = getCurrentUser();
        log.info("Deleting task id: {} for user: {}", id, currentUser.getEmail());
        Task task = taskRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        taskRepository.delete(task);
        log.info("Task deleted successfully. id={}, user={}", id, currentUser.getEmail());
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

        User currentUser = getCurrentUser();

        Page<Task> taskPage = taskRepository.findByUser(currentUser, pageable);

        log.info("Fetching paginated tasks for user: {}. page={}, size={}, sortBy={}, direction={}",
                currentUser.getEmail(), page, size, sortBy, direction);

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

        User currentUser = getCurrentUser();

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage;

        if (status != null && priority != null) {
            taskPage = taskRepository.findByUserAndStatusAndPriority(currentUser, status, priority, pageable);
        } else if (status != null) {
            taskPage = taskRepository.findByUserAndStatus(currentUser, status, pageable);
        } else if (priority != null) {
            taskPage = taskRepository.findByUserAndPriority(currentUser, priority, pageable);
        } else {
            taskPage = taskRepository.findByUser(currentUser, pageable);
        }

        log.info("Filtering tasks for user: {}. status={}, priority={}, page={}, size={}",
                currentUser.getEmail(), status, priority, page, size);

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

        User currentUser = getCurrentUser();

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Task> taskPage = taskRepository
                .findByUserAndTitleContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
                        currentUser,
                        keyword,
                        currentUser,
                        keyword,
                        pageable
                );

        log.info("Searching tasks for user: {} with keyword: {}", currentUser.getEmail(), keyword);

        return taskPage.map(this::mapToDTO);
    }

    private User getCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assert authentication != null;
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }


}
