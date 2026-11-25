# ---------------------------------------------------------------------------
# Image du back PMT (API Spring Boot), construite en deux étapes.
# Les tests ne sont pas rejoués ici : la chaîne CI les exécute avant de
# construire l'image (ils ont besoin du script database/init.sql).
# ---------------------------------------------------------------------------

# Étape 1 : compilation et empaquetage avec Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Les dépendances sont téléchargées dans une couche à part : elle reste en cache
# tant que le pom.xml ne change pas.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# Étape 2 : image d'exécution légère, avec le JRE seul
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# L'application ne tourne pas en root
RUN addgroup -S pmt && adduser -S pmt -G pmt
USER pmt

COPY --from=build /app/target/pmt-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
