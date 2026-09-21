package fr.formation.banque.domaine;

import java.math.BigDecimal;

/**
 * Barème de frais par paliers.
 *
 * <p>Fonction pure : mêmes entrées, même sortie, aucun effet de bord. C'est le
 * candidat idéal au {@code @ParameterizedTest} — une ligne de CSV par cas métier,
 * bornes de paliers comprises.
 *
 * <p>Barème :
 * <ul>
 *   <li>jusqu'à 1 000 inclus : 1,00 forfaitaire</li>
 *   <li>de 1 000 exclu à 10 000 inclus : 0,1 % du montant</li>
 *   <li>au-delà de 10 000 : 15,00 forfaitaire</li>
 * </ul>
 */
public final class GrilleFrais {

    private static final BigDecimal PALIER_BAS = new BigDecimal("1000");
    private static final BigDecimal PALIER_HAUT = new BigDecimal("10000");
    private static final BigDecimal TAUX_PALIER_INTERMEDIAIRE = new BigDecimal("0.001");
    private static final BigDecimal FORFAIT_PALIER_BAS = new BigDecimal("1.00");
    private static final BigDecimal FORFAIT_PALIER_HAUT = new BigDecimal("15.00");

    private GrilleFrais() {
    }

    public static Montant calculer(Montant montant) {
        if (!montant.estStrictementPositif()) {
            throw new IllegalArgumentException("Le montant doit être strictement positif : " + montant);
        }
        BigDecimal valeur = montant.valeur();
        if (valeur.compareTo(PALIER_BAS) <= 0) {
            return new Montant(FORFAIT_PALIER_BAS);
        }
        if (valeur.compareTo(PALIER_HAUT) <= 0) {
            return montant.multiplie(TAUX_PALIER_INTERMEDIAIRE);
        }
        return new Montant(FORFAIT_PALIER_HAUT);
    }
}
