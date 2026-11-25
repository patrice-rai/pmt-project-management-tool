package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.dto.AuthDtos.LoginRequest;
import fr.codesolutions.pmt.dto.AuthDtos.RegisterRequest;
import fr.codesolutions.pmt.dto.ErrorResponse;
import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.exception.GlobalExceptionHandler;
import fr.codesolutions.pmt.model.Role;
import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.TaskHistory;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.TaskHistoryRepository;
import fr.codesolutions.pmt.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires des services de support : comptes, historique, e-mails et traduction des erreurs.
 */
class SupportServicesTest {

    @Nested
    class Comptes {

        private final UserRepository userRepository = mock(UserRepository.class);
        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        private final AuthService authService = new AuthService(userRepository, passwordEncoder);

        @Test
        void leMotDePasseEstEnregistreChiffre() {
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            authService.register(new RegisterRequest(" emma ", " Emma@PMT.local ", "motdepasse1"));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getUsername()).isEqualTo("emma");
            assertThat(saved.getValue().getEmail()).isEqualTo("emma@pmt.local");
            assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("motdepasse1");
            assertThat(passwordEncoder.matches("motdepasse1", saved.getValue().getPasswordHash())).isTrue();
        }

        @Test
        void unEmailDejaPrisEmpecheLInscription() {
            when(userRepository.existsByEmailIgnoreCase("emma@pmt.local")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(new RegisterRequest("emma", "emma@pmt.local", "motdepasse1")))
                    .isInstanceOfSatisfying(ApiException.class,
                            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
            verify(userRepository, never()).save(any());
        }

        @Test
        void laConnexionVerifieLeMotDePasse() {
            User emma = new User("emma", "emma@pmt.local", passwordEncoder.encode("motdepasse1"));
            when(userRepository.findByEmailIgnoreCase("emma@pmt.local")).thenReturn(Optional.of(emma));

            assertThat(authService.login(new LoginRequest("emma@pmt.local", "motdepasse1")).username())
                    .isEqualTo("emma");
            assertThatThrownBy(() -> authService.login(new LoginRequest("emma@pmt.local", "autre")))
                    .isInstanceOfSatisfying(ApiException.class,
                            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        void unIdentifiantInconnuEstRefuse() {
            when(userRepository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.requireUser(9L))
                    .isInstanceOfSatisfying(ApiException.class,
                            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }
    }

    @Nested
    class Historique {

        private final TaskHistoryRepository historyRepository = mock(TaskHistoryRepository.class);
        private final TaskHistoryService historyService = new TaskHistoryService(historyRepository);
        private final User alice = TestData.user(1, "alice");
        private final Task task = TestData.task(10, TestData.project(1), alice);

        @Test
        void unChangementEstEnregistreAvecAncienneEtNouvelleValeur() {
            boolean recorded = historyService.recordChange(task, alice, "dueDate",
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 15));

            assertThat(recorded).isTrue();
            ArgumentCaptor<TaskHistory> saved = ArgumentCaptor.forClass(TaskHistory.class);
            verify(historyRepository).save(saved.capture());
            assertThat(saved.getValue().getTask()).isSameAs(task);
            assertThat(saved.getValue().getChangedBy()).isSameAs(alice);
            assertThat(saved.getValue().getFieldName()).isEqualTo("dueDate");
            assertThat(saved.getValue().getOldValue()).isEqualTo("2026-02-01");
            assertThat(saved.getValue().getNewValue()).isEqualTo("2026-02-15");
            assertThat(saved.getValue().getChangedAt()).isNotNull();
        }

        @Test
        void uneValeurAjouteeOuRetireeEstUnChangement() {
            assertThat(historyService.recordChange(task, alice, "endDate", null, LocalDate.of(2026, 2, 1))).isTrue();
            assertThat(historyService.recordChange(task, alice, "assignee", "bob", null)).isTrue();
        }

        @Test
        void uneValeurIdentiqueNEstPasEnregistree() {
            assertThat(historyService.recordChange(task, alice, "status", "TODO", "TODO")).isFalse();
            assertThat(historyService.recordChange(task, alice, "endDate", null, null)).isFalse();

            verify(historyRepository, never()).save(any());
        }
    }

    @Nested
    class Notifications {

        private final JavaMailSender mailSender = mock(JavaMailSender.class);
        private final NotificationService notificationService =
                new NotificationService(mailSender, "no-reply@pmt.local");
        private final User alice = TestData.user(1, "alice");
        private final User bob = TestData.user(2, "bob");
        private final Task task = TestData.task(10, TestData.project(1), alice);

        @Test
        void lEmailEstAdresseALAssigne() {
            assertThat(notificationService.notifyTaskAssigned(task, bob, alice)).isTrue();

            ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(mail.capture());
            assertThat(mail.getValue().getFrom()).isEqualTo("no-reply@pmt.local");
            assertThat(mail.getValue().getTo()).containsExactly("bob@pmt.local");
            assertThat(mail.getValue().getSubject()).isEqualTo("[PMT] Nouvelle tâche assignée : Tâche 10");
            assertThat(mail.getValue().getText()).contains("Bonjour bob", "alice", "Projet 1", "MEDIUM", "2026-02-01");
        }

        @Test
        void unePanneDeMessagerieNeFaitPasEchouerLAssignation() {
            doThrow(new MailSendException("serveur injoignable")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThat(notificationService.notifyTaskAssigned(task, bob, alice)).isFalse();
        }
    }

    @Nested
    class Erreurs {

        private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

        @Test
        void uneErreurMetierGardeSonCodeEtSonMessage() {
            ResponseEntity<ErrorResponse> response = handler.handleApiException(ApiException.conflict("Doublon"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().error()).isEqualTo("Conflict");
            assertThat(response.getBody().message()).isEqualTo("Doublon");
            assertThat(response.getBody().details()).isEmpty();
            assertThat(response.getBody().timestamp()).isNotNull();
        }

        @Test
        void uneErreurInattendueEstMasquee() {
            ResponseEntity<ErrorResponse> response =
                    handler.handleUnexpected(new IllegalStateException("détail technique"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().message()).isEqualTo("Une erreur interne est survenue");
        }

        @Test
        void seulsAdministrateurEtMembrePeuventModifierLesTaches() {
            assertThat(Role.ADMIN.canEditTasks()).isTrue();
            assertThat(Role.MEMBER.canEditTasks()).isTrue();
            assertThat(Role.OBSERVER.canEditTasks()).isFalse();
        }
    }
}
