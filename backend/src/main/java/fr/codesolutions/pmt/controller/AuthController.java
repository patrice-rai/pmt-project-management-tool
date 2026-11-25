package fr.codesolutions.pmt.controller;

import fr.codesolutions.pmt.dto.AuthDtos.LoginRequest;
import fr.codesolutions.pmt.dto.AuthDtos.RegisterRequest;
import fr.codesolutions.pmt.dto.AuthDtos.UserResponse;
import fr.codesolutions.pmt.dto.ErrorResponse;
import fr.codesolutions.pmt.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inscription et connexion (user stories 1 et 2). Ces routes ne demandent pas d'en-tête X-User-Id.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription et connexion")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte",
            description = "Inscrit un visiteur avec un nom d'utilisateur, une adresse e-mail et un mot de passe.")
    @ApiResponse(responseCode = "201", description = "Compte créé")
    @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ou nom d'utilisateur déjà utilisé",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter",
            description = "Vérifie l'e-mail et le mot de passe. L'identifiant renvoyé est à placer ensuite "
                    + "dans l'en-tête X-User-Id de chaque requête.")
    @ApiResponse(responseCode = "200", description = "Connexion réussie")
    @ApiResponse(responseCode = "401", description = "E-mail ou mot de passe incorrect",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
