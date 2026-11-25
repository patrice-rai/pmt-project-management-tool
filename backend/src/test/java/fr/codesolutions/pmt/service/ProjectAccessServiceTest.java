package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.model.Project;
import fr.codesolutions.pmt.model.Role;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.ProjectMemberRepository;
import fr.codesolutions.pmt.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tableau des permissions de l'énoncé : une ligne par action, une colonne par rôle.
 */
@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    private static final long PROJECT_ID = 1;
    private static final long USER_ID = 7;

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private AuthService authService;
    @InjectMocks
    private ProjectAccessService accessService;

    private void givenMemberWithRole(Role role) {
        Project project = TestData.project(PROJECT_ID);
        User user = TestData.user(USER_ID, "utilisateur");
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(memberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(TestData.member(project, user, role)));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void tousLesRolesPeuventConsulter(Role role) {
        givenMemberWithRole(role);

        assertThat(accessService.requireMember(PROJECT_ID, USER_ID).getRole()).isEqualTo(role);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, true", "MEMBER, true", "OBSERVER, false"})
    void seulsAdministrateurEtMembreModifientLesTaches(Role role, boolean allowed) {
        givenMemberWithRole(role);

        if (allowed) {
            assertThat(accessService.requireTaskEditor(PROJECT_ID, USER_ID).getRole()).isEqualTo(role);
        } else {
            assertThatThrownBy(() -> accessService.requireTaskEditor(PROJECT_ID, USER_ID))
                    .isInstanceOfSatisfying(ApiException.class,
                            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, true", "MEMBER, false", "OBSERVER, false"})
    void seulLAdministrateurGereLesMembres(Role role, boolean allowed) {
        givenMemberWithRole(role);

        if (allowed) {
            assertThat(accessService.requireAdmin(PROJECT_ID, USER_ID).getRole()).isEqualTo(Role.ADMIN);
        } else {
            assertThatThrownBy(() -> accessService.requireAdmin(PROJECT_ID, USER_ID))
                    .isInstanceOfSatisfying(ApiException.class,
                            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Test
    void unNonMembreEstRefuse() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(memberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireMember(PROJECT_ID, USER_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void unProjetInexistantRenvoie404() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

        assertThatThrownBy(() -> accessService.requireMember(PROJECT_ID, USER_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void unUtilisateurInconnuEstRefuseAvantToutAutreControle() {
        when(authService.requireUser(USER_ID)).thenThrow(ApiException.unauthorized("Utilisateur inconnu"));

        assertThatThrownBy(() -> accessService.requireMember(PROJECT_ID, USER_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
