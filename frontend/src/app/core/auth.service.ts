import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginRequest, RegisterRequest, User } from './models';

/** Clé de `localStorage` sous laquelle l'utilisateur connecté est conservé. */
export const STORAGE_KEY = 'pmt.user';

/**
 * Gère l'inscription, la connexion et la session.
 *
 * Le back n'utilise pas Spring Security : après connexion, le front garde l'utilisateur
 * dans `localStorage` et l'intercepteur envoie son identifiant dans l'en-tête `X-User-Id`.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** Utilisateur connecté, ou `null` pour un visiteur. */
  readonly currentUser = signal<User | null>(this.readStoredUser());

  /** Crée un compte. L'utilisateur n'est pas connecté automatiquement. */
  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>('/api/auth/register', request);
  }

  /** Vérifie les identifiants et ouvre la session en cas de succès. */
  login(request: LoginRequest): Observable<User> {
    return this.http.post<User>('/api/auth/login', request).pipe(
      tap((user) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
        this.currentUser.set(user);
      }),
    );
  }

  /** Ferme la session côté navigateur. */
  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.currentUser.set(null);
  }

  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  /** Relit la session enregistrée ; une valeur illisible est ignorée et effacée. */
  private readStoredUser(): User | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      const user = JSON.parse(raw) as User;
      return typeof user?.id === 'number' ? user : null;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
