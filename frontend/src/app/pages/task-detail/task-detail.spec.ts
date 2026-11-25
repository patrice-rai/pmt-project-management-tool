import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ALICE, HISTORY, MEMBERS, project, task } from '../../../testing/fixtures';
import { STORAGE_KEY } from '../../core/auth.service';
import { Role, Task } from '../../core/models';
import { userIdInterceptor } from '../../core/user-id.interceptor';
import { TaskDetail } from './task-detail';

/** Tests d'intégration du détail d'une tâche (composant + services réels + intercepteur). */
describe('TaskDetail', () => {
  let harness: RouterTestingHarness;
  let component: TaskDetail;
  let http: HttpTestingController;

  async function open(): Promise<void> {
    localStorage.clear();
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ALICE));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([userIdInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'projects/:id/tasks/:taskId', component: TaskDetail }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    component = await harness.navigateByUrl('/projects/1/tasks/10', TaskDetail);
  }

  async function setup(role: Role, shown: Task = task()): Promise<void> {
    await open();
    http.expectOne('/api/projects/1').flush(project(role));
    http.expectOne('/api/projects/1/members').flush(MEMBERS);
    http.expectOne('/api/tasks/10').flush(shown);
    http.expectOne('/api/tasks/10/history').flush(HISTORY);
    harness.detectChanges();
  }

  function el(): HTMLElement {
    return harness.routeNativeElement!;
  }

  afterEach(() => http.verify());

  it("affiche toutes les informations de la tâche et son historique", async () => {
    await setup('ADMIN');

    expect(el().querySelector('h1')!.textContent).toContain('Écrire la maquette');
    const details = el().querySelector('.details')!.textContent!;
    expect(details).toContain('À faire');
    expect(details).toContain('Haute');
    expect(details).toContain('Maquette de la page accueil');
    expect(details).toContain('01/02/2026');
    expect(details).toContain('bob');
    expect(details).toContain('alice');

    const entries = el().querySelectorAll('.history-entry');
    expect(entries.length).toBe(2);
    expect(entries[0].textContent).toContain('Statut');
    expect(entries[0].textContent).toContain('À faire');
    expect(entries[0].textContent).toContain('En cours');
    expect(entries[1].textContent).toContain('Date de fin');
    expect(entries[1].textContent).toContain('—');
  });

  it('préremplit le formulaire de mise à jour', async () => {
    await setup('MEMBER');
    expect(component.form.getRawValue()).toEqual({
      name: 'Écrire la maquette',
      description: 'Maquette de la page accueil',
      dueDate: '2026-02-01',
      endDate: '',
      priority: 'HIGH',
      status: 'TODO',
    });
    expect(el().querySelector<HTMLSelectElement>('#assignee')!.value).toBe('2');
  });

  it("gère une tâche sans description, sans dates et sans assigné", async () => {
    await setup(
      'ADMIN',
      task({ description: null, dueDate: null, assigneeId: null, assigneeName: null }),
    );
    expect(el().querySelector('[data-testid="assignee"]')!.textContent).toContain('Personne');
    expect(component.form.getRawValue().description).toBe('');
    expect(el().querySelector<HTMLSelectElement>('#assignee')!.value).toBe('');
  });

  it("met la tâche en lecture seule pour un observateur", async () => {
    await setup('OBSERVER');
    expect(component.canEdit).toBe(false);
    expect(el().querySelector('form')).toBeNull();
    expect(el().querySelector('#assignee')).toBeNull();
    expect(el().querySelector('[data-testid="read-only"]')).not.toBeNull();
    // L'historique reste consultable.
    expect(el().querySelectorAll('.history-entry').length).toBe(2);
  });

  it("indique quand l'historique est vide", async () => {
    await open();
    http.expectOne('/api/projects/1').flush(project('MEMBER'));
    http.expectOne('/api/projects/1/members').flush(MEMBERS);
    http.expectOne('/api/tasks/10').flush(task());
    http.expectOne('/api/tasks/10/history').flush([]);
    harness.detectChanges();
    expect(el().textContent).toContain('Aucune modification');
  });

  it("affiche l'erreur de chargement", async () => {
    await open();
    http.expectOne('/api/projects/1').flush(project('MEMBER'));
    http.expectOne('/api/projects/1/members').flush(MEMBERS);
    http
      .expectOne('/api/tasks/10')
      .flush({ message: 'Tâche introuvable' }, { status: 404, statusText: 'Not Found' });
    http.expectOne('/api/tasks/10/history');
    harness.detectChanges();

    expect(el().querySelector('[role="alert"]')!.textContent).toContain('Tâche introuvable');
    expect(component.canEdit).toBe(false);
  });

  it('met à jour la tâche, ajoute une date de fin et recharge l’historique', async () => {
    await setup('MEMBER');
    component.form.patchValue({ status: 'DONE', endDate: '2026-01-30', description: ' ' });
    el().querySelector('form')!.dispatchEvent(new Event('submit'));

    const req = http.expectOne('/api/tasks/10');
    expect(req.request.method).toBe('PUT');
    expect(req.request.headers.get('X-User-Id')).toBe('1');
    expect(req.request.body).toEqual({
      name: 'Écrire la maquette',
      description: null,
      dueDate: '2026-02-01',
      endDate: '2026-01-30',
      priority: 'HIGH',
      status: 'DONE',
      assigneeId: 2,
    });
    req.flush(task({ status: 'DONE', endDate: '2026-01-30', description: null }));

    http.expectOne('/api/tasks/10/history').flush([
      { ...HISTORY[0], id: 101, oldValue: 'TODO', newValue: 'DONE' },
      ...HISTORY,
    ]);
    harness.detectChanges();

    expect(el().querySelector('[role="status"]')!.textContent).toContain('mise à jour');
    expect(el().querySelector('.details')!.textContent).toContain('Terminée');
    expect(el().querySelector('.details')!.textContent).toContain('30/01/2026');
    expect(el().querySelectorAll('.history-entry').length).toBe(3);
  });

  it('envoie des dates absentes quand elles sont vidées', async () => {
    await setup('ADMIN');
    component.form.patchValue({ dueDate: '', endDate: '' });
    component.save();

    const req = http.expectOne('/api/tasks/10');
    expect(req.request.body.dueDate).toBeNull();
    expect(req.request.body.endDate).toBeNull();
    req.flush(task({ dueDate: null }));
    http.expectOne('/api/tasks/10/history').flush(HISTORY);
  });

  it("n'envoie pas une mise à jour sans nom", async () => {
    await setup('ADMIN');
    component.form.patchValue({ name: '' });
    el().querySelector('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();

    http.expectNone((req) => req.method === 'PUT');
    expect(el().textContent).toContain('Le nom est obligatoire');
  });

  it('affiche le refus du back à la mise à jour', async () => {
    await setup('MEMBER');
    component.save();
    http
      .expectOne('/api/tasks/10')
      .flush(
        { message: 'La date de fin précède la création' },
        { status: 400, statusText: 'Bad Request' },
      );
    harness.detectChanges();

    expect(el().querySelector('[role="alert"]')!.textContent).toContain('date de fin');
    expect(component.success).toBe('');
  });

  it('assigne la tâche à un membre et annonce l’e-mail', async () => {
    await setup('ADMIN');
    const select = el().querySelector<HTMLSelectElement>('#assignee')!;
    select.value = '3';
    select.dispatchEvent(new Event('change'));

    const req = http.expectOne('/api/tasks/10/assignee');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ assigneeId: 3 });
    req.flush(task({ assigneeId: 3, assigneeName: 'chloe' }));
    http.expectOne('/api/tasks/10/history').flush(HISTORY);
    harness.detectChanges();

    expect(el().querySelector('[data-testid="assignee"]')!.textContent).toContain('chloe');
    expect(el().querySelector('[role="status"]')!.textContent).toContain('e-mail');
  });

  it('désassigne la tâche', async () => {
    await setup('ADMIN');
    component.assign('');

    const req = http.expectOne('/api/tasks/10/assignee');
    expect(req.request.body).toEqual({ assigneeId: null });
    req.flush(task({ assigneeId: null, assigneeName: null }));
    http.expectOne('/api/tasks/10/history').flush(HISTORY);

    expect(component.success).toContain("n'est plus assignée");
  });

  it("affiche le refus du back à l'assignation", async () => {
    await setup('MEMBER');
    component.assign('3');
    http
      .expectOne('/api/tasks/10/assignee')
      .flush(
        { message: "Cet utilisateur n'est pas membre du projet" },
        { status: 400, statusText: 'Bad Request' },
      );
    harness.detectChanges();

    expect(el().querySelector('[role="alert"]')!.textContent).toContain('pas membre du projet');
  });

  it("signale l'échec du rechargement de l'historique", async () => {
    await setup('ADMIN');
    component.assign('3');
    http.expectOne('/api/tasks/10/assignee').flush(task({ assigneeId: 3, assigneeName: 'chloe' }));
    http
      .expectOne('/api/tasks/10/history')
      .flush({ message: 'Historique indisponible' }, { status: 500, statusText: 'KO' });

    expect(component.error).toBe('Historique indisponible');
  });
});
