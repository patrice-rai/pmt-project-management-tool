package fr.codesolutions.pmt.dto;

import fr.codesolutions.pmt.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Objets échangés pour l'inscription et la connexion.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "Données d'inscription d'un visiteur")
    public record RegisterRequest(
            @Schema(description = "Nom d'utilisateur, unique", example = "alice")
            @NotBlank(message = "Le nom d'utilisateur est obligatoire")
            @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
            String username,

            @Schema(description = "Adresse e-mail, unique", example = "alice@pmt.local")
            @NotBlank(message = "L'adresse e-mail est obligatoire")
            @Email(message = "L'adresse e-mail n'est pas valide")
            String email,

            @Schema(description = "Mot de passe, 8 caractères minimum", example = "password123")
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères")
            String password) {
    }

    @Schema(description = "Identifiants de connexion")
    public record LoginRequest(
            @Schema(example = "alice@pmt.local")
            @NotBlank(message = "L'adresse e-mail est obligatoire")
            String email,

            @Schema(example = "password123")
            @NotBlank(message = "Le mot de passe est obligatoire")
            String password) {
    }

    @Schema(description = "Utilisateur, sans son mot de passe")
    public record UserResponse(
            @Schema(example = "1") Long id,
            @Schema(example = "alice") String username,
            @Schema(example = "alice@pmt.local") String email) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
        }
    }
}
