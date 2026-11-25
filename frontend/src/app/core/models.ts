/** Modèles échangés avec l'API PMT. Ils reprennent exactement les réponses JSON du back. */

/** Rôle d'un utilisateur sur un projet donné. */
export type Role = 'ADMIN' | 'MEMBER' | 'OBSERVER';

/** Priorité d'une tâche. */
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

/** Statut d'une tâche, qui détermine sa colonne sur le tableau de bord. */
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

/** Utilisateur connecté, tel que renvoyé par l'inscription et la connexion. */
export interface User {
  id: number;
  username: string;
  email: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Projet vu par l'utilisateur connecté : `role` est son propre rôle sur ce projet. */
export interface Project {
  id: number;
  name: string;
  description: string | null;
  startDate: string;
  role: Role;
}

export interface ProjectRequest {
  name: string;
  description: string | null;
  startDate: string;
}

export interface Member {
  userId: number;
  username: string;
  email: string;
  role: Role;
}

export interface InviteRequest {
  email: string;
  role: Role;
}

export interface Task {
  id: number;
  projectId: number;
  name: string;
  description: string | null;
  dueDate: string | null;
  endDate: string | null;
  priority: Priority;
  status: TaskStatus;
  assigneeId: number | null;
  assigneeName: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface TaskCreateRequest {
  name: string;
  description: string | null;
  dueDate: string | null;
  priority: Priority;
  assigneeId: number | null;
}

export interface TaskUpdateRequest {
  name: string;
  description: string | null;
  dueDate: string | null;
  endDate: string | null;
  priority: Priority;
  status: TaskStatus;
  assigneeId: number | null;
}

/** Une ligne de l'historique : un champ modifié, par qui et quand. */
export interface TaskHistoryEntry {
  id: number;
  fieldName: string;
  oldValue: string | null;
  newValue: string | null;
  changedById: number;
  changedByName: string;
  changedAt: string;
}

/** Corps d'erreur renvoyé par l'API. `details` associe un champ à son message de validation. */
export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  details: Record<string, string>;
}
