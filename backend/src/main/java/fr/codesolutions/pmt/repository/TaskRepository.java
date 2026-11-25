package fr.codesolutions.pmt.repository;

import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Accès aux tâches. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderByDueDateAsc(Long projectId);

    List<Task> findByProjectIdAndStatusOrderByDueDateAsc(Long projectId, TaskStatus status);
}
