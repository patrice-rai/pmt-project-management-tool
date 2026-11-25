package fr.codesolutions.pmt.controller;

import fr.codesolutions.pmt.dto.ErrorResponse;
import fr.codesolutions.pmt.dto.ProjectDtos.MemberRequest;
import fr.codesolutions.pmt.dto.ProjectDtos.MemberResponse;
import fr.codesolutions.pmt.dto.ProjectDtos.ProjectRequest;
import fr.codesolutions.pmt.dto.ProjectDtos.ProjectResponse;
import fr.codesolutions.pmt.dto.ProjectDtos.RoleRequest;
import fr.codesolutions.pmt.dto.TaskDtos.TaskCreateRequest;
import fr.codesolutions.pmt.dto.TaskDtos.TaskResponse;
import fr.codesolutions.pmt.model.TaskStatus;
import fr.codesolutions.pmt.service.ProjectService;
import fr.codesolutions.pmt.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Projets, membres, rôles et tâches d'un projet (user stories 3 à 6 et 10).
 */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projets", description = "Projets, membres, rôles et tableau de bord")
@ApiResponse(responseCode = "401", description = "En-tête X-User-Id absent ou utilisateur inconnu",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class ProjectController {

    /** En-tête qui identifie l'utilisateur connecté. */
    public static final String USER_HEADER = "X-User-Id";
    static final String USER_HEADER_DOC = "Identifiant de l'utilisateur connecté";

    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un projet",
            description = "Crée un projet ; l'utilisateur connecté en devient l'administrateur.")
    @ApiResponse(responseCode = "201", description = "Projet créé")
    @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectResponse create(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Valid @RequestBody ProjectRequest request) {
        return projectService.create(userId, request);
    }

    @GetMapping
    @Operation(summary = "Lister mes projets",
            description = "Renvoie les projets dont l'utilisateur connecté est membre, avec son rôle sur chacun.")
    @ApiResponse(responseCode = "200", description = "Liste des projets")
    public List<ProjectResponse> list(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId) {
        return projectService.listForUser(userId);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Consulter un projet")
    @ApiResponse(responseCode = "200", description = "Projet trouvé")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Projet introuvable",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectResponse get(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant du projet", example = "1") @PathVariable Long projectId) {
        return projectService.get(projectId, userId);
    }

    @GetMapping("/{projectId}/members")
    @Operation(summary = "Lister les membres d'un projet")
    @ApiResponse(responseCode = "200", description = "Liste des membres et de leur rôle")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<MemberResponse> listMembers(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant du projet", example = "1") @PathVariable Long projectId) {
        return projectService.listMembers(projectId, userId);
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inviter un membre",
            description = "Ajoute au projet l'utilisateur qui possède cette adresse e-mail, avec le rôle indiqué. "
                    + "Réservé aux administrateurs du projet.")
    @ApiResponse(responseCode = "201", description = "Membre ajouté")
    @ApiResponse(responseCode = "403", description = "L'utilisateur connecté n'est pas administrateur du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Projet introuvable ou aucun compte avec cette adresse",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Utilisateur déjà membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MemberResponse addMember(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant du projet", example = "1") @PathVariable Long projectId,
            @Valid @RequestBody MemberRequest request) {
        return projectService.addMember(projectId, userId, request);
    }

    @PatchMapping("/{projectId}/members/{memberId}")
    @Operation(summary = "Attribuer un rôle à un membre",
            description = "Change le rôle d'un membre (ADMIN, MEMBER ou OBSERVER). Réservé aux administrateurs. "
                    + "Le dernier administrateur d'un projet ne peut pas être rétrogradé.")
    @ApiResponse(responseCode = "200", description = "Rôle modifié")
    @ApiResponse(responseCode = "403", description = "L'utilisateur connecté n'est pas administrateur du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Projet introuvable ou utilisateur non membre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Le projet n'aurait plus d'administrateur",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MemberResponse changeRole(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant du projet", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Identifiant de l'utilisateur dont on change le rôle", example = "2")
            @PathVariable Long memberId,
            @Valid @RequestBody RoleRequest request) {
        return projectService.changeRole(projectId, userId, memberId, request.role());
    }

    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "Tableau de bord : tâches d'un projet",
            description = "Renvoie les tâches du projet triées par échéance. Le paramètre status permet de "
                    + "n'obtenir qu'une colonne du tableau de bord. Accessible à tous les rôles.")
    @ApiResponse(responseCode = "200", description = "Liste des tâches")
    @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<TaskResponse> listTasks(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant du projet", example = "1") @PathVariable Long projectId,
            @Parameter(description = "Filtre facultatif sur le statut", example = "IN_PROGRESS")
            @RequestParam(required = false) TaskStatus status) {
        return taskService.list(projectId, userId, status);
    }

    @PostMapping("/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une tâche",
            description = "Crée une tâche au statut TODO. Réservé aux administrateurs et aux membres. "
                    + "Si assigneeId est renseigné, le membre assigné reçoit un e-mail.")
    @ApiResponse(responseCode = "201", description = "Tâche créée")
    @ApiResponse(responseCode = "400", description = "Données invalides ou assigné non membre du projet",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rôle insuffisant (observateur) ou non membre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public TaskResponse createTask(
            @Parameter(description = USER_HEADER_DOC, example = "1") @RequestHeader(USER_HEADER) Long userId,
            @Parameter(description = "Identifiant du projet", example = "1") @PathVariable Long projectId,
            @Valid @RequestBody TaskCreateRequest request) {
        return taskService.create(projectId, userId, request);
    }
}
