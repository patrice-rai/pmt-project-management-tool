package fr.codesolutions.pmt.service;

import fr.codesolutions.pmt.model.Task;
import fr.codesolutions.pmt.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Notifications par e-mail.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String sender;

    public NotificationService(JavaMailSender mailSender,
                               @Value("${pmt.mail.from:no-reply@pmt.local}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    /**
     * Prévient un utilisateur qu'une tâche vient de lui être assignée.
     * Une panne du serveur de messagerie ne doit pas faire échouer l'assignation :
     * l'erreur est journalisée et la méthode rend la main normalement.
     *
     * @param task       tâche assignée
     * @param assignee   destinataire de l'e-mail
     * @param assignedBy utilisateur qui a fait l'assignation
     * @return true si l'e-mail a été remis au serveur de messagerie
     */
    public boolean notifyTaskAssigned(Task task, User assignee, User assignedBy) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(assignee.getEmail());
        message.setSubject("[PMT] Nouvelle tâche assignée : " + task.getName());
        message.setText("""
                Bonjour %s,

                %s vous a assigné la tâche « %s » du projet « %s ».
                Priorité : %s
                Date d'échéance : %s

                Connectez-vous à PMT pour en voir le détail.
                """.formatted(assignee.getUsername(), assignedBy.getUsername(), task.getName(),
                task.getProject().getName(), task.getPriority(), task.getDueDate()));
        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            log.warn("E-mail d'assignation non envoyé à {} : {}", assignee.getEmail(), ex.getMessage());
            return false;
        }
    }
}
