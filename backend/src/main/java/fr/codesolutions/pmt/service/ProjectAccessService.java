package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.model.ProjectMember;
import fr.codesolutions.pmt.model.Role;
import fr.codesolutions.pmt.repository.ProjectMemberRepository;
import fr.codesolutions.pmt.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrôle des droits par projet. C'est le seul endroit où le tableau des
 * permissions de l'énoncé est appliqué :
 * <ul>
 *   <li>tout membre (administrateur, membre, observateur) peut consulter ;</li>
 *   <li>administrateur et membre peuvent créer, assigner et modifier les tâches ;</li>
 *   <li>seul l'administrateur gère les membres et les rôles.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final AuthService authService;

    public ProjectAccessService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                                AuthService authService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.authService = authService;
    }

    /**
     * Droit de consultation : l'appelant doit appartenir au projet, quel que soit son rôle.
     *
     * @return l'appartenance de l'appelant (projet, utilisateur et rôle)
     * @throws ApiException 401 si l'utilisateur est inconnu, 404 si le projet n'existe pas,
     *                      403 si l'utilisateur n'est pas membre du projet
     */
    public ProjectMember requireMember(Long projectId, Long userId) {
        authService.requireUser(userId);
        if (!projectRepository.existsById(projectId)) {
            throw ApiException.notFound("Projet introuvable");
        }
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> ApiException.forbidden("Vous n'êtes pas membre de ce projet"));
    }

    /**
     * Droit d'écriture sur les tâches : administrateur ou membre.
     *
     * @throws ApiException 403 pour un observateur
     */
    public ProjectMember requireTaskEditor(Long projectId, Long userId) {
        ProjectMember membership = requireMember(projectId, userId);
        if (!membership.getRole().canEditTasks()) {
            throw ApiException.forbidden("Un observateur ne peut pas créer ni modifier de tâche");
        }
        return membership;
    }

    /**
     * Droit de gestion des membres : administrateur uniquement.
     *
     * @throws ApiException 403 pour un membre ou un observateur
     */
    public ProjectMember requireAdmin(Long projectId, Long userId) {
        ProjectMember membership = requireMember(projectId, userId);
        if (membership.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Seul un administrateur du projet peut gérer les membres");
        }
        return membership;
    }
}
