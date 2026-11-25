package fr.codesolutions.pmt.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stories 1 et 2 : inscription et connexion. */
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER = "/api/auth/register";
    private static final String LOGIN = "/api/auth/login";

    @Test
    void inscriptionPuisConnexion() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "emma", "email": "Emma@PMT.local", "password": "motdepasse1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("emma"))
                .andExpect(jsonPath("$.email").value("emma@pmt.local"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "emma@pmt.local", "password": "motdepasse1"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("emma"));
    }

    @Test
    void inscriptionRefuseeSiEmailDejaUtilise() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "alice2", "email": "alice@pmt.local", "password": "motdepasse1"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Un compte existe déjà avec cette adresse e-mail"));
    }

    @Test
    void inscriptionRefuseeSiNomDejaPris() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "Alice", "email": "autre@pmt.local", "password": "motdepasse1"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ce nom d'utilisateur est déjà pris"));
    }

    @Test
    void inscriptionRefuseeSiDonneesInvalides() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "emma", "email": "pas-un-email", "password": "court"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").value("L'adresse e-mail n'est pas valide"))
                .andExpect(jsonPath("$.details.password").exists())
                .andExpect(jsonPath("$.details.username").doesNotExist());
    }

    @Test
    void requeteMalFormeeRenvoie400() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.APPLICATION_JSON).content("{ceci n'est pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La requête est mal formée"));
    }

    @Test
    void connexionAvecUnCompteDesDonneesDeTest() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "alice@pmt.local", "password": "password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ALICE))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void connexionRefuseeSiMotDePasseFaux() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "alice@pmt.local", "password": "mauvais"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Adresse e-mail ou mot de passe incorrect"));
    }

    @Test
    void connexionRefuseeSiEmailInconnu() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "inconnu@pmt.local", "password": "password123"}"""))
                .andExpect(status().isUnauthorized());
    }
}
