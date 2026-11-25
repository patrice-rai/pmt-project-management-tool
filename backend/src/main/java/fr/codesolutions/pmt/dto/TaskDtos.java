package fr.codesolutions.pmt.dto;

import fr.codesolutions.pmt.model.Priority;
import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.TaskHistory;
import fr.codesolutions.pmt.model.TaskStatus;
import fr.codesolutions.pmt.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Objets échangés pour les tâches et leur historique.
 */
public final class TaskDtos {

    private TaskDtos() {
    }

    @Schema(description = "Données de création d'une tâche")
    public record TaskCreateRequest(
            @Schema(example = "Maquetter la page d'accueil")
            @NotBlank(message = "Le nom de la tâche est obligatoire")
            @Size(max = 150, message = "Le nom de la tâche ne doit pas dépasser 150 caractères")
            String name,

            @Schema(example = "Maquette haute fidélité, versions bureau et mobile.")
            @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
            String description,

            @Schema(example = "2026-02-05")
            @NotNull(message = "La date d'échéance est obligatoire")
            LocalDate dueDate,

            @Schema(example = "MEDIUM")
            @NotNull(message = "La priorité est obligatoire")
            Priority priority,

            @Schema(description = "Identifiant du membre assigné (facultatif)", example = "2", nullable = true)
            Long assigneeId) {
    }

    @Schema(description = "Mise à jour complète d'une tâche")
    public record TaskUpdateRequest(
            @Schema(example = "Maquetter la page d'accueil")
            @NotBlank(message = "Le nom de la tâche est obligatoire")
            @Size(max = 150, message = "Le nom de la tâche ne doit pas dépasser 150 caractères")
            String name,

            @Schema(example = "Maquette validée par le client.")
            @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
            String description,

            @Schema(example = "2026-02-05")
            @NotNull(message = "La date d'échéance est obligatoire")
            LocalDate dueDate,

            @Schema(description = "Date de fin réelle (facultative)", example = "2026-02-03", nullable = true)
            LocalDate endDate,

            @Schema(example = "HIGH")
            @NotNull(message = "La priorité est obligatoire")
            Priority priority,

            @Schema(example = "DONE")
            @NotNull(message = "Le statut est obligatoire")
            TaskStatus status,

            @Schema(description = "Identifiant du membre assigné, null pour désassigner", example = "2", nullable = true)
            Long assigneeId) {
    }

    @Schema(description = "Assignation d'une tâche")
    public record AssignRequest(
            @Schema(description = "Identifiant du membre assigné, null pour désassigner", example = "2", nullable = true)
            Long assigneeId) {
    }

    @Schema(description = "Tâche avec toutes ses informations")
    public record TaskResponse(
            @Schema(example = "2") Long id,
            @Schema(example = "1") Long projectId,
            @Schema(example = "Maquetter la page d'accueil") String name,
            @Schema(example = "Maquette haute fidélité, versions bureau et mobile.") String description,
            @Schema(example = "2026-02-05") LocalDate dueDate,
            @Schema(example = "2026-02-03", nullable = true) LocalDate endDate,
            @Schema(example = "MEDIUM") Priority priority,
            @Schema(example = "IN_PROGRESS") TaskStatus status,
            @Schema(example = "2", nullable = true) Long assigneeId,
            @Schema(example = "bob", nullable = true) String assigneeName,
            @Schema(example = "1") Long createdById,
            @Schema(example = "alice") String createdByName,
            @Schema(example = "2026-01-07T09:00:00") LocalDateTime createdAt,
            @Schema(example = "2026-01-21T09:30:00") LocalDateTime updatedAt) {

        public static TaskResponse from(Task task) {
            User assignee = task.getAssignee();
            return new TaskResponse(task.getId(), task.getProject().getId(), task.getName(), task.getDescription(),
                    task.getDueDate(), task.getEndDate(), task.getPriority(), task.getStatus(),
                    assignee == null ? null : assignee.getId(),
                    assignee == null ? null : assignee.getUsername(),
                    task.getCreatedBy().getId(), task.getCreatedBy().getUsername(),
                    task.getCreatedAt(), task.getUpdatedAt());
        }
    }

    @Schema(description = "Modification d'un champ d'une tâche")
    public record HistoryResponse(
            @Schema(example = "5") Long id,
            @Schema(description = "Champ modifié", example = "status") String fieldName,
            @Schema(example = "TODO", nullable = true) String oldValue,
            @Schema(example = "IN_PROGRESS", nullable = true) String newValue,
            @Schema(example = "2") Long changedById,
            @Schema(example = "bob") String changedByName,
            @Schema(example = "2026-01-21T09:30:00") LocalDateTime changedAt) {

        public static HistoryResponse from(TaskHistory entry) {
            return new HistoryResponse(entry.getId(), entry.getFieldName(), entry.getOldValue(), entry.getNewValue(),
                    entry.getChangedBy().getId(), entry.getChangedBy().getUsername(), entry.getChangedAt());
        }
    }
}
