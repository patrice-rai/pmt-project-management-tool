import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ALICE, MEMBERS, TASKS, project, task } from '../../../testing/fixtures';
import { STORAGE_KEY } from '../../core/auth.service';
import { Role } from '../../core/models';
import { userIdInterceptor } from '../../core/user-id.interceptor';
import { ProjectDashboard } from './project-dashboard';

/**
 * Tests d'intégration du tableau de bord : le composant, les vrais services,
 * l'intercepteur et le routeur fonctionnent ensemble ; seul le réseau est simulé.
 */
describe('ProjectDashboard', () => {
  let harness: RouterTestingHarness;
  let component: ProjectDashboard;
  let http: HttpTestingController;

  async function open(): Promise<void> {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ALICE));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([userIdInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'projects/:id', component: ProjectDashboard }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    component = await harness.navigateByUrl('/projects/1', ProjectDashboard);
  }

  /** Ouvre la page et répond aux trois appels de chargement. */
  async function setup(role: Role): Promise<void> {
    await open();
    http.expectOne('/api/projects/1').flush(project(role));
    http.expectOne('/api/projects/1/members').flush(MEMBERS);
    http.expectOne('/api/projects/1/tasks').flush(TASKS);
    harness.detectChanges();
  }

  function el(): HTMLElement {
    return harness.routeNativeElement!;
  }

  function submit(form: HTMLFormElement): void {
    form.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it("identifie l'utilisateur auprès de l'API et range les tâches par statut", async () => {
    await open();
    const projectReq = http.expectOne('/api/projects/1');
    expect(projectReq.request.headers.get('X-User-Id')).toBe('1');
    projectReq.flush(project('ADMIN'));
    http.expectOne('/api/projects/1/members').flush(MEMBERS);
    http.expectOne('/api/projects/1/tasks').flush(TASKS);
    harness.detectChanges();

    expect(el().querySelector('h1')!.textContent).toContain('Refonte du site');
    const columns = el().querySelectorAll('.board-column');
    expect(columns.length).toBe(3);
    expect(columns[0].textContent).toContain('À faire');
    expect(columns[0].textContent).toContain('Écrire la maquette');
    expect(columns[0].textContent).toContain('01/02/2026');
    expect(columns[1].textContent).toContain('Configurer la CI');
    expect(columns[2].textContent).toContain('Choisir le nom de domaine');
    expect(columns[2].textContent).toContain('Non assignée');

    const card = el().querySelector('.task-card')!;
    expect(card.getAttribute('href')).toBe('/projects/1/tasks/10');
  });

  it('montre toutes les actions à un administrateur', async () => {
    await setup('ADMIN');
    expect(component.isAdmin).toBe(true);
    expect(component.canEdit).toBe(true);
    expect(el().querySelector('#task-name')).not.toBeNull();
    expect(el().querySelector('#invite-email')).not.toBeNull();
    expect(el().querySelectorAll('.member-row select').length).toBe(3);
  });

  it('laisse un membre créer des tâches mais pas gérer les membres', async () => {
    await setup('MEMBER');
    expect(component.isAdmin).toBe(false);
    expect(el().querySelector('#task-name')).not.toBeNull();
    expect(el().querySelector('#invite-email')).toBeNull();
    expect(el().querySelectorAll('.member-row select').length).toBe(0);
    expect(el().querySelector('.member-row')!.textContent).toContain('Administrateur');
  });

  it("ne montre aucune action de modification à un observateur", async () => {
    await setup('OBSERVER');
    expect(component.canEdit).toBe(false);
    expect(el().querySelector('[data-testid="my-role"]')!.textContent).toContain('Observateur');
    expect(el().querySelector('#task-name')).toBeNull();
    expect(el().querySelector('#invite-email')).toBeNull();
    expect(el().querySelectorAll('form').length).toBe(0);
    // Le tableau de bord reste visible.
    expect(el().querySelectorAll('.task-card').length).toBe(3);
  });

  it("affiche l'erreur quand le projet est interdit", async () => {
    await open();
    http
      .expectOne('/api/projects/1')
      .flush(
        { message: "Vous n'êtes pas membre de ce projet" },
        { status: 403, statusText: 'Forbidden' },
      );
    http.expectOne('/api/projects/1/members');
    http.expectOne('/api/projects/1/tasks');
    harness.detectChanges();

    expect(el().querySelector('[role="alert"]')!.textContent).toContain('pas membre');
    expect(component.isAdmin).toBe(false);
    expect(component.canEdit).toBe(false);
  });

  it('filtre le tableau par statut puis revient à tous les statuts', async () => {
    await setup('ADMIN');
    const select = el().querySelector<HTMLSelectElement>('#status-filter')!;

    select.value = 'DONE';
    select.dispatchEvent(new Event('change'));
    http.expectOne('/api/projects/1/tasks?status=DONE').flush([TASKS[2]]);
    harness.detectChanges();

    const columns = el().querySelectorAll('.board-column');
    expect(columns.length).toBe(1);
    expect(columns[0].getAttribute('data-status')).toBe('DONE');
    expect(el().querySelectorAll('.task-card').length).toBe(1);

    select.value = '';
    select.dispatchEvent(new Event('change'));
    http.expectOne('/api/projects/1/tasks').flush(TASKS);
    harness.detectChanges();
    expect(el().querySelectorAll('.board-column').length).toBe(3);
  });

  it('signale une erreur de filtrage', async () => {
    await setup('ADMIN');
    component.filterByStatus('TODO');
    http
      .expectOne('/api/projects/1/tasks?status=TODO')
      .flush({ message: 'Statut inconnu' }, { status: 400, statusText: 'Bad Request' });
    harness.detectChanges();
    expect(el().querySelector('[data-testid="task-error"]')!.textContent).toContain(
      'Statut inconnu',
    );
  });

  it('indique une colonne vide', async () => {
    await open();
    http.expectOne('/api/projects/1').flush({ ...project('MEMBER'), description: null });
    http.expectOne('/api/projects/1/members').flush(MEMBERS);
    http.expectOne('/api/projects/1/tasks').flush([]);
    harness.detectChanges();
    expect(el().querySelector('.board-column')!.textContent).toContain('Aucune tâche');
  });

  describe('invitation et rôles (administrateur)', () => {
    beforeEach(async () => setup('ADMIN'));

    function inviteForm(): HTMLFormElement {
      return el().querySelector<HTMLFormElement>('aside form')!;
    }

    it("refuse une adresse d'invitation invalide", () => {
      component.inviteForm.setValue({ email: 'pas-un-email', role: 'MEMBER' });
      submit(inviteForm());
      http.expectNone((req) => req.method === 'POST');
      expect(el().textContent).toContain('adresse e-mail valide');
    });

    it('invite un membre avec le rôle choisi', () => {
      component.inviteForm.setValue({ email: 'dan@pmt.test', role: 'OBSERVER' });
      submit(inviteForm());

      const req = http.expectOne((r) => r.method === 'POST');
      expect(req.request.url).toBe('/api/projects/1/members');
      expect(req.request.body).toEqual({ email: 'dan@pmt.test', role: 'OBSERVER' });
      req.flush({ userId: 4, username: 'dan', email: 'dan@pmt.test', role: 'OBSERVER' });
      harness.detectChanges();

      expect(el().querySelectorAll('.member-row').length).toBe(4);
      expect(component.inviteForm.getRawValue()).toEqual({ email: '', role: 'MEMBER' });
    });

    it("affiche l'erreur si aucun compte ne correspond à l'e-mail", () => {
      component.inviteForm.setValue({ email: 'inconnu@pmt.test', role: 'MEMBER' });
      submit(inviteForm());

      http
        .expectOne((r) => r.method === 'POST')
        .flush(
          { message: 'Aucun compte avec cette adresse e-mail' },
          { status: 404, statusText: 'Not Found' },
        );
      harness.detectChanges();

      expect(el().querySelector('[data-testid="member-error"]')!.textContent).toContain(
        'Aucun compte',
      );
      expect(component.members.length).toBe(3);
    });

    it("change le rôle d'un membre", () => {
      const select = el().querySelectorAll<HTMLSelectElement>('.member-row select')[1];
      select.value = 'OBSERVER';
      select.dispatchEvent(new Event('change'));

      const req = http.expectOne('/api/projects/1/members/2');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ role: 'OBSERVER' });
      req.flush({ ...MEMBERS[1], role: 'OBSERVER' });
      harness.detectChanges();

      expect(component.members[1].role).toBe('OBSERVER');
      expect(component.members[0].role).toBe('ADMIN');
    });

    it('garde le rôle actuel si le back refuse (dernier administrateur)', () => {
      component.changeRole(MEMBERS[0], 'MEMBER');
      http
        .expectOne('/api/projects/1/members/1')
        .flush(
          { message: 'Un projet doit garder au moins un administrateur' },
          { status: 409, statusText: 'Conflict' },
        );
      harness.detectChanges();

      expect(component.members[0].role).toBe('ADMIN');
      expect(el().querySelector('[data-testid="member-error"]')!.textContent).toContain(
        'au moins un administrateur',
      );
    });
  });

  describe('création de tâche', () => {
    function taskForm(): HTMLFormElement {
      return el().querySelector<HTMLFormElement>('section.card form')!;
    }

    it('refuse une tâche sans nom', async () => {
      await setup('MEMBER');
      submit(taskForm());
      http.expectNone((req) => req.method === 'POST');
      expect(el().textContent).toContain('Le nom est obligatoire');
    });

    it('crée une tâche assignée et l’ajoute au tableau', async () => {
      await setup('MEMBER');
      component.taskForm.setValue({
        name: 'Rédiger les CGU',
        description: 'Avec le service juridique',
        dueDate: '2026-04-01',
        priority: 'HIGH',
        assigneeId: '3',
      });
      submit(taskForm());

      const req = http.expectOne((r) => r.method === 'POST');
      expect(req.request.url).toBe('/api/projects/1/tasks');
      expect(req.request.body).toEqual({
        name: 'Rédiger les CGU',
        description: 'Avec le service juridique',
        dueDate: '2026-04-01',
        priority: 'HIGH',
        assigneeId: 3,
      });
      req.flush(task({ id: 13, name: 'Rédiger les CGU', assigneeId: 3, assigneeName: 'chloe' }));
      harness.detectChanges();

      expect(el().querySelector('[data-status="TODO"]')!.textContent).toContain('Rédiger les CGU');
      expect(component.taskForm.getRawValue().priority).toBe('MEDIUM');
    });

    it('envoie les champs facultatifs vides comme absents', async () => {
      await setup('ADMIN');
      component.taskForm.patchValue({ name: 'Tâche minimale' });
      submit(taskForm());

      const req = http.expectOne((r) => r.method === 'POST');
      expect(req.request.body).toEqual({
        name: 'Tâche minimale',
        description: null,
        dueDate: null,
        priority: 'MEDIUM',
        assigneeId: null,
      });
      req.flush(task({ id: 14, name: 'Tâche minimale' }));
    });

    it("n'affiche pas la nouvelle tâche si elle sort du filtre courant", async () => {
      await setup('ADMIN');
      component.filterByStatus('DONE');
      http.expectOne('/api/projects/1/tasks?status=DONE').flush([TASKS[2]]);

      component.taskForm.patchValue({ name: 'Nouvelle tâche' });
      component.createTask();
      http.expectOne((r) => r.method === 'POST').flush(task({ id: 15, status: 'TODO' }));

      expect(component.tasks.map((t) => t.id)).toEqual([12]);
    });

    it('affiche le refus du back', async () => {
      await setup('MEMBER');
      component.taskForm.patchValue({ name: 'Tâche refusée' });
      submit(taskForm());

      http
        .expectOne((r) => r.method === 'POST')
        .flush({ message: 'Action interdite' }, { status: 403, statusText: 'Forbidden' });
      harness.detectChanges();

      expect(el().querySelector('[data-testid="task-error"]')!.textContent).toContain(
        'Action interdite',
      );
      expect(component.tasks.length).toBe(3);
    });
  });
});
