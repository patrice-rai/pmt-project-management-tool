import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ALICE } from '../../testing/fixtures';
import { AuthService, STORAGE_KEY } from './auth.service';

describe('AuthService', () => {
  let http: HttpTestingController;

  function createService(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http.verify());

  it('considère un visiteur comme non connecté', () => {
    const service = createService();
    expect(service.currentUser()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
  });

  it("envoie l'inscription sans ouvrir de session", () => {
    const service = createService();
    let created: unknown;
    service
      .register({ username: 'alice', email: 'alice@pmt.test', password: 'motdepasse' })
      .subscribe((user) => (created = user));

    const req = http.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.username).toBe('alice');
    req.flush(ALICE);

    expect(created).toEqual(ALICE);
    expect(service.isLoggedIn()).toBe(false);
  });

  it("garde l'utilisateur en mémoire et dans localStorage après connexion", () => {
    const service = createService();
    service.login({ email: 'alice@pmt.test', password: 'motdepasse' }).subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(ALICE);

    expect(service.currentUser()).toEqual(ALICE);
    expect(JSON.parse(localStorage.getItem(STORAGE_KEY)!)).toEqual(ALICE);
  });

  it("n'ouvre pas de session si la connexion échoue", () => {
    const service = createService();
    let status = 0;
    service
      .login({ email: 'alice@pmt.test', password: 'faux' })
      .subscribe({ error: (err) => (status = err.status) });

    http
      .expectOne('/api/auth/login')
      .flush({ message: 'Identifiants invalides' }, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
    expect(service.isLoggedIn()).toBe(false);
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('restaure la session enregistrée au démarrage', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ALICE));
    const service = createService();
    expect(service.currentUser()).toEqual(ALICE);
    expect(service.isLoggedIn()).toBe(true);
  });

  it('ignore et efface une session illisible', () => {
    localStorage.setItem(STORAGE_KEY, '{pas du json');
    const service = createService();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('ignore une session sans identifiant', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ username: 'alice' }));
    expect(createService().currentUser()).toBeNull();
  });

  it('efface la session à la déconnexion', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ALICE));
    const service = createService();
    service.logout();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });
});
