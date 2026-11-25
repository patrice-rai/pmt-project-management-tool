package fr.codesolutions.pmt.model;

/**
 * Rôle d'un utilisateur sur un projet donné.
 * Le rôle est porté par {@link ProjectMember} : il peut différer d'un projet à l'autre.
 */
public enum Role {
    /** Gère les membres et les rôles, en plus des droits du membre. */
    ADMIN,
    /** Crée, assigne et met à jour les tâches. */
    MEMBER,
    /** Consultation seule. */
    OBSERVER;

    /** Indique si le rôle autorise la création et la modification des tâches. */
    public boolean canEditTasks() {
        return this == ADMIN || this == MEMBER;
    }
}
