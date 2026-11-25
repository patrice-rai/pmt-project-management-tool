import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { project } from '../../../testing/fixtures';
import { Projects } from './projects';

describe('Projects', () => {
  let fixture: ComponentFixture<Projects>;
  let component: Projects;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Projects],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Projects);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function text(): string {
    return fixture.nativeElement.textContent;
  }

  function submit(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('affiche mes projets avec mon rôle', () => {
    expect(text()).toContain('Chargement');

    http.expectOne('/api/projects').flush([
      project('ADMIN'),
      { ...project('OBSERVER'), id: 2, name: 'Appli mobile', description: null },
    ]);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll('.project-link');
    expect(links.length).toBe(2);
    expect(links[0].textContent).toContain('Refonte du site');
    expect(links[0].textContent).toContain('Administrateur');
    expect(links[0].textContent).toContain('15/01/2026');
    expect(links[1].textContent).toContain('Observateur');
    expect(links[0].getAttribute('href')).toBe('/projects/1');
  });

  it("invite à créer un projet quand la liste est vide", () => {
    http.expectOne('/api/projects').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="empty"]')).not.toBeNull();
  });

  it('affiche une erreur si le chargement échoue', () => {
    http
      .expectOne('/api/projects')
      .flush({ message: 'Service indisponible' }, { status: 503, statusText: 'KO' });
    fixture.detectChanges();
    expect(text()).toContain('Service indisponible');
  });

  it('refuse un formulaire incomplet', () => {
    http.expectOne('/api/projects').flush([]);
    submit();

    http.expectNone((req) => req.method === 'POST');
    expect(text()).toContain('Le nom est obligatoire');
    expect(text()).toContain('La date de début est obligatoire');
  });

  it('crée un projet et le place en tête de liste', () => {
    http.expectOne('/api/projects').flush([project()]);
    component.form.setValue({ name: 'Appli mobile', description: '  ', startDate: '2026-03-01' });
    submit();

    const req = http.expectOne((r) => r.method === 'POST' && r.url === '/api/projects');
    // Une description vide est envoyée comme absente.
    expect(req.request.body).toEqual({
      name: 'Appli mobile',
      description: null,
      startDate: '2026-03-01',
    });
    req.flush({ id: 2, name: 'Appli mobile', description: null, startDate: '2026-03-01', role: 'ADMIN' });
    fixture.detectChanges();

    expect(component.projects.map((p) => p.id)).toEqual([2, 1]);
    expect(component.form.getRawValue().name).toBe('');
    expect(component.submitting).toBe(false);
  });

  it('affiche le refus du back à la création', () => {
    http.expectOne('/api/projects').flush([]);
    component.form.setValue({ name: 'Appli', description: 'Une appli', startDate: '2026-03-01' });
    submit();

    const req = http.expectOne((r) => r.method === 'POST');
    expect(req.request.body.description).toBe('Une appli');
    req.flush({ message: 'Nom de projet invalide' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(text()).toContain('Nom de projet invalide');
    expect(component.projects).toEqual([]);
  });
});
