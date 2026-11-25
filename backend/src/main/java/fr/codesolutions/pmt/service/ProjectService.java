package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.dto.ProjectDtos.MemberRequest;
import fr.codesolutions.pmt.dto.ProjectDtos.MemberResponse;
import fr.codesolutions.pmt.dto.ProjectDtos.ProjectRequest;
import fr.codesolutions.pmt.dto.ProjectDtos.ProjectResponse;
import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.model.Project;
import fr.codesolutions.pmt.model.ProjectMember;
import fr.codesolutions.pmt.model.Role;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.ProjectMemberRepository;
import fr.codesolutions.pmt.repository.ProjectRepository;
import fr.codesolutions.pmt.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des projets, de leurs membres et des rôles.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final ProjectAccessService accessService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                          UserRepository userRepository, AuthService authService,
                          ProjectAccessService accessService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.accessService = accessService;
    }

    /** Crée un projet ; son créateur en devient automatiquement administrateur. */
    @Transactional
    public ProjectResponse create(Long userId, ProjectRequest request) {
        User creator = authService.requireUser(userId);
        Project project = projectRepository.save(
                new Project(request.name().trim(), request.description(), request.startDate()));
        ProjectMember membership = memberRepository.save(new ProjectMember(project, creator, Role.ADMIN));
        return ProjectResponse.from(membership);
    }

    /** Projets dont l'utilisateur est membre, avec son rôle sur chacun. */
    public List<ProjectResponse> listForUser(Long userId) {
        authService.requireUser(userId);
        return memberRepository.findByUserIdOrderByProjectNameAsc(userId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /** Détail d'un projet, réservé à ses membres. */
    public ProjectResponse get(Long projectId, Long userId) {
        return ProjectResponse.from(accessService.requireMember(projectId, userId));
    }

    /** Membres d'un projet, visibles par tous ses membres. */
    public List<MemberResponse> listMembers(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        return memberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /**
     * Invite un utilisateur dans le projet à partir de son adresse e-mail.
     * Choix de conception : l'adresse doit correspondre à un compte existant.
     *
     * @throws ApiException 403 si l'appelant n'est pas administrateur, 404 si aucun compte
     *                      n'a cette adresse, 409 si l'utilisateur est déjà membre
     */
    @Transactional
    public MemberResponse addMember(Long projectId, Long userId, MemberRequest request) {
        ProjectMember admin = accessService.requireAdmin(projectId, userId);
        User invited = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> ApiException.notFound("Aucun compte n'existe avec cette adresse e-mail"));
        if (memberRepository.existsByProjectIdAndUserId(projectId, invited.getId())) {
            throw ApiException.conflict("Cet utilisateur est déjà membre du projet");
        }
        ProjectMember membership = memberRepository.save(
                new ProjectMember(admin.getProject(), invited, request.role()));
        return MemberResponse.from(membership);
    }

    /**
     * Change le rôle d'un membre. Un projet garde toujours au moins un administrateur.
     *
     * @throws ApiException 403 si l'appelant n'est pas administrateur, 404 si la cible n'est pas
     *                      membre, 409 si l'on rétrograde le dernier administrateur
     */
    @Transactional
    public MemberResponse changeRole(Long projectId, Long userId, Long targetUserId, Role newRole) {
        accessService.requireAdmin(projectId, userId);
        ProjectMember target = memberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> ApiException.notFound("Cet utilisateur n'est pas membre du projet"));
        boolean demotesAdmin = target.getRole() == Role.ADMIN && newRole != Role.ADMIN;
        if (demotesAdmin && memberRepository.countByProjectIdAndRole(projectId, Role.ADMIN) <= 1) {
            throw ApiException.conflict("Le projet doit conserver au moins un administrateur");
        }
        target.setRole(newRole);
        return MemberResponse.from(target);
    }
}
