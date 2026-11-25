package fr.codesolutions.pmt.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stories 3, 4 et 5 : projets, invitations et rôles. */
class ProjectIntegrationTest extends AbstractIntegrationTest {

    private static final String PROJECTS = "/api/projects";
    private static final String MEMBERS_P1 = "/api/projects/1/members";

    // --- Identification -----------------------------------------------------------------

    @Test
    void sansEnTeteUtilisateurLaRequeteEstRefusee() throws Exception {
        mockMvc.perform(get(PROJECTS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification requise : en-tête X-User-Id manquant"));
    }

    @Test
    void utilisateurInconnuEstRefuse() throws Exception {
        mockMvc.perform(as(999, get(PROJECTS))).andExpect(status().isUnauthorized());
    }

    @Test
    void identifiantNonNumeriqueRenvoie400() throws Exception {
        mockMvc.perform(get(PROJECTS).header("X-User-Id", "abc")).andExpect(status().isBadRequest());
    }

    // --- Story 3 : projets --------------------------------------------------------------

    @Test
    void chacunVoitSesProjetsAvecSonRole() throws Exception {
        // Le rôle dépend du projet : alice est observatrice du projet 2 et administratrice du projet 1
        mockMvc.perform(as(ALICE, get(PROJECTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Application mobile"))
                .andExpect(jsonPath("$[0].role").value("OBSERVER"))
                .andExpect(jsonPath("$[1].name").value("Refonte du site vitrine"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));

        mockMvc.perform(as(DIANE, get(PROJECTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void leCreateurDUnProjetEnDevientAdministrateur() throws Exception {
        mockMvc.perform(as(DIANE, post(PROJECTS), """
                        {"name": "Intranet", "description": "Portail interne", "startDate": "2026-03-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Intranet"))
                .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(as(DIANE, get(PROJECTS)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void creationDeProjetRefuseeSiDonneesInvalides() throws Exception {
        mockMvc.perform(as(ALICE, post(PROJECTS), """
                        {"name": " ", "description": "Sans nom ni date"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("Le nom du projet est obligatoire"))
                .andExpect(jsonPath("$.details.startDate").value("La date de début est obligatoire"));
    }

    @Test
    void consultationDUnProjetReserveeAuxMembres() throws Exception {
        mockMvc.perform(as(CHARLIE, get(PROJECTS + "/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Refonte du site vitrine"))
                .andExpect(jsonPath("$.role").value("OBSERVER"));

        mockMvc.perform(as(DIANE, get(PROJECTS + "/1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Vous n'êtes pas membre de ce projet"));

        mockMvc.perform(as(ALICE, get(PROJECTS + "/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Projet introuvable"));
    }

    // --- Story 4 : inviter un membre ----------------------------------------------------

    @Test
    void tousLesRolesVoientLesMembres() throws Exception {
        mockMvc.perform(as(CHARLIE, get(MEMBERS_P1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[2].email").value("charlie@pmt.local"))
                .andExpect(jsonPath("$[2].role").value("OBSERVER"));
    }

    @Test
    void lAdministrateurInviteUnMembreParSonEmail() throws Exception {
        mockMvc.perform(as(ALICE, post(MEMBERS_P1), """
                        {"email": "Diane@pmt.local", "role": "MEMBER"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(DIANE))
                .andExpect(jsonPath("$.username").value("diane"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(as(DIANE, get(MEMBERS_P1))).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void unMembreNePeutPasInviter() throws Exception {
        mockMvc.perform(as(BOB, post(MEMBERS_P1), """
                        {"email": "diane@pmt.local", "role": "MEMBER"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seul un administrateur du projet peut gérer les membres"));
    }

    @Test
    void unObservateurNePeutPasInviter() throws Exception {
        mockMvc.perform(as(CHARLIE, post(MEMBERS_P1), """
                        {"email": "diane@pmt.local", "role": "OBSERVER"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitationRefuseeSiAucunCompteNAvecCetEmail() throws Exception {
        mockMvc.perform(as(ALICE, post(MEMBERS_P1), """
                        {"email": "personne@pmt.local", "role": "MEMBER"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun compte n'existe avec cette adresse e-mail"));
    }

    @Test
    void invitationRefuseeSiDejaMembre() throws Exception {
        mockMvc.perform(as(ALICE, post(MEMBERS_P1), """
                        {"email": "bob@pmt.local", "role": "OBSERVER"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cet utilisateur est déjà membre du projet"));
    }

    @Test
    void invitationRefuseeSiRoleInconnuOuAbsent() throws Exception {
        mockMvc.perform(as(ALICE, post(MEMBERS_P1), """
                        {"email": "diane@pmt.local", "role": "CHEF"}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(as(ALICE, post(MEMBERS_P1), """
                        {"email": "diane@pmt.local"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.role").value("Le rôle est obligatoire"));
    }

    // --- Story 5 : attribuer un rôle ----------------------------------------------------

    @Test
    void lAdministrateurChangeLeRoleDUnMembre() throws Exception {
        mockMvc.perform(as(ALICE, patch(MEMBERS_P1 + "/" + CHARLIE), """
                        {"role": "MEMBER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(CHARLIE))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        // Le nouveau rôle s'applique tout de suite : charlie peut maintenant créer une tâche
        mockMvc.perform(as(CHARLIE, post("/api/projects/1/tasks"), """
                        {"name": "Tâche de charlie", "dueDate": "2026-03-01", "priority": "LOW"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void unMembreOuUnObservateurNePeutPasChangerLesRoles() throws Exception {
        mockMvc.perform(as(BOB, patch(MEMBERS_P1 + "/" + CHARLIE), """
                        {"role": "ADMIN"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(CHARLIE, patch(MEMBERS_P1 + "/" + CHARLIE), """
                        {"role": "ADMIN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void leDernierAdministrateurNePeutPasEtreRetrograde() throws Exception {
        mockMvc.perform(as(ALICE, patch(MEMBERS_P1 + "/" + ALICE), """
                        {"role": "MEMBER"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Le projet doit conserver au moins un administrateur"));

        // Avec un second administrateur, la rétrogradation devient possible
        mockMvc.perform(as(ALICE, patch(MEMBERS_P1 + "/" + BOB), """
                        {"role": "ADMIN"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(as(ALICE, patch(MEMBERS_P1 + "/" + ALICE), """
                        {"role": "OBSERVER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OBSERVER"));
    }

    @Test
    void changementDeRoleRefusePourUnNonMembre() throws Exception {
        mockMvc.perform(as(ALICE, patch(MEMBERS_P1 + "/" + DIANE), """
                        {"role": "MEMBER"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cet utilisateur n'est pas membre du projet"));
    }
}
