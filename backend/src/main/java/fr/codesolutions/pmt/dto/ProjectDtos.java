package fr.codesolutions.pmt.dto;

import fr.codesolutions.pmt.model.Project;
import fr.codesolutions.pmt.model.ProjectMember;
import fr.codesolutions.pmt.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Objets échangés pour les projets et leurs membres.
 */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    @Schema(description = "Données de création d'un projet")
    public record ProjectRequest(
            @Schema(example = "Refonte du site vitrine")
            @NotBlank(message = "Le nom du projet est obligatoire")
            @Size(max = 100, message = "Le nom du projet ne doit pas dépasser 100 caractères")
            String name,

            @Schema(example = "Nouvelle version du site institutionnel.")
            @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
            String description,

            @Schema(example = "2026-01-12")
            @NotNull(message = "La date de début est obligatoire")
            LocalDate startDate) {
    }

    @Schema(description = "Projet vu par l'utilisateur connecté")
    public record ProjectResponse(
            @Schema(example = "1") Long id,
            @Schema(example = "Refonte du site vitrine") String name,
            @Schema(example = "Nouvelle version du site institutionnel.") String description,
            @Schema(example = "2026-01-12") LocalDate startDate,
            @Schema(description = "Rôle de l'utilisateur connecté sur ce projet", example = "ADMIN") Role role) {

        public static ProjectResponse from(ProjectMember membership) {
            Project project = membership.getProject();
            return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                    project.getStartDate(), membership.getRole());
        }
    }

    @Schema(description = "Invitation d'un utilisateur existant dans un projet")
    public record MemberRequest(
            @Schema(description = "Adresse e-mail du compte à inviter", example = "diane@pmt.local")
            @NotBlank(message = "L'adresse e-mail est obligatoire")
            @Email(message = "L'adresse e-mail n'est pas valide")
            String email,

            @Schema(example = "MEMBER")
            @NotNull(message = "Le rôle est obligatoire")
            Role role) {
    }

    @Schema(description = "Nouveau rôle d'un membre")
    public record RoleRequest(
            @Schema(example = "OBSERVER")
            @NotNull(message = "Le rôle est obligatoire")
            Role role) {
    }

    @Schema(description = "Membre d'un projet")
    public record MemberResponse(
            @Schema(example = "2") Long userId,
            @Schema(example = "bob") String username,
            @Schema(example = "bob@pmt.local") String email,
            @Schema(example = "MEMBER") Role role) {

        public static MemberResponse from(ProjectMember membership) {
            return new MemberResponse(membership.getUser().getId(), membership.getUser().getUsername(),
                    membership.getUser().getEmail(), membership.getRole());
        }
    }
}
