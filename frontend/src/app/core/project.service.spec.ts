import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MEMBERS, project } from '../../testing/fixtures';
import { ProjectService } from './project.service';

describe('ProjectService', () => {
  let service: ProjectService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les projets', () => {
    let count = 0;
    service.list().subscribe((projects) => (count = projects.length));
    const req = http.expectOne('/api/projects');
    expect(req.request.method).toBe('GET');
    req.flush([project()]);
    expect(count).toBe(1);
  });

  it('crée un projet', () => {
    const body = { name: 'Refonte', description: null, startDate: '2026-01-15' };
    service.create(body).subscribe();
    const req = http.expectOne('/api/projects');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(project());
  });

  it('lit un projet', () => {
    service.get(1).subscribe();
    const req = http.expectOne('/api/projects/1');
    expect(req.request.method).toBe('GET');
    req.flush(project());
  });

  it("liste les membres d'un projet", () => {
    service.members(1).subscribe();
    const req = http.expectOne('/api/projects/1/members');
    expect(req.request.method).toBe('GET');
    req.flush(MEMBERS);
  });

  it('invite un membre par e-mail', () => {
    service.invite(1, { email: 'bob@pmt.test', role: 'MEMBER' }).subscribe();
    const req = http.expectOne('/api/projects/1/members');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'bob@pmt.test', role: 'MEMBER' });
    req.flush(MEMBERS[1]);
  });

  it("change le rôle d'un membre", () => {
    service.changeRole(1, 2, 'OBSERVER').subscribe();
    const req = http.expectOne('/api/projects/1/members/2');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ role: 'OBSERVER' });
    req.flush({ ...MEMBERS[1], role: 'OBSERVER' });
  });
});
