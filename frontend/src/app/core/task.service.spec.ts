import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { HISTORY, TASKS, task } from '../../testing/fixtures';
import { TaskService } from './task.service';

describe('TaskService', () => {
  let service: TaskService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TaskService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it("liste toutes les tâches d'un projet", () => {
    service.list(1).subscribe();
    const req = http.expectOne('/api/projects/1/tasks');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(TASKS);
  });

  it('filtre les tâches par statut', () => {
    service.list(1, 'DONE').subscribe();
    const req = http.expectOne('/api/projects/1/tasks?status=DONE');
    expect(req.request.params.get('status')).toBe('DONE');
    req.flush([TASKS[2]]);
  });

  it('crée une tâche', () => {
    const body = {
      name: 'Maquette',
      description: null,
      dueDate: '2026-02-01',
      priority: 'HIGH' as const,
      assigneeId: 2,
    };
    service.create(1, body).subscribe();
    const req = http.expectOne('/api/projects/1/tasks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(task());
  });

  it('lit une tâche', () => {
    service.get(10).subscribe();
    const req = http.expectOne('/api/tasks/10');
    expect(req.request.method).toBe('GET');
    req.flush(task());
  });

  it('met à jour une tâche', () => {
    service
      .update(10, {
        name: 'Maquette',
        description: null,
        dueDate: null,
        endDate: '2026-02-03',
        priority: 'LOW',
        status: 'DONE',
        assigneeId: null,
      })
      .subscribe();
    const req = http.expectOne('/api/tasks/10');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.endDate).toBe('2026-02-03');
    req.flush(task({ status: 'DONE' }));
  });

  it('assigne et désassigne une tâche', () => {
    service.assign(10, 3).subscribe();
    const assign = http.expectOne('/api/tasks/10/assignee');
    expect(assign.request.method).toBe('PATCH');
    expect(assign.request.body).toEqual({ assigneeId: 3 });
    assign.flush(task({ assigneeId: 3 }));

    service.assign(10, null).subscribe();
    const unassign = http.expectOne('/api/tasks/10/assignee');
    expect(unassign.request.body).toEqual({ assigneeId: null });
    unassign.flush(task({ assigneeId: null }));
  });

  it("lit l'historique d'une tâche", () => {
    let entries = 0;
    service.history(10).subscribe((history) => (entries = history.length));
    const req = http.expectOne('/api/tasks/10/history');
    expect(req.request.method).toBe('GET');
    req.flush(HISTORY);
    expect(entries).toBe(2);
  });
});
