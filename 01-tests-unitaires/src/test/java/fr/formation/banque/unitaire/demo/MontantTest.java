package fr.formation.banque.unitaire.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import fr.formation.banque.domaine.Devise;
import fr.formation.banque.domaine.DeviseIncompatibleException;
import fr.formation.banque.domaine.Montant;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DÉMO 1 — anatomie d'un test unitaire.
 *
 * <p>Points à observer pendant la démonstration :
 * <ol>
 *   <li><b>Structure AAA</b> : Arrange (préparer), Act (agir), Assert (vérifier).
 *       Trois blocs séparés par une ligne vide, dans cet ordre, toujours.</li>
 *   <li><b>Un comportement par test</b> : le nom de la méthode décrit une règle
 *       métier, pas une méthode Java. Si le nom contient « et », il y a deux tests.</li>
 *   <li><b>Nommage</b> : {@code devrait_<comportement>_quand_<condition>}.
 *       Le rapport de test se lit alors comme une spécification.</li>
 *   <li><b>AssertJ</b> plutôt que {@code assertEquals} : l'auto-complétion guide,
 *       et le message d'échec est explicite sans effort.</li>
 *   <li><b>Aucun mock</b> : {@link Montant} est un value object. Mocker un type
 *       qu'on peut instancier en une ligne, c'est mocker le code qu'on teste.</li>
 * </ol>
 *
 * <p>Coût d'exécution de cette classe : quelques millisecondes. C'est la
 * définition opérationnelle d'un test unitaire.
 */
@DisplayName("Montant — value object monétaire")
class MontantTest {

    @Test
    @DisplayName("additionne deux montants de même devise")
    void devrait_additionner_quand_les_devises_sont_identiques() {
        // Arrange
        Montant dix = Montant.euros("10.00");
        Montant cinq = Montant.euros("5.50");

        // Act
        Montant somme = dix.plus(cinq);

        // Assert
        assertThat(somme).isEqualTo(Montant.euros("15.50"));
    }

    @Test
    @DisplayName("refuse d'additionner deux devises différentes")
    void devrait_lever_DeviseIncompatibleException_quand_les_devises_different() {
        Montant euros = Montant.euros("10.00");
        Montant dollars = Montant.de("10.00", Devise.USD);

        // assertThatThrownBy capture l'exception ET permet d'asserter dessus.
        // On vérifie le TYPE et le MESSAGE : une exception mal typée ne doit pas
        // faire passer le test par accident.
        assertThatThrownBy(() -> euros.plus(dollars))
                .isInstanceOf(DeviseIncompatibleException.class)
                .hasMessageContaining("EUR")
                .hasMessageContaining("USD");
    }

    @Test
    @DisplayName("normalise l'échelle : 10 EUR égale 10.00 EUR")
    void devrait_normaliser_l_echelle_quand_le_montant_est_construit() {
        // Piège classique de BigDecimal : new BigDecimal("10").equals(new BigDecimal("10.00"))
        // vaut false. Le record normalise l'échelle pour que l'égalité métier
        // corresponde à l'égalité Java — ce qui rend toutes les assertions triviales.
        Montant sansDecimale = Montant.euros("10");
        Montant avecDecimales = Montant.euros("10.00");

        assertThat(sansDecimale).isEqualTo(avecDecimales);
        assertThat(sansDecimale.valeur()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("arrondit au centime supérieur à la construction")
    void devrait_arrondir_au_centime_quand_le_calcul_produit_plus_de_deux_decimales() {
        Montant montant = Montant.euros("1234.56");

        Montant frais = montant.multiplie(new BigDecimal("0.001"));

        // 1.23456 -> 1.23 (HALF_UP)
        assertThat(frais).isEqualTo(Montant.euros("1.23"));
    }

    @Test
    @DisplayName("compare deux montants de même devise")
    void devrait_comparer_quand_les_devises_sont_identiques() {
        Montant petit = Montant.euros("10.00");
        Montant grand = Montant.euros("20.00");

        // AssertJ connaît Comparable : isLessThan lit mieux que compareTo() < 0.
        assertThat(petit).isLessThan(grand);
        assertThat(grand).isGreaterThan(petit);
        assertThatNoException().isThrownBy(() -> petit.compareTo(grand));
    }

    @Test
    @DisplayName("expose un montant négatif après soustraction d'un montant plus grand")
    void devrait_etre_negatif_quand_la_soustraction_depasse_le_montant() {
        Montant solde = Montant.euros("10.00");

        Montant resultat = solde.moins(Montant.euros("30.00"));

        assertThat(resultat.estNegatif()).isTrue();
        assertThat(resultat).isEqualTo(Montant.euros("-20.00"));
    }
}
