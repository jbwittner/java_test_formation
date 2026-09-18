package fr.formation.banque.domaine;

/** Levée lorsqu'une opération mélange deux devises différentes. */
public class DeviseIncompatibleException extends RuntimeException {

    public DeviseIncompatibleException(Devise attendue, Devise recue) {
        super("Devise incompatible : attendue " + attendue + ", reçue " + recue);
    }
}
