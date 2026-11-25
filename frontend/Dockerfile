# Étape 1 : construction de l'application Angular
FROM node:22-alpine AS build
WORKDIR /app

# Les dépendances d'abord : cette couche reste en cache tant que les manifestes ne changent pas.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Étape 2 : image légère qui sert les fichiers statiques
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
