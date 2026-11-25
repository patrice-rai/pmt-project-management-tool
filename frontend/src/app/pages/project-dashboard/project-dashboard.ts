import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  PRIORITIES,
  PRIORITY_LABELS,
  ROLES,
  ROLE_LABELS,
  STATUSES,
  STATUS_LABELS,
  errorMessage,
} from '../../core/labels';
import { Member, Priority, Project, Role, Task, TaskStatus } from '../../core/models';
import { ProjectService } from '../../core/project.service';
import { TaskService } from '../../core/task.service';

/**
 * Tableau de bord d'un projet : tâches rangées par statut, membres et leurs rôles.
 *
 * Les actions affichées dépendent du rôle de l'utilisateur sur ce projet :
 * l'administrateur gère les membres, l'administrateur et le membre créent des tâches,
 * l'observateur consulte seulement. Le back refait ces contrôles de son côté.
 */
@Component({
  selector: 'app-project-dashboard',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './project-dashboard.html',
})
export class ProjectDashboard implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly projectService = inject(ProjectService);
  private readonly taskService = inject(TaskService);
  private readonly fb = inject(FormBuilder);

  readonly roles = ROLES;
  readonly statuses = STATUSES;
  readonly priorities = PRIORITIES;
  readonly roleLabels = ROLE_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly priorityLabels = PRIORITY_LABELS;

  readonly projectId = Number(this.route.snapshot.paramMap.get('id'));

  readonly inviteForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['MEMBER' as Role, Validators.required],
  });

  readonly taskForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    dueDate: ['', Validators.required],
    priority: ['MEDIUM' as Priority, Validators.required],
    assigneeId: [''],
  });

  project: Project | null = null;
  members: Member[] = [];
  tasks: Task[] = [];

  /** Statut retenu par le filtre ; chaîne vide pour tout afficher. */
  statusFilter: TaskStatus | '' = '';

  loading = true;
  loadError = '';
  memberError = '';
  taskError = '';

  ngOnInit(): void {
    forkJoin({
      project: this.projectService.get(this.projectId),
      members: this.projectService.members(this.projectId),
      tasks: this.taskService.list(this.projectId),
    }).subscribe({
      next: ({ project, members, tasks }) => {
        this.project = project;
        this.members = members;
        this.tasks = tasks;
        this.loading = false;
      },
      error: (err) => {
        this.loadError = errorMessage(err);
        this.loading = false;
      },
    });
  }

  /** Seul l'administrateur invite des membres et change les rôles. */
  get isAdmin(): boolean {
    return this.project?.role === 'ADMIN';
  }

  /** L'administrateur et le membre peuvent créer des tâches ; l'observateur non. */
  get canEdit(): boolean {
    return this.project?.role === 'ADMIN' || this.project?.role === 'MEMBER';
  }

  /** Colonnes affichées : toutes, ou seulement celle du statut filtré. */
  get visibleStatuses(): TaskStatus[] {
    return this.statusFilter ? [this.statusFilter] : this.statuses;
  }

  tasksByStatus(status: TaskStatus): Task[] {
    return this.tasks.filter((task) => task.status === status);
  }

  /** Applique le filtre de statut en redemandant les tâches à l'API. */
  filterByStatus(value: string): void {
    this.statusFilter = value as TaskStatus | '';
    this.taskService.list(this.projectId, this.statusFilter || undefined).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.taskError = '';
      },
      error: (err) => (this.taskError = errorMessage(err)),
    });
  }

  /** Invite un utilisateur existant par son adresse e-mail, avec le rôle choisi. */
  invite(): void {
    if (this.inviteForm.invalid) {
      this.inviteForm.markAllAsTouched();
      return;
    }
    this.memberError = '';
    this.projectService.invite(this.projectId, this.inviteForm.getRawValue()).subscribe({
      next: (member) => {
        this.members = [...this.members, member];
        this.inviteForm.reset();
      },
      error: (err) => (this.memberError = errorMessage(err)),
    });
  }

  /** Change le rôle d'un membre. En cas de refus du back, l'ancien rôle reste affiché. */
  changeRole(member: Member, value: string): void {
    this.memberError = '';
    this.projectService.changeRole(this.projectId, member.userId, value as Role).subscribe({
      next: (updated) => {
        this.members = this.members.map((m) => (m.userId === updated.userId ? updated : m));
      },
      error: (err) => {
        this.memberError = errorMessage(err);
        // Recrée la liste pour que la liste déroulante revienne sur le rôle réel.
        this.members = this.members.map((m) => ({ ...m }));
      },
    });
  }

  /** Crée une tâche ; elle apparaît dans la colonne de son statut initial. */
  createTask(): void {
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
      return;
    }
    const { name, description, dueDate, priority, assigneeId } = this.taskForm.getRawValue();
    this.taskError = '';
    this.taskService
      .create(this.projectId, {
        name,
        description: description.trim() || null,
        dueDate,
        priority,
        assigneeId: assigneeId ? Number(assigneeId) : null,
      })
      .subscribe({
        next: (task) => {
          // Une tâche hors du filtre courant n'a pas à s'afficher.
          if (!this.statusFilter || this.statusFilter === task.status) {
            this.tasks = [...this.tasks, task];
          }
          this.taskForm.reset();
        },
        error: (err) => (this.taskError = errorMessage(err)),
      });
  }
}
