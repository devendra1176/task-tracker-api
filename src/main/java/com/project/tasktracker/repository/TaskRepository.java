package com.project.tasktracker.repository;

import com.project.tasktracker.entity.Task;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // FILTERING METHODS
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByPriority(Priority priority, Pageable pageable);

    Page<Task> findByStatusAndPriority(TaskStatus status, Priority priority, Pageable pageable);

    // SEARCH METHOD
    Page<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );
}