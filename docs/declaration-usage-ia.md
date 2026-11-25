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

<!-- À compléter par moi avant le rendu : ce que j'ai décidé, relu, corrigé ou réécrit, et comment
     j'ai vérifié que l'application répond à l'énoncé. -->
