import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  PRIORITIES,
  PRIORITY_LABELS,
  STATUSES,
  STATUS_LABELS,
  errorMessage,
  fieldLabel,
  historyValue,
} from '../../core/labels';
import {
  Member,
  Priority,
  Project,
  Task,
  TaskHistoryEntry,
  TaskStatus,
} from '../../core/models';
import { ProjectService } from '../../core/project.service';
import { TaskService } from '../../core/task.service';

/**
 * Détail d'une tâche : toutes ses informations, sa mise à jour, son assignation
 * et l'historique de ses modifications.
 *
 * L'observateur voit tout en lecture seule ; l'administrateur et le membre peuvent modifier.
 */
@Component({
  selector: 'app-task-detail',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './task-detail.html',
})
export class TaskDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly projectService = inject(ProjectService);
  private readonly taskService = inject(TaskService);

  readonly statuses = STATUSES;
  readonly priorities = PRIORITIES;
  readonly statusLabels = STATUS_LABELS;
  readonly priorityLabels = PRIORITY_LABELS;
  readonly fieldLabel = fieldLabel;
  readonly historyValue = historyValue;

  readonly projectId = Number(this.route.snapshot.paramMap.get('id'));
  readonly taskId = Number(this.route.snapshot.paramMap.get('taskId'));

  readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    dueDate: [''],
    endDate: [''],
    priority: ['MEDIUM' as Priority, Validators.required],
    status: ['TODO' as TaskStatus, Validators.required],
  });

  project: Project | null = null;
  members: Member[] = [];
  task: Task | null = null;
  history: TaskHistoryEntry[] = [];

  loading = true;
  loadError = '';
  error = '';
  success = '';

  ngOnInit(): void {
    forkJoin({
      project: this.projectService.get(this.projectId),
      members: this.projectService.members(this.projectId),
      task: this.taskService.get(this.taskId),
      history: this.taskService.history(this.taskId),
    }).subscribe({
      next: ({ project, members, task, history }) => {
        this.project = project;
        this.members = members;
        this.history = history;
        this.showTask(task);
        this.loading = false;
      },
      error: (err) => {
        this.loadError = errorMessage(err);
        this.loading = false;
      },
    });
  }

  /** L'administrateur et le membre peuvent modifier et assigner ; l'observateur non. */
  get canEdit(): boolean {
    return this.project?.role === 'ADMIN' || this.project?.role === 'MEMBER';
  }

  /** Enregistre les informations de la tâche puis recharge l'historique. */
  save(): void {
    if (this.form.invalid || !this.task) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, description, dueDate, endDate, priority, status } = this.form.getRawValue();
    this.resetMessages();
    this.taskService
      .update(this.taskId, {
        name,
        description: description.trim() || null,
        dueDate: dueDate || null,
        endDate: endDate || null,
        priority,
        status,
        // L'assignation a son propre formulaire : on renvoie la valeur actuelle.
        assigneeId: this.task.assigneeId,
      })
      .subscribe({
        next: (task) => this.afterChange(task, 'La tâche a été mise à jour.'),
        error: (err) => (this.error = errorMessage(err)),
      });
  }

  /** Assigne la tâche au membre choisi (ou la désassigne). Le back prévient l'assigné par e-mail. */
  assign(value: string): void {
    const assigneeId = value ? Number(value) : null;
    this.resetMessages();
    this.taskService.assign(this.taskId, assigneeId).subscribe({
      next: (task) =>
        this.afterChange(
          task,
          assigneeId === null
            ? "La tâche n'est plus assignée."
            : `Tâche assignée à ${task.assigneeName}. Un e-mail lui a été envoyé.`,
        ),
      error: (err) => (this.error = errorMessage(err)),
    });
  }

  /** Affiche la tâche et recopie ses valeurs dans le formulaire. */
  private showTask(task: Task): void {
    this.task = task;
    this.form.setValue({
      name: task.name,
      description: task.description ?? '',
      dueDate: task.dueDate ?? '',
      endDate: task.endDate ?? '',
      priority: task.priority,
      status: task.status,
    });
  }

  /** Après une modification réussie : met l'écran à jour et relit l'historique. */
  private afterChange(task: Task, message: string): void {
    this.showTask(task);
    this.success = message;
    this.taskService.history(this.taskId).subscribe({
      next: (history) => (this.history = history),
      error: (err) => (this.error = errorMessage(err)),
    });
  }

  private resetMessages(): void {
    this.error = '';
    this.success = '';
  }
}
