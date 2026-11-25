import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

/** Coquille de l'application : barre de navigation et zone où s'affichent les pages. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Utilisateur connecté, lu directement par le gabarit. */
  protected readonly user = this.auth.currentUser;

  /** Déconnecte l'utilisateur et le renvoie vers la page de connexion. */
  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
