package fr.codesolutions.pmt.repository;

import fr.codesolutions.pmt.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Accès aux comptes utilisateurs. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);
}
