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
    private final TypeCompte type;
    private Montant solde;
    private final Montant decouvertAutorise;

    public Compte(String iban, TypeCompte type, Montant solde, Montant decouvertAutorise) {
        this.iban = Objects.requireNonNull(iban, "iban");
        this.type = Objects.requireNonNull(type, "type");
        this.solde = Objects.requireNonNull(solde, "solde");
        this.decouvertAutorise = Objects.requireNonNull(decouvertAutorise, "decouvertAutorise");
        if (decouvertAutorise.estNegatif()) {
            throw new IllegalArgumentException("Le découvert autorisé ne peut pas être négatif");
        }
        if (solde.devise() != decouvertAutorise.devise()) {
            throw new DeviseIncompatibleException(solde.devise(), decouvertAutorise.devise());
        }
    }

    /** Raccourci de confort pour les tests et les cas simples : compte standard sans découvert. */
    public static Compte standard(String iban, Montant solde) {
        return new Compte(iban, TypeCompte.STANDARD, solde, Montant.zero(solde.devise()));
    }

    /**
     * Débite le compte.
     *
     * @throws IllegalArgumentException   si le montant n'est pas strictement positif
     * @throws SoldeInsuffisantException  si le débit dépasse le solde + le découvert autorisé
     */
    public void debiter(Montant montant) {
        exigerMontantPositif(montant);
        Montant nouveauSolde = solde.moins(montant);
        if (nouveauSolde.compareTo(decouvertAutorise.negatif()) < 0) {
            throw new SoldeInsuffisantException(iban, montant, montantDisponible());
        }
        solde = nouveauSolde;
    }

    public void crediter(Montant montant) {
        exigerMontantPositif(montant);
        solde = solde.plus(montant);
    }

    /** Solde + découvert autorisé : ce que le client peut réellement dépenser. */
    public Montant montantDisponible() {
        return solde.plus(decouvertAutorise);
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

    public TypeCompte type() {
        return type;
    }

    public Montant solde() {
        return solde;
    }

    public Montant decouvertAutorise() {
        return decouvertAutorise;
    }

    @Override
    public String toString() {
        return "Compte[" + iban + ", " + type + ", solde=" + solde + "]";
    }
}
