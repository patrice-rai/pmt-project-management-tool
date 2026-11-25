import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ALICE } from '../../testing/fixtures';
import { STORAGE_KEY } from './auth.service';
import { userIdInterceptor } from './user-id.interceptor';

describe('userIdInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  function setup(loggedIn: boolean): void {
    localStorage.clear();
    if (loggedIn) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(ALICE));
    }
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([userIdInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  }

  afterEach(() => controller.verify());

  it("ajoute l'identifiant de l'utilisateur connecté aux appels de l'API", () => {
    setup(true);
    http.get('/api/projects').subscribe();
    const req = controller.expectOne('/api/projects');
    expect(req.request.headers.get('X-User-Id')).toBe('1');
    req.flush([]);
  });

  it("n'ajoute rien pour un visiteur", () => {
    setup(false);
    http.get('/api/projects').subscribe();
    const req = controller.expectOne('/api/projects');
    expect(req.request.headers.has('X-User-Id')).toBe(false);
    req.flush([]);
  });

  it("n'ajoute rien sur les routes d'authentification", () => {
    setup(true);
    http.post('/api/auth/login', {}).subscribe();
    const req = controller.expectOne('/api/auth/login');
    expect(req.request.headers.has('X-User-Id')).toBe(false);
    req.flush({});
  });

  it("n'ajoute rien hors de l'API", () => {
    setup(true);
    http.get('/assets/logo.svg').subscribe();
    const req = controller.expectOne('/assets/logo.svg');
    expect(req.request.headers.has('X-User-Id')).toBe(false);
    req.flush('');
  });
});
