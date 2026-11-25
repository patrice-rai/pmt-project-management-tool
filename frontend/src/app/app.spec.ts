import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { ALICE } from '../testing/fixtures';
import { App } from './app';
import { STORAGE_KEY } from './core/auth.service';

describe('App', () => {
  let fixture: ComponentFixture<App>;

  function setup(loggedIn: boolean): void {
    localStorage.clear();
    if (loggedIn) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(ALICE));
    }
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  }

  it('propose la connexion et l’inscription à un visiteur', () => {
    setup(false);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Connexion');
    expect(text).toContain('Inscription');
    expect(text).not.toContain('Se déconnecter');
  });

  it("affiche le nom de l'utilisateur connecté", () => {
    setup(true);
    const user = fixture.nativeElement.querySelector('[data-testid="current-user"]');
    expect(user.textContent).toContain('alice');
  });

  it('déconnecte et renvoie vers la connexion', () => {
    setup(true);
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();

    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(fixture.nativeElement.textContent).toContain('Connexion');
  });
});
