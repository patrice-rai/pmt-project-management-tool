package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.dto.TaskDtos.HistoryResponse;
import fr.codesolutions.pmt.dto.TaskDtos.TaskCreateRequest;
import fr.codesolutions.pmt.dto.TaskDtos.TaskResponse;
import fr.codesolutions.pmt.dto.TaskDtos.TaskUpdateRequest;
import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.model.ProjectMember;
import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.TaskStatus;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.ProjectMemberRepository;
import fr.codesolutions.pmt.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestion des tâches : création, assignation, mise à jour, consultation et historique.
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService accessService;
    private final TaskHistoryService historyService;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, ProjectMemberRepository memberRepository,
                       ProjectAccessService accessService, TaskHistoryService historyService,
                       NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.historyService = historyService;
        this.notificationService = notificationService;
    }

    /**
     * Crée une tâche dans un projet (administrateur ou membre).
     * Si un assigné est fourni, il est notifié par e-mail.
     */
    @Transactional
    public TaskResponse create(Long projectId, Long userId, TaskCreateRequest request) {
        ProjectMember author = accessService.requireTaskEditor(projectId, userId);
        Task task = new Task(author.getProject(), request.name().trim(), blankToNull(request.description()),
                request.dueDate(), request.priority(), author.getUser());
        User assignee = resolveAssignee(projectId, request.assigneeId());
        task.setAssignee(assignee);
        task = taskRepository.save(task);
        if (assignee != null) {
            notificationService.notifyTaskAssigned(task, assignee, author.getUser());
        }
        return TaskResponse.from(task);
    }

    /**
     * Tâches d'un projet pour le tableau de bord, éventuellement filtrées par statut.
     *
     * @param status statut voulu, ou null pour toutes les tâches
     */
    public List<TaskResponse> list(Long projectId, Long userId, TaskStatus status) {
        accessService.requireMember(projectId, userId);
        List<Task> tasks = status == null
                ? taskRepository.findByProjectIdOrderByDueDateAsc(projectId)
                : taskRepository.findByProjectIdAndStatusOrderByDueDateAsc(projectId, status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    /** Détail d'une tâche, visible par tous les membres du projet. */
    public TaskResponse get(Long taskId, Long userId) {
        Task task = requireTask(taskId);
        accessService.requireMember(task.getProject().getId(), userId);
        return TaskResponse.from(task);
    }

    /**
     * Met à jour une tâche (administrateur ou membre). Chaque champ réellement modifié
     * produit une ligne d'historique ; un nouvel assigné est notifié par e-mail.
     */
    @Transactional
    public TaskResponse update(Long taskId, Long userId, TaskUpdateRequest request) {
        Task task = requireTask(taskId);
        Long projectId = task.getProject().getId();
        User author = accessService.requireTaskEditor(projectId, userId).getUser();

        String name = request.name().trim();
        String description = blankToNull(request.description());
        boolean changed = historyService.recordChange(task, author, "name", task.getName(), name);
        changed |= historyService.recordChange(task, author, "description", task.getDescription(), description);
        changed |= historyService.recordChange(task, author, "dueDate", task.getDueDate(), request.dueDate());
        changed |= historyService.recordChange(task, author, "endDate", task.getEndDate(), request.endDate());
        changed |= historyService.recordChange(task, author, "priority", task.getPriority(), request.priority());
        changed |= historyService.recordChange(task, author, "status", task.getStatus(), request.status());

        task.setName(name);
        task.setDescription(description);
        task.setDueDate(request.dueDate());
        task.setEndDate(request.endDate());
        task.setPriority(request.priority());
        task.setStatus(request.status());
        changed |= changeAssignee(task, author, request.assigneeId());

        if (changed) {
            task.setUpdatedAt(LocalDateTime.now());
        }
        return TaskResponse.from(task);
    }

    /**
     * Assigne une tâche à un membre du projet, ou la désassigne (administrateur ou membre).
     *
     * @param assigneeId identifiant du membre, ou null pour retirer l'assignation
     */
    @Transactional
    public TaskResponse assign(Long taskId, Long userId, Long assigneeId) {
        Task task = requireTask(taskId);
        User author = accessService.requireTaskEditor(task.getProject().getId(), userId).getUser();
        if (changeAssignee(task, author, assigneeId)) {
            task.setUpdatedAt(LocalDateTime.now());
        }
        return TaskResponse.from(task);
    }

    /** Historique des modifications d'une tâche, visible par tous les membres du projet. */
    public List<HistoryResponse> history(Long taskId, Long userId) {
        Task task = requireTask(taskId);
        accessService.requireMember(task.getProject().getId(), userId);
        return historyService.historyOf(taskId).stream().map(HistoryResponse::from).toList();
    }

    /**
     * Applique un changement d'assigné : historique puis notification du nouvel assigné.
     *
     * @return true si l'assigné a changé
     */
    private boolean changeAssignee(Task task, User author, Long assigneeId) {
        User newAssignee = resolveAssignee(task.getProject().getId(), assigneeId);
        boolean changed = historyService.recordChange(task, author, "assignee",
                usernameOf(task.getAssignee()), usernameOf(newAssignee));
        task.setAssignee(newAssignee);
        if (changed && newAssignee != null) {
            notificationService.notifyTaskAssigned(task, newAssignee, author);
        }
        return changed;
    }

    /**
     * Une tâche ne peut être assignée qu'à un membre du projet, quel que soit son rôle.
     *
     * @throws ApiException 400 si l'utilisateur visé n'est pas membre du projet
     */
    private User resolveAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return memberRepository.findByProjectIdAndUserId(projectId, assigneeId)
                .map(ProjectMember::getUser)
                .orElseThrow(() -> ApiException.badRequest("La tâche ne peut être assignée qu'à un membre du projet"));
    }

    private Task requireTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Tâche introuvable"));
    }

    private static String usernameOf(User user) {
        return user == null ? null : user.getUsername();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
