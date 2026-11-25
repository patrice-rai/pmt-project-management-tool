# Déclaration d'usage de l'IA

Étude de cas PMT (Project Management Tool) - Patrice RAÏ

## Outil utilisé

Claude Code (Anthropic), assistant de programmation en ligne de commande, modèle Claude.

## Usage

Je me suis appuyé sur l'aide de l'IA pour écrire le code de cette étude de cas, à partir de l'énoncé,
de la feuille de travail et de mes consignes. Cette aide a porté sur :

- le schéma de la base de données et le script SQL (`docs/`, `database/init.sql`) ;
- le code du back Spring Boot et ses tests (`backend/`) ;
- le code du front Angular et ses tests (`frontend/`) ;
- les deux Dockerfile, le fichier Docker Compose et la chaîne GitHub Actions ;
- le `README.md` et la documentation de l'API.

L'assistant a aussi exécuté les vérifications : tests du back (`mvnw verify`) et du front
(`npm run test:coverage`) avec contrôle des seuils de couverture, build de production du front,
construction des images et lancement de l'ensemble avec Docker Compose.

## Ma part

- J'ai fourni l'énoncé, la feuille de travail et le plan de route, et piloté le travail de l'assistant.
- J'ai choisi GitHub et GitHub Actions pour le dépôt et la chaîne, et Docker Hub (compte `hybripat`)
  pour les images.
- J'ai créé le jeton d'accès Docker Hub et configuré le secret du dépôt moi-même : l'assistant n'a
  jamais eu accès à ce jeton.
- J'ai décidé de la publication du dépôt et de la formulation de cette déclaration.
