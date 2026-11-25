package fr.codesolutions.pmt.controller;

import fr.codesolutions.pmt.dto.ErrorResponse;
import fr.codesolutions.pmt.dto.TaskDtos.AssignRequest;
import fr.codesolutions.pmt.dto.TaskDtos.HistoryResponse;
import fr.codesolutions.pmt.dto.TaskDtos.TaskResponse;
import fr.codesolutions.pmt.dto.TaskDtos.TaskUpdateRequest;
import fr.codesolutions.pmt.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static fr.codesolutions.pmt.controller.ProjectController.USER_HEADER;
import static fr.codesolutions.pmt.controller.ProjectController.USER_HEADER_DOC;

/**
 * Consultation, mise à jour, assignation et historique d'une tâche (user stories 7 à 9, 11 et 12).
 */
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tâches", description = "Détail, mise à jour, assignation et historique")
@ApiResponse(responseCode = "401", description = "En-tête X-User-Id absent ou utilisateur inconnu",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "404", description = "Tâche introuvable",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Consulter une tâche",
            description = "Renvoie toutes les informations d'une tâche. Accessible à tous les rôles du projet.")
    @ApiResponse(responseCode = "200", description = "Tâche trouvée")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse get(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant de la tâche", example = "2") @PathVariable Long taskId) {
        return taskService.get(taskId, userId);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "Mettre à jour une tâche",
            description = "Remplace les informations de la tâche (dont la date de fin, le statut et l'assigné). "
                    + "Réservé aux administrateurs et aux membres. Chaque champ modifié est ajouté à l'historique.")
    @ApiResponse(responseCode = "200", description = "Tâche mise à jour")
    @ApiResponse(responseCode = "400", description = "Données invalides ou assigné non membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant (observateur) ou non membre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse update(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant de la tâche", example = "2") @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.update(taskId, userId, request);
    }

    @PatchMapping("/{taskId}/assignee")
    @Operation(summary = "Assigner une tâche",
            description = "Assigne la tâche à un membre du projet, qui reçoit un e-mail de notification. "
                    + "Un assigneeId null retire l'assignation. Réservé aux administrateurs et aux membres.")
    @ApiResponse(responseCode = "200", description = "Tâche assignée")
    @ApiResponse(responseCode = "400", description = "L'utilisateur visé n'est pas membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant (observateur) ou non membre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse assign(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant de la tâche", example = "3") @PathVariable Long taskId,
            @RequestBody AssignRequest request) {
        return taskService.assign(taskId, userId, request.assigneeId());
    }

    @GetMapping("/{taskId}/history")
    @Operation(summary = "Historique d'une tâche",
            description = "Renvoie les modifications de la tâche, de la plus récente à la plus ancienne. "
                    + "Accessible à tous les rôles du projet.")
    @ApiResponse(responseCode = "200", description = "Historique de la tâche")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<HistoryResponse> history(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant de la tâche", example = "1") @PathVariable Long taskId) {
        return taskService.history(taskId, userId);
    }
}
