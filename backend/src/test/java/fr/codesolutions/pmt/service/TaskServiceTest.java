package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.dto.TaskDtos.TaskCreateRequest;
import fr.codesolutions.pmt.dto.TaskDtos.TaskResponse;
import fr.codesolutions.pmt.dto.TaskDtos.TaskUpdateRequest;
import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.model.Priority;
import fr.codesolutions.pmt.model.Project;
import fr.codesolutions.pmt.model.Role;
import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.TaskStatus;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.ProjectMemberRepository;
import fr.codesolutions.pmt.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Règles métier des tâches, testées sans base de données ni serveur de messagerie.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final long PROJECT_ID = 1;
    private static final long TASK_ID = 10;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private ProjectAccessService accessService;
    @Mock
    private TaskHistoryService historyService;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private TaskService taskService;

    private Project project;
    private User alice;
    private User bob;
    private Task task;

    @BeforeEach
    void setUp() {
        project = TestData.project(PROJECT_ID);
        alice = TestData.user(1, "alice");
        bob = TestData.user(2, "bob");
        task = TestData.task(TASK_ID, project, alice);
        lenient().when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        lenient().when(accessService.requireTaskEditor(PROJECT_ID, alice.getId()))
                .thenReturn(TestData.member(project, alice, Role.ADMIN));
        lenient().when(memberRepository.findByProjectIdAndUserId(PROJECT_ID, bob.getId()))
                .thenReturn(Optional.of(TestData.member(project, bob, Role.MEMBER)));
    }

    private TaskUpdateRequest updateRequest(TaskStatus status, LocalDate endDate, Long assigneeId) {
        return new TaskUpdateRequest(task.getName(), task.getDescription(), task.getDueDate(), endDate,
                task.getPriority(), status, assigneeId);
    }

    @Test
    void laCreationEnregistreLaTacheAuStatutTodo() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(PROJECT_ID, alice.getId(),
                new TaskCreateRequest("  Nouvelle tâche ", "", LocalDate.of(2026, 3, 1), Priority.HIGH, null));

        assertThat(response.name()).isEqualTo("Nouvelle tâche");
        assertThat(response.description()).isNull();
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.createdByName()).isEqualTo("alice");
        verifyNoInteractions(notificationService);
    }

    @Test
    void laCreationAvecAssigneLeNotifie() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(PROJECT_ID, alice.getId(),
                new TaskCreateRequest("Tâche", "Détail", LocalDate.of(2026, 3, 1), Priority.LOW, bob.getId()));

        assertThat(response.assigneeName()).isEqualTo("bob");
        verify(notificationService).notifyTaskAssigned(any(Task.class), eq(bob), eq(alice));
    }

    @Test
    void laCreationEstRefuseeAUnObservateur() {
        when(accessService.requireTaskEditor(PROJECT_ID, 3L))
                .thenThrow(ApiException.forbidden("Un observateur ne peut pas créer ni modifier de tâche"));

        assertThatThrownBy(() -> taskService.create(PROJECT_ID, 3L,
                new TaskCreateRequest("Tâche", null, LocalDate.of(2026, 3, 1), Priority.LOW, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void onNePeutAssignerQuUnMembreDuProjet() {
        when(memberRepository.findByProjectIdAndUserId(PROJECT_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assign(TASK_ID, alice.getId(), 99L))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(notificationService);
    }

    @Test
    void lAssignationNotifieLeNouvelAssigne() {
        when(historyService.recordChange(task, alice, "assignee", null, "bob")).thenReturn(true);
        LocalDateTime before = task.getUpdatedAt();

        TaskResponse response = taskService.assign(TASK_ID, alice.getId(), bob.getId());

        assertThat(response.assigneeId()).isEqualTo(bob.getId());
        assertThat(task.getUpdatedAt()).isAfterOrEqualTo(before);
        verify(notificationService).notifyTaskAssigned(task, bob, alice);
    }

    @Test
    void retirerUnAssigneNeNotifiePersonne() {
        task.setAssignee(bob);
        when(historyService.recordChange(task, alice, "assignee", "bob", null)).thenReturn(true);

        TaskResponse response = taskService.assign(TASK_ID, alice.getId(), null);

        assertThat(response.assigneeId()).isNull();
        verifyNoInteractions(notificationService);
    }

    @Test
    void laMiseAJourTraceChaqueChampEtAppliqueLesValeurs() {
        when(historyService.recordChange(eq(task), eq(alice), any(), any(), any())).thenReturn(false);
        when(historyService.recordChange(task, alice, "status", TaskStatus.TODO, TaskStatus.DONE)).thenReturn(true);
        LocalDate endDate = LocalDate.of(2026, 1, 30);

        TaskResponse response = taskService.update(TASK_ID, alice.getId(),
                updateRequest(TaskStatus.DONE, endDate, null));

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(response.endDate()).isEqualTo(endDate);
        // Un appel par champ suivi : la décision d'écrire ou non revient à TaskHistoryService
        for (String field : List.of("name", "description", "dueDate", "endDate", "priority", "status", "assignee")) {
            verify(historyService).recordChange(eq(task), eq(alice), eq(field), any(), any());
        }
        verifyNoInteractions(notificationService);
    }

    @Test
    void uneMiseAJourSansChangementNeToucheNiLaDateNiLaMessagerie() {
        LocalDateTime before = LocalDateTime.of(2026, 1, 1, 8, 0);
        task.setUpdatedAt(before);
        when(historyService.recordChange(eq(task), eq(alice), any(), any(), any())).thenReturn(false);

        taskService.update(TASK_ID, alice.getId(), updateRequest(TaskStatus.TODO, null, null));

        assertThat(task.getUpdatedAt()).isEqualTo(before);
        verifyNoInteractions(notificationService);
    }

    @Test
    void uneTacheInconnueRenvoie404() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.get(404L, alice.getId()))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void leTableauDeBordInterrogeLeBonFiltre() {
        when(taskRepository.findByProjectIdOrderByDueDateAsc(PROJECT_ID)).thenReturn(List.of(task));
        when(taskRepository.findByProjectIdAndStatusOrderByDueDateAsc(PROJECT_ID, TaskStatus.DONE))
                .thenReturn(List.of());

        assertThat(taskService.list(PROJECT_ID, alice.getId(), null)).hasSize(1);
        assertThat(taskService.list(PROJECT_ID, alice.getId(), TaskStatus.DONE)).isEmpty();
    }
}
