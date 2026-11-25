package fr.codesolutions.pmt.integration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stories 6 à 12 : tâches, tableau de bord, notifications et historique. */
class TaskIntegrationTest extends AbstractIntegrationTest {

    private static final String TASKS_P1 = "/api/projects/1/tasks";

    /** Tâche 3 du jeu de test (TODO, LOW, non assignée) avec ses valeurs d'origine. */
    private static final String TASK_3_UNCHANGED = """
            {"name": "Intégrer le formulaire de contact",
             "description": "Formulaire avec validation et envoi d'e-mail.",
             "dueDate": "2026-02-20", "endDate": null, "priority": "LOW", "status": "TODO", "assigneeId": null}""";

    // --- Story 10 : tableau de bord -----------------------------------------------------

    @Test
    void tousLesRolesVoientLeTableauDeBord() throws Exception {
        mockMvc.perform(as(CHARLIE, get(TASKS_P1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", hasItems("TODO", "IN_PROGRESS", "DONE")));
    }

    @Test
    void leTableauDeBordSeFiltreParStatut() throws Exception {
        mockMvc.perform(as(ALICE, get(TASKS_P1).param("status", "IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Maquetter la page d'accueil"))
                .andExpect(jsonPath("$[0].assigneeName").value("bob"));

        mockMvc.perform(as(ALICE, get(TASKS_P1).param("status", "INCONNU")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leTableauDeBordEstRefuseAuxNonMembres() throws Exception {
        mockMvc.perform(as(DIANE, get(TASKS_P1))).andExpect(status().isForbidden());
    }

    // --- Story 6 : créer une tâche ------------------------------------------------------

    @Test
    void unMembreCreeUneTacheEtLAssigneRecoitUnEmail() throws Exception {
        mockMvc.perform(as(BOB, post(TASKS_P1), """
                        {"name": "Rédiger les mentions légales", "description": "Avec le service juridique",
                         "dueDate": "2026-03-10", "priority": "HIGH", "assigneeId": 3}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-03-10"))
                .andExpect(jsonPath("$.assigneeName").value("charlie"))
                .andExpect(jsonPath("$.createdByName").value("bob"));

        // Story 11 : un observateur aussi est notifié quand une tâche lui est assignée
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("charlie@pmt.local");
        assertThat(mail.getValue().getSubject()).contains("Rédiger les mentions légales");
        assertThat(mail.getValue().getText()).contains("bob", "Refonte du site vitrine");
    }

    @Test
    void uneTacheSansAssigneNEnvoieAucunEmail() throws Exception {
        mockMvc.perform(as(ALICE, post(TASKS_P1), """
                        {"name": "Tâche libre", "description": "  ", "dueDate": "2026-03-10", "priority": "LOW"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.assigneeId").value(nullValue()));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void unObservateurNePeutPasCreerDeTache() throws Exception {
        mockMvc.perform(as(CHARLIE, post(TASKS_P1), """
                        {"name": "Interdit", "dueDate": "2026-03-10", "priority": "LOW"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Un observateur ne peut pas créer ni modifier de tâche"));
    }

    @Test
    void leRoleDependDuProjet() throws Exception {
        // alice administre le projet 1 mais n'est qu'observatrice du projet 2
        mockMvc.perform(as(ALICE, post("/api/projects/2/tasks"), """
                        {"name": "Interdit", "dueDate": "2026-03-10", "priority": "LOW"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void creationRefuseeSiDonneesInvalidesOuAssigneHorsProjet() throws Exception {
        mockMvc.perform(as(BOB, post(TASKS_P1), """
                        {"description": "Ni nom, ni date, ni priorité"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.dueDate").exists())
                .andExpect(jsonPath("$.details.priority").exists());

        mockMvc.perform(as(BOB, post(TASKS_P1), """
                        {"name": "Mal assignée", "dueDate": "2026-03-10", "priority": "LOW", "assigneeId": 4}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La tâche ne peut être assignée qu'à un membre du projet"));
    }

    // --- Story 9 : voir une tâche -------------------------------------------------------

    @Test
    void tousLesRolesVoientLeDetailDUneTache() throws Exception {
        mockMvc.perform(as(CHARLIE, get("/api/tasks/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rédiger le cahier des charges"))
                .andExpect(jsonPath("$.description").value("Recueillir les besoins auprès du service communication."))
                .andExpect(jsonPath("$.dueDate").value("2026-01-20"))
                .andExpect(jsonPath("$.endDate").value("2026-01-19"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.assigneeId").value(ALICE))
                .andExpect(jsonPath("$.createdByName").value("alice"));
    }

    @Test
    void leDetailEstRefuseAuxNonMembresEtIntrouvableSiInexistant() throws Exception {
        mockMvc.perform(as(DIANE, get("/api/tasks/1"))).andExpect(status().isForbidden());
        // charlie n'appartient pas au projet 2, dont dépend la tâche 4
        mockMvc.perform(as(CHARLIE, get("/api/tasks/4"))).andExpect(status().isForbidden());
        mockMvc.perform(as(ALICE, get("/api/tasks/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tâche introuvable"));
    }

    // --- Stories 8 et 12 : mettre à jour et tracer --------------------------------------

    @Test
    void laMiseAJourModifieLaTacheEtAlimenteLHistorique() throws Exception {
        mockMvc.perform(as(BOB, put("/api/tasks/3"), """
                        {"name": "Intégrer le formulaire de contact", "description": "Avec captcha",
                         "dueDate": "2026-02-25", "endDate": "2026-02-24", "priority": "HIGH",
                         "status": "DONE", "assigneeId": 1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Avec captcha"))
                .andExpect(jsonPath("$.dueDate").value("2026-02-25"))
                .andExpect(jsonPath("$.endDate").value("2026-02-24"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.assigneeName").value("alice"));

        // Six champs ont changé (le nom est resté le même) : six lignes d'historique
        mockMvc.perform(as(CHARLIE, get("/api/tasks/3/history")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].fieldName",
                        hasItems("description", "dueDate", "endDate", "priority", "status", "assignee")))
                .andExpect(jsonPath("$[*].changedByName", hasItems("bob")))
                .andExpect(jsonPath("$[?(@.fieldName == 'status')].oldValue").value("TODO"))
                .andExpect(jsonPath("$[?(@.fieldName == 'status')].newValue").value("DONE"))
                .andExpect(jsonPath("$[?(@.fieldName == 'assignee')].newValue").value("alice"));

        // Story 11 : la nouvelle assignée est notifiée
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("alice@pmt.local");
    }

    @Test
    void uneMiseAJourSansChangementNeCreePasDHistorique() throws Exception {
        mockMvc.perform(as(BOB, put("/api/tasks/3"), TASK_3_UNCHANGED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value("2026-01-08T15:00:00"));

        mockMvc.perform(as(BOB, get("/api/tasks/3/history"))).andExpect(jsonPath("$", hasSize(0)));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void unObservateurNePeutPasMettreAJourUneTache() throws Exception {
        mockMvc.perform(as(CHARLIE, put("/api/tasks/3"), TASK_3_UNCHANGED)).andExpect(status().isForbidden());
    }

    @Test
    void miseAJourRefuseeSiDonneesInvalidesOuTacheInconnue() throws Exception {
        mockMvc.perform(as(BOB, put("/api/tasks/3"), """
                        {"name": "", "dueDate": "2026-02-20", "priority": "LOW"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.status").value("Le statut est obligatoire"));

        mockMvc.perform(as(BOB, put("/api/tasks/999"), TASK_3_UNCHANGED)).andExpect(status().isNotFound());
    }

    // --- Stories 7 et 11 : assigner et notifier -----------------------------------------

    @Test
    void lAssignationNotifieLeMembreEtSeRetrouveDansLHistorique() throws Exception {
        mockMvc.perform(as(ALICE, patch("/api/tasks/3/assignee"), """
                        {"assigneeId": 2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(BOB))
                .andExpect(jsonPath("$.assigneeName").value("bob"));

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("bob@pmt.local");
        assertThat(mail.getValue().getSubject()).isEqualTo("[PMT] Nouvelle tâche assignée : Intégrer le formulaire de contact");

        mockMvc.perform(as(ALICE, get("/api/tasks/3/history")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fieldName").value("assignee"))
                .andExpect(jsonPath("$[0].oldValue").value(nullValue()))
                .andExpect(jsonPath("$[0].newValue").value("bob"))
                .andExpect(jsonPath("$[0].changedByName").value("alice"));
    }

    @Test
    void retirerLAssignationNEnvoiePasDEmail() throws Exception {
        mockMvc.perform(as(BOB, patch("/api/tasks/2/assignee"), """
                        {"assigneeId": null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value(nullValue()));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void reassignerAuMemeMembreNeNotifiePasDeNouveau() throws Exception {
        mockMvc.perform(as(ALICE, patch("/api/tasks/2/assignee"), """
                        {"assigneeId": 2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value("2026-01-21T09:30:00"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void assignationRefuseePourUnObservateurOuVersUnNonMembre() throws Exception {
        mockMvc.perform(as(CHARLIE, patch("/api/tasks/3/assignee"), """
                        {"assigneeId": 2}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(as(ALICE, patch("/api/tasks/3/assignee"), """
                        {"assigneeId": 4}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(as(ALICE, patch("/api/tasks/999/assignee"), """
                        {"assigneeId": 2}"""))
                .andExpect(status().isNotFound());
    }

    // --- Story 12 : historique ----------------------------------------------------------

    @Test
    void tousLesRolesVoientLHistoriqueDuPlusRecentAuPlusAncien() throws Exception {
        mockMvc.perform(as(CHARLIE, get("/api/tasks/1/history")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].fieldName").value("endDate"))
                .andExpect(jsonPath("$[0].newValue").value("2026-01-19"))
                .andExpect(jsonPath("$[1].newValue").value("DONE"))
                .andExpect(jsonPath("$[2].oldValue").value("TODO"))
                .andExpect(jsonPath("$[2].changedAt").value("2026-01-12T09:00:00"));
    }

    @Test
    void lHistoriqueEstRefuseAuxNonMembres() throws Exception {
        mockMvc.perform(as(DIANE, get("/api/tasks/1/history"))).andExpect(status().isForbidden());
        mockMvc.perform(as(ALICE, get("/api/tasks/999/history"))).andExpect(status().isNotFound());
    }
}
