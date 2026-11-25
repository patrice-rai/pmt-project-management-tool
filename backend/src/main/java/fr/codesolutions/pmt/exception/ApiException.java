package fr.codesolutions.pmt.exception;

import org.springframework.http.HttpStatus;

/**
 * Erreur métier associée à un code HTTP.
 * Les services lèvent ces exceptions ; {@link GlobalExceptionHandler} les traduit en réponse JSON.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 400 : la demande est incohérente (par exemple assigner un non-membre). */
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** 401 : l'appelant n'est pas identifié. */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    /** 403 : l'appelant est identifié mais son rôle ne suffit pas. */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    /** 404 : la ressource n'existe pas. */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    /** 409 : la demande entre en conflit avec l'état actuel (doublon, dernier administrateur). */
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
