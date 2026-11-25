package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.TaskHistory;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.TaskHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Historique des modifications des tâches.
 * Toute écriture dans l'historique passe par {@link #recordChange}.
 */
@Service
public class TaskHistoryService {

    private final TaskHistoryRepository historyRepository;

    public TaskHistoryService(TaskHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Enregistre la modification d'un champ, uniquement si sa valeur a réellement changé.
     *
     * @param task      tâche modifiée
     * @param author    utilisateur à l'origine de la modification
     * @param fieldName nom du champ (name, description, dueDate, endDate, priority, status, assignee)
     * @param oldValue  valeur avant modification (peut être null)
     * @param newValue  valeur après modification (peut être null)
     * @return true si une ligne d'historique a été créée, false si la valeur est inchangée
     */
    @Transactional
    public boolean recordChange(Task task, User author, String fieldName, Object oldValue, Object newValue) {
        String before = asText(oldValue);
        String after = asText(newValue);
        if (Objects.equals(before, after)) {
            return false;
        }
        historyRepository.save(new TaskHistory(task, author, fieldName, before, after));
        return true;
    }

    /** Historique d'une tâche, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<TaskHistory> historyOf(Long taskId) {
        return historyRepository.findByTaskIdOrderByChangedAtDescIdDesc(taskId);
    }

    private static String asText(Object value) {
        return value == null ? null : value.toString();
    }
}
