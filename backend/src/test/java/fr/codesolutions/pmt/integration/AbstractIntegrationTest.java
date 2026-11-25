package fr.codesolutions.pmt.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Socle des tests d'intégration : toute l'application est démarrée (contrôleurs, services,
 * accès aux données) sur une base H2 créée par database/init.sql. Seul l'envoi d'e-mails est simulé.
 * Chaque test s'exécute dans une transaction annulée à la fin : les données de test restent intactes.
 *
 * <p>Données de test : sur le projet 1, alice est administratrice, bob membre et charlie observateur ;
 * sur le projet 2, bob est administrateur et alice observatrice ; diane n'appartient à aucun projet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class AbstractIntegrationTest {

    protected static final long ALICE = 1;
    protected static final long BOB = 2;
    protected static final long CHARLIE = 3;
    protected static final long DIANE = 4;

    @Autowired
    protected MockMvc mockMvc;

    /** Simulé pour ne pas envoyer de vrais e-mails et pour vérifier les notifications. */
    @MockBean
    protected JavaMailSender mailSender;

    /** Ajoute à la requête l'en-tête d'identification et, s'il y en a un, le corps JSON. */
    protected MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request, String json) {
        request.header("X-User-Id", userId);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request;
    }

    protected MockHttpServletRequestBuilder as(long userId, MockHttpServletRequestBuilder request) {
        return as(userId, request, null);
    }
}
