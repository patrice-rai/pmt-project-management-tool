import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ALICE } from '../../../testing/fixtures';
import { Register } from './register';

@Component({ template: '' })
class Blank {}

describe('Register', () => {
  let harness: RouterTestingHarness;
  let component: Register;
  let http: HttpTestingController;

  const VALID = { username: 'alice', email: 'alice@pmt.test', password: 'motdepasse' };

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'register', component: Register },
          { path: 'login', component: Blank },
        ]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    component = await harness.navigateByUrl('/register', Register);
  });

  afterEach(() => http.verify());

  function submit(): void {
    harness.routeNativeElement!.querySelector('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  it('signale les champs invalides sans appeler le back', () => {
    component.form.setValue({ username: 'al', email: 'pas-un-email', password: 'court' });
    submit();

    http.expectNone('/api/auth/register');
    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Entre 3 et 50 caractères');
    expect(text).toContain('adresse e-mail valide');
    expect(text).toContain('Au moins 8 caractères');
  });

  it('crée le compte puis renvoie vers la connexion', async () => {
    component.form.setValue(VALID);
    submit();

    const req = http.expectOne('/api/auth/register');
    expect(req.request.body).toEqual(VALID);
    req.flush(ALICE, { status: 201, statusText: 'Created' });
    await harness.fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/login?registered=1');
  });

  it("affiche le conflit quand l'e-mail est déjà pris", () => {
    component.form.setValue(VALID);
    submit();

    http
      .expectOne('/api/auth/register')
      .flush(
        { message: 'Cette adresse e-mail est déjà utilisée', details: {} },
        { status: 409, statusText: 'Conflict' },
      );
    harness.detectChanges();

    expect(harness.routeNativeElement!.querySelector('[role="alert"]')!.textContent).toContain(
      'déjà utilisée',
    );
    expect(component.submitting).toBe(false);
  });

  it('affiche les erreurs de validation du back sous chaque champ', () => {
    component.form.setValue(VALID);
    submit();

    http.expectOne('/api/auth/register').flush(
      {
        message: 'Données invalides',
        details: {
          username: 'Nom réservé',
          email: 'Domaine refusé',
          password: 'Mot de passe trop simple',
        },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    harness.detectChanges();

    const text = harness.routeNativeElement!.textContent!;
    expect(text).toContain('Nom réservé');
    expect(text).toContain('Domaine refusé');
    expect(text).toContain('Mot de passe trop simple');
  });

  it('garde un message générique si le serveur ne répond pas', () => {
    component.form.setValue(VALID);
    submit();

    http.expectOne('/api/auth/register').error(new ProgressEvent('error'));
    harness.detectChanges();

    expect(component.error).toContain('injoignable');
    expect(component.fieldErrors).toEqual({});
  });
});
