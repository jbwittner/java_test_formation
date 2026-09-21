package fr.formation.banque.domaine;

/** Levée lorsqu'un débit dépasse le solde du compte. */
public class SoldeInsuffisantException extends RuntimeException {

    private final String iban;

    public SoldeInsuffisantException(String iban, Montant demande, Montant disponible) {
        super("Solde insuffisant sur " + iban + " : demandé " + demande + ", disponible " + disponible);
        this.iban = iban;
    }

    public String iban() {
        return iban;
    }
}
