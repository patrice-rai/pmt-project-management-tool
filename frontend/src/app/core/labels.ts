import { HttpErrorResponse } from '@angular/common/http';
import { Priority, Role, TaskStatus } from './models';

/** Libellés français affichés à l'écran pour les valeurs techniques de l'API. */
export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrateur',
  MEMBER: 'Membre',
  OBSERVER: 'Observateur',
};

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  DONE: 'Terminée',
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
};

/** Ordre d'affichage dans les listes déroulantes et les colonnes du tableau de bord. */
export const ROLES: Role[] = ['ADMIN', 'MEMBER', 'OBSERVER'];
export const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
export const PRIORITIES: Priority[] = ['LOW', 'MEDIUM', 'HIGH'];

/** Nom lisible des champs d'une tâche, pour l'historique des modifications. */
const FIELD_LABELS: Record<string, string> = {
  name: 'Nom',
  description: 'Description',
  dueDate: "Date d'échéance",
  endDate: 'Date de fin',
  priority: 'Priorité',
  status: 'Statut',
  assignee: 'Assigné à',
};

/** Traduit le nom technique d'un champ ; renvoie le nom brut s'il est inconnu. */
export function fieldLabel(fieldName: string): string {
  return FIELD_LABELS[fieldName] ?? fieldName;
}

/**
 * Traduit une valeur d'historique (statut ou priorité) en libellé français.
 * Les autres valeurs sont affichées telles quelles, et l'absence de valeur par un tiret.
 */
export function historyValue(fieldName: string, value: string | null): string {
  if (value === null || value === '') {
    return '—';
  }
  if (fieldName === 'status' && value in STATUS_LABELS) {
    return STATUS_LABELS[value as TaskStatus];
  }
  if (fieldName === 'priority' && value in PRIORITY_LABELS) {
    return PRIORITY_LABELS[value as Priority];
  }
  return value;
}

/**
 * Extrait le message à montrer à l'utilisateur depuis une erreur HTTP.
 * Le message du back est prioritaire ; sinon on distingue la panne réseau du cas général.
 */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as { message?: string } | null;
    if (body && typeof body === 'object' && body.message) {
      return body.message;
    }
    if (err.status === 0) {
      return 'Le serveur est injoignable. Vérifiez votre connexion.';
    }
  }
  return 'Une erreur inattendue est survenue.';
}
