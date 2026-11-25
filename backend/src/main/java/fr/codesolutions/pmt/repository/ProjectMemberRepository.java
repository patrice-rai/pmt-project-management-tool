package fr.codesolutions.pmt.repository;

import fr.codesolutions.pmt.model.ProjectMember;
import fr.codesolutions.pmt.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Accès aux appartenances utilisateur / projet. */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    /** Membres d'un projet, du plus ancien au plus récent. */
    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(Long projectId);

    /** Projets auxquels participe un utilisateur, par ordre alphabétique. */
    List<ProjectMember> findByUserIdOrderByProjectNameAsc(Long userId);

    long countByProjectIdAndRole(Long projectId, Role role);
}
