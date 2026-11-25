import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage, fieldLabel, historyValue } from './labels';

describe('libellés', () => {
  it("traduit les champs connus de l'historique et laisse les autres tels quels", () => {
    expect(fieldLabel('dueDate')).toBe("Date d'échéance");
    expect(fieldLabel('inconnu')).toBe('inconnu');
  });

  it("traduit les valeurs de statut et de priorité de l'historique", () => {
    expect(historyValue('status', 'IN_PROGRESS')).toBe('En cours');
    expect(historyValue('priority', 'HIGH')).toBe('Haute');
  });

  it('affiche un tiret pour une valeur absente et garde les valeurs libres', () => {
    expect(historyValue('endDate', null)).toBe('—');
    expect(historyValue('name', '')).toBe('—');
    expect(historyValue('name', 'Maquette')).toBe('Maquette');
    expect(historyValue('status', 'ARCHIVED')).toBe('ARCHIVED');
    expect(historyValue('priority', 'URGENT')).toBe('URGENT');
  });
});

describe('errorMessage', () => {
  it('reprend le message renvoyé par le back', () => {
    const err = new HttpErrorResponse({ status: 409, error: { message: 'E-mail déjà utilisé' } });
    expect(errorMessage(err)).toBe('E-mail déjà utilisé');
  });

  it('signale un serveur injoignable', () => {
    const err = new HttpErrorResponse({ status: 0, error: null });
    expect(errorMessage(err)).toContain('injoignable');
  });

  it('retombe sur un message générique', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 500, error: 'boom' }))).toBe(
      'Une erreur inattendue est survenue.',
    );
    expect(errorMessage(new HttpErrorResponse({ status: 500, error: {} }))).toBe(
      'Une erreur inattendue est survenue.',
    );
    expect(errorMessage(new Error('boom'))).toBe('Une erreur inattendue est survenue.');
  });
});
