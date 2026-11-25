package fr.codesolutions.pmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Corps de réponse commun à toutes les erreurs de l'API.
 */
@Schema(description = "Erreur renvoyée par l'API")
public record ErrorResponse(
        @Schema(example = "403") int status,
        @Schema(example = "Forbidden") String error,
        @Schema(example = "Votre rôle sur ce projet ne permet pas cette action") String message,
        @Schema(example = "2026-01-21T09:30:00") LocalDateTime timestamp,
        @Schema(description = "Erreurs de validation par champ (vide sinon)") Map<String, String> details) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return of(status, message, Map.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> details) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now(), details);
    }
}
