import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ALICE } from '../../../testing/fixtures';
import { AuthService } from '../../core/auth.service';
import { Login } from './login';

@Component({ template: '' })
class Blank {}

describe('Login', () => {
  let harness: RouterTestingHarness;
  let component: Login;
  let http: HttpTestingController;

  async function setup(url = '/login'): Promise<void> {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: Login },
          { path: 'projects', component: Blank },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    component = await harness.navigateByUrl(url, Login);
  }

  function submit(): void {
    harness.routeNativeElement!.querySelector('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  afterEach(() => http.verify());

  it("n'envoie rien tant que le formulaire est invalide", async () => {
    await setup();
    submit();

    http.expectNone('/api/auth/login');
    expect(harness.routeNativeElement!.textContent).toContain('adresse e-mail valide');
    expect(harness.routeNativeElement!.textContent).toContain('mot de passe est obligatoire');
  });

  it('connecte puis ouvre la liste des projets', async () => {
    await setup();
    component.form.setValue({ email: 'alice@pmt.test', password: 'motdepasse' });
    submit();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'alice@pmt.test', password: 'motdepasse' });
    req.flush(ALICE);
    await harness.fixture.whenStable();

    expect(TestBed.inject(AuthService).currentUser()).toEqual(ALICE);
    expect(TestBed.inject(Router).url).toBe('/projects');
  });

  it('affiche le message du back si les identifiants sont refusés', async () => {
    await setup();
    component.form.setValue({ email: 'alice@pmt.test', password: 'mauvais' });
    submit();

    http
      .expectOne('/api/auth/login')
      .flush(
        { message: 'E-mail ou mot de passe incorrect' },
        { status: 401, statusText: 'Unauthorized' },
      );
    harness.detectChanges();

    expect(harness.routeNativeElement!.querySelector('[role="alert"]')!.textContent).toContain(
      'E-mail ou mot de passe incorrect',
    );
    expect(component.submitting).toBe(false);
  });

  it("confirme la création du compte après l'inscription", async () => {
    await setup('/login?registered=1');
    expect(harness.routeNativeElement!.querySelector('[role="status"]')!.textContent).toContain(
      'Votre compte est créé',
    );
  });

  it("n'affiche pas ce message lors d'une visite normale", async () => {
    await setup();
    expect(harness.routeNativeElement!.querySelector('[role="status"]')).toBeNull();
  });
});
