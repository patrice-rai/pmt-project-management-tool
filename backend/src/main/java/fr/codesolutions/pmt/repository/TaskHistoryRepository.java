package fr.codesolutions.pmt.repository;

import fr.codesolutions.pmt.model.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Accès à l'historique des modifications des tâches. */
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    /** Historique d'une tâche, de la modification la plus récente à la plus ancienne. */
    List<TaskHistory> findByTaskIdOrderByChangedAtDescIdDesc(Long taskId);
}
