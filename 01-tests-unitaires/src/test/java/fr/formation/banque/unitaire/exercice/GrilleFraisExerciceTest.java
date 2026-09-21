package fr.formation.banque.unitaire.exercice;

import fr.formation.banque.domaine.GrilleFrais;
import fr.formation.banque.domaine.Montant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EXERCICE 1 — couvrir un barème avec un seul test paramétré.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/01-grille-frais.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.unitaire.corrige.GrilleFraisCorrigeTest}
 */
@Disabled("Exercice 1 — voir docs/exercices/01-grille-frais.md, puis retirer cette annotation")
@DisplayName("EXERCICE 1 — GrilleFrais")
class GrilleFraisExerciceTest {

    @Test
    @DisplayName("amorçage : à remplacer par un test paramétré")
    void devrait_appliquer_le_forfait_quand_le_montant_est_dans_le_palier_bas() {
        // Arrange
        Montant montant = Montant.euros("500.00");

        // Act
        Montant frais = GrilleFrais.calculer(montant);

        // Assert
        // TODO : asserter que les frais valent 1,00 EUR, puis généraliser
        //        cette méthode en @ParameterizedTest sur tout le barème.
    }

    // TODO : les quatre bornes de paliers (1000 / 1000.01 et 10000 / 10000.01)

    // TODO : montant nul et montant négatif
}
