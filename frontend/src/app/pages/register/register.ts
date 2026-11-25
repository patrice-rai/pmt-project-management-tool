import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/labels';

/** Page d'inscription : nom d'utilisateur, e-mail et mot de passe. */
@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  error = '';
  /** Messages de validation renvoyés par le back, champ par champ. */
  fieldErrors: Record<string, string> = {};
  submitting = false;

  /** Crée le compte puis renvoie vers la connexion. */
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error = '';
    this.fieldErrors = {};
    this.submitting = true;
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: 1 } }),
      error: (err) => {
        this.error = errorMessage(err);
        if (err instanceof HttpErrorResponse && err.error?.details) {
          this.fieldErrors = err.error.details;
        }
        this.submitting = false;
      },
    });
  }
}
