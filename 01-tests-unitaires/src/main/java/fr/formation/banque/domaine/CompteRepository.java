package fr.formation.banque.domaine;

import java.util.Optional;

/**
 * Port de persistance, défini par le domaine et implémenté à l'extérieur
 * (chapitre 02 : adaptateur JPA / PostgreSQL).
 *
 * <p>C'est cette interface qui rend {@link ServiceVirement} testable unitairement :
 * en test on fournit un double, en production un adaptateur JPA. Un port est une
 * dépendance légitime à mocker — contrairement à {@link Montant} ou {@link Compte}.
 */
public interface CompteRepository {

    Optional<Compte> parIban(String iban);

    void enregistrer(Compte compte);
}
