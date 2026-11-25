package fr.codesolutions.pmt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Beans techniques de l'application.
 */
@Configuration
public class AppConfig {

    /** Chiffrement des mots de passe : seule l'empreinte BCrypt est enregistrée. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** En-tête de la documentation OpenAPI, consultable sur /swagger-ui.html. */
    @Bean
    public OpenAPI pmtOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API PMT - Project Management Tool")
                .version("1.0.0")
                .contact(new Contact().name("Patrice RAÏ"))
                .description("""
                        API REST de la plateforme de gestion de projet collaboratif PMT.

                        **Identification** : la sécurisation du back n'est pas demandée par le cahier des charges. \
                        Après la connexion (`POST /api/auth/login`), le client envoie l'identifiant de l'utilisateur \
                        dans l'en-tête `X-User-Id` de chaque requête. Les droits (administrateur, membre, observateur) \
                        sont contrôlés pour chaque projet à partir de cet identifiant.

                        **Erreurs** : toutes les erreurs ont la même forme (`ErrorResponse`).
                        """));
    }
}
