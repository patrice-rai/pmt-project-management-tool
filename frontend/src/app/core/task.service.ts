import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Task,
  TaskCreateRequest,
  TaskHistoryEntry,
  TaskStatus,
  TaskUpdateRequest,
} from './models';

/** Appels de l'API pour les tâches et leur historique. */
@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);

  /** Tâches d'un projet, éventuellement limitées à un statut. */
  list(projectId: number, status?: TaskStatus): Observable<Task[]> {
    const params = status ? new HttpParams().set('status', status) : undefined;
    return this.http.get<Task[]>(`/api/projects/${projectId}/tasks`, { params });
  }

  create(projectId: number, request: TaskCreateRequest): Observable<Task> {
    return this.http.post<Task>(`/api/projects/${projectId}/tasks`, request);
  }

  get(taskId: number): Observable<Task> {
    return this.http.get<Task>(`/api/tasks/${taskId}`);
  }

  /** Remplace toutes les informations modifiables d'une tâche. */
  update(taskId: number, request: TaskUpdateRequest): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${taskId}`, request);
  }

  /** Assigne la tâche à un membre, ou la désassigne avec `null`. L'assigné reçoit un e-mail. */
  assign(taskId: number, assigneeId: number | null): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${taskId}/assignee`, { assigneeId });
  }

  /** Historique des modifications, de la plus récente à la plus ancienne. */
  history(taskId: number): Observable<TaskHistoryEntry[]> {
    return this.http.get<TaskHistoryEntry[]>(`/api/tasks/${taskId}/history`);
  }
}
