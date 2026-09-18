package fr.formation.banque.unitaire.exercice;

import fr.formation.banque.domaine.GrilleFrais;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.TypeCompte;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EXERCICE 1 — tester un barème avec des tests paramétrés.
 *
 * <p><b>Barème implémenté par {@link GrilleFrais} :</b>
 * <ul>
 *   <li>compte PREMIUM : aucun frais, quel que soit le montant</li>
 *   <li>jusqu'à 1 000 EUR inclus : 1,00 EUR forfaitaire</li>
 *   <li>de 1 000 EUR exclu à 10 000 EUR inclus : 0,1 % du montant</li>
 *   <li>au-delà de 10 000 EUR : 15,00 EUR forfaitaire</li>
 *   <li>montant nul ou négatif : {@link IllegalArgumentException}</li>
 * </ul>
 *
 * <p><b>Consignes</b>
 * <ol>
 *   <li>Retirer le {@code @Disabled} de la classe.</li>
 *   <li>Remplacer le test d'amorçage par un {@code @ParameterizedTest} +
 *       {@code @CsvSource} couvrant les trois paliers.</li>
 *   <li>Ajouter explicitement les <b>bornes</b> : 1000 / 1000.01 et
 *       10000 / 10000.01. Chercher quelle borne est incluse, laquelle est exclue.</li>
 *   <li>Couvrir l'exonération PREMIUM.</li>
 *   <li>Couvrir les montants invalides (0 et négatif).</li>
 *   <li>Donner un {@code name = "..."} au test paramétré pour que le rapport
 *       nomme chaque cas.</li>
 * </ol>
 *
 * <p><b>Question de fin d'exercice</b> : combien de {@code @Test} auriez-vous
 * écrits sans {@code @ParameterizedTest} ? Lequel des deux rapports de test
 * fait mieux comprendre le barème à quelqu'un qui ne connaît pas le code ?
 *
 * <p>Corrigé : {@code fr.formation.banque.unitaire.corrige.GrilleFraisCorrigeTest}
 */
@Disabled("TODO exercice 1 — retirer cette annotation puis écrire les tests")
@DisplayName("EXERCICE 1 — GrilleFrais")
class GrilleFraisExerciceTest {

    @Test
    @DisplayName("amorçage : à remplacer par un test paramétré")
    void devrait_appliquer_le_forfait_quand_le_montant_est_dans_le_palier_bas() {
        // Arrange
        Montant montant = Montant.euros("500.00");

        // Act
        Montant frais = GrilleFrais.calculer(montant, TypeCompte.STANDARD);

        // Assert
        // TODO : asserter que les frais valent 1,00 EUR, puis généraliser
        //        cette méthode en @ParameterizedTest sur tout le barème.
    }

    // TODO : exonération des comptes PREMIUM

    // TODO : montant nul et montant négatif
}
