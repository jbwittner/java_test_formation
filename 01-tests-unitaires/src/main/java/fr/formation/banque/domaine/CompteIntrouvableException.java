package fr.formation.banque.domaine;

/** Levée lorsqu'aucun compte ne correspond à l'IBAN fourni. */
public class CompteIntrouvableException extends RuntimeException {

    public CompteIntrouvableException(String iban) {
        super("Aucun compte pour l'IBAN " + iban);
    }
}
