package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.model.Priority;
import fr.codesolutions.pmt.model.Project;
import fr.codesolutions.pmt.model.ProjectMember;
import fr.codesolutions.pmt.model.Role;
import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

/**
 * Fabrique d'entités pour les tests unitaires. Les identifiants, normalement
 * générés par la base, sont posés par réflexion.
 */
final class TestData {

    private TestData() {
    }

    static User user(long id, String username) {
        User user = new User(username, username + "@pmt.local", "empreinte");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    static Project project(long id) {
        Project project = new Project("Projet " + id, "Description", LocalDate.of(2026, 1, 12));
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    static ProjectMember member(Project project, User user, Role role) {
        return new ProjectMember(project, user, role);
    }

    static Task task(long id, Project project, User creator) {
        Task task = new Task(project, "Tâche " + id, "Description", LocalDate.of(2026, 2, 1), Priority.MEDIUM, creator);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
