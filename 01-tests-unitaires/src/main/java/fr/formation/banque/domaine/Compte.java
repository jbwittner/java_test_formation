package fr.formation.banque.domaine;

import java.util.Objects;

/**
 * Entité du domaine : identifiée par son IBAN, porteuse des règles de débit/crédit.
 *
 * <p>Toute la logique « puis-je débiter ? » vit ici, pas dans un service et surtout
 * pas dans un contrôleur. C'est ce qui permet de la tester sans démarrer Spring.
 */
public class Compte {

    private final String iban;
    private Montant solde;

    public Compte(String iban, Montant solde) {
        this.iban = Objects.requireNonNull(iban, "iban");
        this.solde = Objects.requireNonNull(solde, "solde");
        if (solde.estNegatif()) {
            throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
        }
    }

    /**
     * Débite le compte.
     *
     * @throws IllegalArgumentException   si le montant n'est pas strictement positif
     * @throws SoldeInsuffisantException  si le débit dépasse le solde
     */
    public void debiter(Montant montant) {
        exigerMontantPositif(montant);
        Montant nouveauSolde = solde.moins(montant);
        if (nouveauSolde.estNegatif()) {
            throw new SoldeInsuffisantException(iban, montant, solde);
        }
        solde = nouveauSolde;
    }

    public void crediter(Montant montant) {
        exigerMontantPositif(montant);
        solde = solde.plus(montant);
    }

    private void exigerMontantPositif(Montant montant) {
        Objects.requireNonNull(montant, "montant");
        if (!montant.estStrictementPositif()) {
            throw new IllegalArgumentException("Le montant doit être strictement positif : " + montant);
        }
    }

    public String iban() {
        return iban;
    }

    public Montant solde() {
        return solde;
    }

    @Override
    public String toString() {
        return "Compte[" + iban + ", solde=" + solde + "]";
    }
}
