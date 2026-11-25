import { Member, Project, Task, TaskHistoryEntry, User } from '../app/core/models';

/** Jeu de données partagé par les tests. */

export const ALICE: User = { id: 1, username: 'alice', email: 'alice@pmt.test' };

export function project(role: Project['role'] = 'ADMIN'): Project {
  return {
    id: 1,
    name: 'Refonte du site',
    description: 'Nouvelle version du site vitrine',
    startDate: '2026-01-15',
    role,
  };
}

export const MEMBERS: Member[] = [
  { userId: 1, username: 'alice', email: 'alice@pmt.test', role: 'ADMIN' },
  { userId: 2, username: 'bob', email: 'bob@pmt.test', role: 'MEMBER' },
  { userId: 3, username: 'chloe', email: 'chloe@pmt.test', role: 'OBSERVER' },
];

export function task(overrides: Partial<Task> = {}): Task {
  return {
    id: 10,
    projectId: 1,
    name: 'Écrire la maquette',
    description: 'Maquette de la page accueil',
    dueDate: '2026-02-01',
    endDate: null,
    priority: 'HIGH',
    status: 'TODO',
    assigneeId: 2,
    assigneeName: 'bob',
    createdById: 1,
    createdByName: 'alice',
    createdAt: '2026-01-16T09:00:00',
    updatedAt: '2026-01-16T09:00:00',
    ...overrides,
  };
}

export const TASKS: Task[] = [
  task(),
  task({ id: 11, name: 'Configurer la CI', status: 'IN_PROGRESS', priority: 'MEDIUM' }),
  task({
    id: 12,
    name: 'Choisir le nom de domaine',
    status: 'DONE',
    priority: 'LOW',
    dueDate: null,
    assigneeId: null,
    assigneeName: null,
  }),
];

export const HISTORY: TaskHistoryEntry[] = [
  {
    id: 100,
    fieldName: 'status',
    oldValue: 'TODO',
    newValue: 'IN_PROGRESS',
    changedById: 2,
    changedByName: 'bob',
    changedAt: '2026-01-17T10:30:00',
  },
  {
    id: 99,
    fieldName: 'endDate',
    oldValue: null,
    newValue: '2026-01-20',
    changedById: 1,
    changedByName: 'alice',
    changedAt: '2026-01-17T08:00:00',
  },
];
