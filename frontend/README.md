# PMT · front Angular

Interface de PMT (Project Management Tool), écrite avec Angular 20 et testée avec Jest.

## Commandes

| Besoin | Commande |
|---|---|
| Installer les dépendances | `npm ci` |
| Lancer en développement (http://localhost:4200) | `npm start` |
| Lancer les tests | `npm test` |
| Tests avec rapport de couverture | `npm run test:coverage` |
| Build de production | `npm run build` |

En développement, les appels à `/api` sont renvoyés vers `http://localhost:8080` par `proxy.conf.json` : le back doit donc tourner sur ce port. En production, c'est nginx qui joue ce rôle (`nginx.conf`).

## Couverture

`npm run test:coverage` écrit le rapport HTML dans `coverage/index.html`. La commande échoue si la couverture passe sous 60 % (instructions, branches, fonctions ou lignes) : le seuil est défini dans `jest.config.js`.

## Organisation du code

- `src/app/core` : modèles, services d'appel à l'API, garde de route, intercepteur HTTP, libellés.
- `src/app/pages` : une page par dossier (connexion, inscription, projets, tableau de bord, détail d'une tâche).
- `src/testing` : données partagées par les tests.

## Qui est connecté ?

Le back n'utilise pas Spring Security. Après la connexion, l'utilisateur est gardé dans `localStorage` (clé `pmt.user`) et l'intercepteur ajoute son identifiant dans l'en-tête `X-User-Id` de chaque appel à l'API, hors `/api/auth/**`.
