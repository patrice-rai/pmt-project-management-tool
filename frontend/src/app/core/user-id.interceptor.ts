import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Ajoute l'en-tête `X-User-Id` aux appels de l'API quand un utilisateur est connecté.
 * Les routes d'authentification n'en ont pas besoin et sont laissées intactes.
 */
export const userIdInterceptor: HttpInterceptorFn = (req, next) => {
  const user = inject(AuthService).currentUser();
  if (!user || !req.url.startsWith('/api/') || req.url.startsWith('/api/auth/')) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'X-User-Id': String(user.id) } }));
};
