import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/labels';

/** Page de connexion par e-mail et mot de passe. */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  /** Vrai quand on arrive juste après une inscription réussie. */
  readonly justRegistered = this.route.snapshot.queryParamMap.get('registered') === '1';

  error = '';
  submitting = false;

  /** Envoie les identifiants ; en cas de succès, ouvre la liste des projets. */
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error = '';
    this.submitting = true;
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/projects']),
      error: (err) => {
        this.error = errorMessage(err);
        this.submitting = false;
      },
    });
  }
}
