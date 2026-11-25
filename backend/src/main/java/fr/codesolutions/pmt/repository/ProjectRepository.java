package fr.codesolutions.pmt.repository;

import fr.codesolutions.pmt.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/** Accès aux projets. */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
