import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { InviteRequest, Member, Project, ProjectRequest, Role } from './models';

/** Appels de l'API pour les projets et leurs membres. */
@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);

  /** Projets dont l'utilisateur connecté est membre, avec son rôle sur chacun. */
  list(): Observable<Project[]> {
    return this.http.get<Project[]>('/api/projects');
  }

  /** Crée un projet ; son créateur en devient l'administrateur. */
  create(request: ProjectRequest): Observable<Project> {
    return this.http.post<Project>('/api/projects', request);
  }

  get(projectId: number): Observable<Project> {
    return this.http.get<Project>(`/api/projects/${projectId}`);
  }

  members(projectId: number): Observable<Member[]> {
    return this.http.get<Member[]>(`/api/projects/${projectId}/members`);
  }

  /** Invite un utilisateur existant par son e-mail (administrateur uniquement). */
  invite(projectId: number, request: InviteRequest): Observable<Member> {
    return this.http.post<Member>(`/api/projects/${projectId}/members`, request);
  }

  /** Change le rôle d'un membre (administrateur uniquement). */
  changeRole(projectId: number, userId: number, role: Role): Observable<Member> {
    return this.http.patch<Member>(`/api/projects/${projectId}/members/${userId}`, { role });
  }
}
