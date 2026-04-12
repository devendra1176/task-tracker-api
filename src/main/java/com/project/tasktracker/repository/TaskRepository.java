package com.project.tasktracker.repository;

import com.project.tasktracker.entity.Task;
import com.project.tasktracker.entity.User;
import com.project.tasktracker.enums.Priority;
import com.project.tasktracker.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // BASIC OWNERSHIP METHODS
    List<Task> findByUser(User user);

    Page<Task> findByUser(User user, Pageable pageable);

    Optional<Task> findByIdAndUser(Long id, User user);

    // USER-BASED FILTERING METHODS
    Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);

    Page<Task> findByUserAndPriority(User user, Priority priority, Pageable pageable);

    Page<Task> findByUserAndStatusAndPriority(
            User user,
            TaskStatus status,
            Priority priority,
            Pageable pageable
    );

    // USER-BASED SEARCH METHOD
    Page<Task> findByUserAndTitleContainingIgnoreCaseOrUserAndDescriptionContainingIgnoreCase(
            User user,
            String title,
            User userAgain,
            String description,
            Pageable pageable
    );
}