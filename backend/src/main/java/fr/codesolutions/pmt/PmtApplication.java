package fr.codesolutions.pmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API PMT (Project Management Tool).
 */
@SpringBootApplication
public class PmtApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmtApplication.class, args);
    }
}
