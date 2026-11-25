package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.dto.AuthDtos.LoginRequest;
import fr.codesolutions.pmt.dto.AuthDtos.RegisterRequest;
import fr.codesolutions.pmt.dto.AuthDtos.UserResponse;
import fr.codesolutions.pmt.exception.ApiException;
import fr.codesolutions.pmt.model.User;
import fr.codesolutions.pmt.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscription, connexion et identification de l'appelant.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crée un compte. L'e-mail et le nom d'utilisateur doivent être libres.
     *
     * @throws ApiException 409 si l'e-mail ou le nom d'utilisateur est déjà pris
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cette adresse e-mail");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw ApiException.conflict("Ce nom d'utilisateur est déjà pris");
        }
        User user = userRepository.save(new User(username, email, passwordEncoder.encode(request.password())));
        return UserResponse.from(user);
    }

    /**
     * Vérifie les identifiants. Le message d'erreur est volontairement le même
     * que l'e-mail soit inconnu ou le mot de passe faux.
     *
     * @throws ApiException 401 si les identifiants sont incorrects
     */
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Adresse e-mail ou mot de passe incorrect"));
        return UserResponse.from(user);
    }

    /**
     * Retrouve l'utilisateur désigné par l'en-tête X-User-Id.
     *
     * @throws ApiException 401 si aucun compte ne porte cet identifiant
     */
    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("Utilisateur inconnu : reconnectez-vous"));
    }
}
