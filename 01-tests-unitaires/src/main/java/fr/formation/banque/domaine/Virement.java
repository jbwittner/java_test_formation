package fr.formation.banque.domaine;

import java.time.LocalDate;

/**
 * Résultat d'un virement exécuté : un fait métier, immuable.
 *
 * @param reference        identifiant fonctionnel du virement
 * @param ibanSource       compte débité
 * @param ibanDestination  compte crédité
 * @param montant          montant transféré (hors frais)
 * @param frais            frais prélevés sur le compte source
 * @param dateDeValeur     date à laquelle l'opération prend effet
 */
public record Virement(
        String reference,
        String ibanSource,
        String ibanDestination,
        Montant montant,
        Montant frais,
        LocalDate dateDeValeur) {

    /** Montant réellement débité du compte source. */
    public Montant totalDebite() {
        return montant.plus(frais);
    }
}
