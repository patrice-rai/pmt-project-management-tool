# PMT - Project Management Tool

Plateforme de gestion de projet collaboratif pour les équipes de développement : projets, membres et
rôles, tâches, tableau de bord par statut, notifications par e-mail et historique des modifications.

| Partie | Technologie | Dossier |
|---|---|---|
| Front | Angular 20, tests Jest | [`frontend/`](frontend) |
| Back | Java 17, Spring Boot 3.3, Spring Data JPA, tests JUnit 5 / Mockito / Spring Boot Test | [`backend/`](backend) |
| Base de données | PostgreSQL 16 | [`database/init.sql`](database/init.sql) |
| Industrialisation | Docker, Docker Compose, GitHub Actions, Docker Hub | [`docker-compose.yml`](docker-compose.yml), [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |

## Sommaire

1. [Livrables de l'étude de cas](#1-livrables-de-létude-de-cas)
2. [Lancer l'application avec Docker](#2-lancer-lapplication-avec-docker)
3. [Procédure de déploiement](#3-procédure-de-déploiement)
4. [Chaîne CI/CD](#4-chaîne-cicd)
5. [Tests et couverture](#5-tests-et-couverture)
6. [Lancer en développement, sans Docker pour le code](#6-lancer-en-développement-sans-docker-pour-le-code)
7. [Documentation de l'API](#7-documentation-de-lapi)
8. [Choix techniques](#8-choix-techniques)

## 1. Livrables de l'étude de cas

| Livrable | Emplacement |
|---|---|
| Schéma de la base de données | [`docs/schema-base-de-donnees.md`](docs/schema-base-de-donnees.md) ([image SVG](docs/schema-base-de-donnees.svg)) |
| Script SQL (structure et données de test) | [`database/init.sql`](database/init.sql) |
| Rapport de couverture du back | [`docs/coverage/backend/index.html`](docs/coverage/backend/index.html), et artefact `couverture-backend` de la chaîne |
| Rapport de couverture du front | [`docs/coverage/frontend/index.html`](docs/coverage/frontend/index.html), et artefact `couverture-frontend` de la chaîne |
| Dockerfile du back | [`backend/Dockerfile`](backend/Dockerfile) |
| Dockerfile du front | [`frontend/Dockerfile`](frontend/Dockerfile) |
| Configuration de la chaîne CI/CD | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |
| Procédure de déploiement | ce fichier, [partie 3](#3-procédure-de-déploiement) |
| Images sur Docker Hub | `<compte-docker-hub>/pmt-backend` et `<compte-docker-hub>/pmt-frontend` |

## 2. Lancer l'application avec Docker

Prérequis : Docker avec Docker Compose (Docker Desktop convient). Rien d'autre à installer.

```bash
git clone <adresse-du-dépôt> pmt
cd pmt
docker compose up --build -d
```

La première construction prend quelques minutes. Ensuite :

| Service | Adresse |
|---|---|
| Application | http://localhost:4200 |
| Documentation de l'API (Swagger UI) | http://localhost:8080/swagger-ui.html |
| E-mails envoyés (Mailpit) | http://localhost:8025 |
| PostgreSQL | `localhost:5432`, base `pmt`, utilisateur `pmt` |

### Comptes de test

Le script SQL crée quatre comptes. Mot de passe de chacun : `password123`.

| E-mail | Projet « Refonte du site vitrine » | Projet « Application mobile » |
|---|---|---|
| `alice@pmt.local` | administratrice | observatrice |
| `bob@pmt.local` | membre | administrateur |
| `charlie@pmt.local` | observateur | non membre |
| `diane@pmt.local` | non membre | non membre |

Pour voir une notification : connectez-vous avec alice, assignez une tâche à bob, puis ouvrez
http://localhost:8025.

### Arrêter, repartir de zéro

```bash
docker compose down        # arrête les conteneurs, garde les données
docker compose down -v     # arrête et supprime les données : le script SQL sera rejoué au prochain démarrage
docker compose logs -f backend   # suivre les journaux du back
docker compose ps          # état des services
```

## 3. Procédure de déploiement

Le déploiement repose sur les deux images publiées sur Docker Hub par la chaîne CI/CD à chaque
modification de la branche `main` : la machine cible n'a besoin ni de Java, ni de Node, ni du code source.

### Sur la machine cible

1. Installer Docker et Docker Compose.
2. Récupérer trois fichiers du dépôt : `docker-compose.yml`, `database/init.sql` (au même chemin relatif)
   et `.env.example`.
3. Copier `.env.example` en `.env` et renseigner :

   ```dotenv
   DOCKERHUB_USERNAME=<compte-docker-hub>
   IMAGE_TAG=latest
   POSTGRES_PASSWORD=<un mot de passe choisi>
   ```

   `IMAGE_TAG` peut aussi valoir l'empreinte complète d'un commit, pour déployer une version précise.
4. Télécharger les images puis démarrer, sans rien reconstruire :

   ```bash
   docker compose pull backend frontend
   docker compose up -d --no-build
   ```

5. Vérifier :

   ```bash
   docker compose ps                                 # les 4 services sont « running », db est « healthy »
   curl -s http://localhost:8080/v3/api-docs | head -c 80    # le back répond
   curl -sI http://localhost:4200 | head -n 1        # le front répond : HTTP/1.1 200 OK
   ```

### Mettre à jour vers une nouvelle version

```bash
docker compose pull backend frontend
docker compose up -d --no-build
```

Les données sont conservées dans le volume `pmt-db-data`.

### Revenir à une version précédente

Mettre dans `.env` l'empreinte du commit voulu (`IMAGE_TAG=<sha>`), puis rejouer les deux commandes
ci-dessus.

### Points d'attention

- Le script `database/init.sql` n'est joué par PostgreSQL qu'au **premier** démarrage, quand le volume est
  vide. Il contient des données de test : pour une vraie mise en production, retirer la partie
  « Données de test » du script avant le premier démarrage.
- Le back vérifie au démarrage que la structure de la base correspond à ses entités
  (`spring.jpa.hibernate.ddl-auto=validate`) et s'arrête si ce n'est pas le cas.
- Le service `mailpit` est un faux serveur de messagerie. Pour envoyer de vrais e-mails, donner au back
  les variables `MAIL_HOST` et `MAIL_PORT` d'un serveur SMTP réel.
- Les ports publiés (4200, 8080, 8025, 5432) se changent dans `docker-compose.yml`.

### Variables d'environnement du back

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `SPRING_DATASOURCE_URL` | adresse JDBC de la base | `jdbc:postgresql://localhost:5432/pmt` |
| `SPRING_DATASOURCE_USERNAME` | utilisateur de la base | `pmt` |
| `SPRING_DATASOURCE_PASSWORD` | mot de passe de la base | `pmt` |
| `MAIL_HOST` / `MAIL_PORT` | serveur SMTP | `localhost` / `1025` |
| `MAIL_FROM` | expéditeur des notifications | `no-reply@pmt.local` |

## 4. Chaîne CI/CD

Fichier : [`.github/workflows/ci.yml`](.github/workflows/ci.yml). Elle se déclenche à chaque `push` sur
`main` et à chaque pull request.

| Étape | Ce qu'elle fait | Elle échoue si |
|---|---|---|
| `backend` | `./mvnw -B verify` : compilation, tests unitaires et d'intégration, rapport JaCoCo | un test échoue, ou la couverture des instructions ou des branches est sous 60 % |
| `frontend` | `npm ci`, `npm run test:coverage`, `npm run build` | un test échoue, la couverture est sous 60 %, ou le build échoue |
| `docker` | construit les deux images et les pousse sur Docker Hub (`latest` et empreinte du commit) | une des deux étapes précédentes a échoué (elle n'est alors pas lancée) |

L'étape `docker` ne s'exécute que sur `main`, jamais sur une pull request. Les deux rapports de
couverture sont joints à chaque exécution (artefacts `couverture-backend` et `couverture-frontend`).

### Configuration à faire une fois

Dans GitHub, **Settings > Secrets and variables > Actions > New repository secret** :

| Secret | Valeur |
|---|---|
| `DOCKERHUB_USERNAME` | nom du compte Docker Hub |
| `DOCKERHUB_TOKEN` | jeton créé dans Docker Hub (**Account settings > Personal access tokens**, droits lecture et écriture) |

Les identifiants ne figurent jamais dans le dépôt.

## 5. Tests et couverture

| | Back | Front |
|---|---|---|
| Commande | `cd backend && ./mvnw verify` | `cd frontend && npm ci && npm run test:coverage` |
| Tests | 80 | 86 |
| Instructions | 98,8 % | 100 % |
| Branches | 98,1 % | 100 % |
| Rapport généré dans | `backend/target/site/jacoco/index.html` | `frontend/coverage/index.html` |
| Seuil de 60 % déclaré dans | `backend/pom.xml` (JaCoCo, règle `check`) | `frontend/jest.config.js` (`coverageThreshold`) |

Une copie des deux rapports est versionnée dans [`docs/coverage/`](docs/coverage).

**Back.** Les tests unitaires (`backend/src/test/java/.../service`) isolent chaque service avec Mockito.
Les tests d'intégration (`.../integration`) démarrent toute l'application avec Spring Boot Test et
MockMvc, du contrôleur jusqu'à la base. Cette base est une H2 en mode PostgreSQL, créée par le vrai script
`database/init.sql` : les tests vérifient donc aussi que le script et les entités JPA concordent, et que le
mot de passe des comptes de test fonctionne. Seul l'envoi d'e-mails est simulé. Chaque case « non » du
tableau des droits de l'énoncé a son test.

**Front.** Les services sont testés avec `HttpTestingController`, la garde et l'intercepteur à part, et
chaque page avec ses cas d'erreur et ses variantes selon le rôle (administrateur, membre, observateur).
Les tests du tableau de bord et du détail d'une tâche assemblent composant, services réels, intercepteur
et routeur.

## 6. Lancer en développement, sans Docker pour le code

Prérequis : Java 17 ou plus, Node.js 22 ou plus, Docker pour la base et la messagerie.

```bash
# 1. Base de données et faux serveur de messagerie
docker compose up -d db mailpit

# 2. Back, sur http://localhost:8080
cd backend
./mvnw spring-boot:run        # sous Windows : mvnw.cmd spring-boot:run

# 3. Front, sur http://localhost:4200 (dans un autre terminal)
cd frontend
npm ci
npm start
```

En développement, le serveur Angular renvoie les appels `/api` vers `http://localhost:8080`
(`frontend/proxy.conf.json`). Dans l'image Docker, c'est nginx qui s'en charge
(`frontend/nginx.conf`) : le navigateur ne parle qu'à une seule origine, il n'y a donc pas de
configuration CORS.

## 7. Documentation de l'API

L'API est décrite avec OpenAPI (springdoc) : description de chaque route, paramètres, exemples de corps
et réponses possibles.

- Swagger UI : http://localhost:8080/swagger-ui.html
- Description JSON : http://localhost:8080/v3/api-docs

| Méthode | Route | Rôle requis sur le projet | Story |
|---|---|---|---|
| `POST` | `/api/auth/register` | aucun | 1 |
| `POST` | `/api/auth/login` | aucun | 2 |
| `GET` | `/api/projects` | utilisateur connecté | 3 |
| `POST` | `/api/projects` | utilisateur connecté (devient administrateur) | 3 |
| `GET` | `/api/projects/{id}` | tout rôle | 3 |
| `GET` | `/api/projects/{id}/members` | tout rôle | 4 |
| `POST` | `/api/projects/{id}/members` | administrateur | 4 |
| `PATCH` | `/api/projects/{id}/members/{userId}` | administrateur | 5 |
| `POST` | `/api/projects/{id}/tasks` | administrateur ou membre | 6 |
| `PATCH` | `/api/tasks/{id}/assignee` | administrateur ou membre | 7, 11 |
| `PUT` | `/api/tasks/{id}` | administrateur ou membre | 8 |
| `GET` | `/api/tasks/{id}` | tout rôle | 9 |
| `GET` | `/api/projects/{id}/tasks?status=` | tout rôle | 10 |
| `GET` | `/api/tasks/{id}/history` | tout rôle | 12 |

Exemple :

```bash
# Connexion : renvoie {"id":1,"username":"alice","email":"alice@pmt.local"}
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@pmt.local","password":"password123"}'

# Tableau de bord du projet 1, colonne « en cours »
curl -s "http://localhost:8080/api/projects/1/tasks?status=IN_PROGRESS" -H "X-User-Id: 1"
```

Toutes les erreurs ont la même forme :

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Un observateur ne peut pas créer ni modifier de tâche",
  "timestamp": "2026-01-21T09:30:00",
  "details": {}
}
```

## 8. Choix techniques

**Qui est connecté ?** L'énoncé précise que la sécurisation du back n'est pas demandée et que Spring
Security n'est pas obligatoire. Après la connexion, le front garde l'utilisateur dans le `localStorage`
et un intercepteur HTTP ajoute son identifiant dans l'en-tête `X-User-Id` de chaque requête. Le back s'en
sert pour retrouver l'utilisateur et contrôler son rôle. C'est une simplification assumée : l'en-tête
n'est pas une preuve d'identité, et une mise en production demanderait une vraie authentification (jeton
signé ou session). Les mots de passe, eux, ne sont jamais stockés en clair : seule leur empreinte BCrypt
est enregistrée.

**Où sont contrôlés les droits ?** Dans la couche de service, par une seule classe,
`ProjectAccessService`, qui applique le tableau des permissions de l'énoncé. Le front masque les boutons
interdits, mais c'est le back qui fait foi : il répond `403` à toute action non autorisée.

**Le rôle dépend du projet.** Il est porté par la table d'association `project_members`. Un projet garde
toujours au moins un administrateur : le dernier ne peut pas être rétrogradé.

**Inviter un membre.** L'adresse e-mail saisie doit correspondre à un compte existant ; sinon l'API répond
`404`.

**Historique.** Toute écriture passe par une seule méthode, `TaskHistoryService.recordChange`, qui
n'enregistre une ligne que si la valeur du champ a réellement changé.

**Notifications.** L'e-mail part au moment où une tâche reçoit un nouvel assigné (à la création, à la mise
à jour ou à l'assignation). Si le serveur de messagerie est en panne, l'erreur est journalisée et
l'assignation est conservée.

### Organisation du code

```
backend/src/main/java/fr/codesolutions/pmt/
  model/        entités JPA et énumérations (Role, Priority, TaskStatus)
  repository/   interfaces Spring Data
  service/      règles métier, contrôle des droits, historique, notifications
  controller/   contrôleurs REST documentés avec OpenAPI
  dto/          objets échangés avec le front, avec leurs règles de validation
  exception/    erreurs métier et traduction en réponses JSON
  config/       chiffrement des mots de passe, en-tête de la documentation

frontend/src/app/
  core/         modèles, services d'appel à l'API, garde de route, intercepteur, libellés
  pages/        connexion, inscription, projets, tableau de bord, détail d'une tâche
```
