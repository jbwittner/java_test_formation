package fr.formation.banque.unitaire.corrige;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import fr.formation.banque.domaine.GrilleFrais;
import fr.formation.banque.domaine.Montant;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * CORRIGÉ — exercice 1 : tests paramétrés sur un barème.
 *
 * <p>Ce que le corrigé illustre :
 * <ul>
 *   <li>{@code @CsvSource} : une ligne par cas métier. Le barème complet tient
 *       dans huit lignes lisibles — écrire huit {@code @Test} presque identiques
 *       aurait masqué la structure du barème.</li>
 *   <li>Les <b>bornes de paliers</b> sont testées des deux côtés (1000 / 1000.01,
 *       10000 / 10000.01). C'est là que vivent les bugs de barème.</li>
 *   <li>{@code name = ...} rend le rapport de test lisible : chaque cas apparaît
 *       avec ses valeurs, et un échec désigne immédiatement le palier fautif.</li>
 *   <li>{@code @MethodSource} quand les cas ne se réduisent pas à des chaînes.</li>
 *   <li>Un {@code @Test} classique reste le bon outil pour les cas d'erreur :
 *       ils ne partagent pas la forme des cas nominaux.</li>
 * </ul>
 */
@DisplayName("GrilleFrais — barème de frais par paliers")
class GrilleFraisCorrigeTest {

    @ParameterizedTest(name = "{0} EUR -> {1} EUR de frais")
    @CsvSource({
            "0.01,     1.00",    // borne basse : le plus petit montant possible
            "500.00,   1.00",    // milieu du palier bas
            "1000.00,  1.00",    // borne HAUTE INCLUSE du palier bas
            "1000.01,  1.00",    // premier montant du palier intermédiaire : 0,1 % = 1,00001 -> 1,00
            "5000.00,  5.00",    // milieu du palier intermédiaire
            "10000.00, 10.00",   // borne HAUTE INCLUSE du palier intermédiaire
            "10000.01, 15.00",   // premier montant du palier haut : bascule sur le forfait
            "999999.00, 15.00"   // palier haut : le forfait ne dépend plus du montant
    })
    @DisplayName("applique le forfait ou le taux du palier")
    void devrait_appliquer_le_bareme_selon_le_palier(String montant, String fraisAttendus) {
        Montant frais = GrilleFrais.calculer(Montant.euros(montant));

        assertThat(frais).isEqualTo(Montant.euros(fraisAttendus));
    }

    /**
     * {@code @MethodSource} quand les cas ne se réduisent pas à des chaînes :
     * ici on veut manipuler de vrais {@link Montant} et nommer chaque cas.
     */
    static Stream<Arguments> casDeProportionnalite() {
        return Stream.of(
                Arguments.of("palier intermédiaire bas", Montant.euros("2000.00"), Montant.euros("2.00")),
                Arguments.of("palier intermédiaire haut", Montant.euros("9000.00"), Montant.euros("9.00")),
                Arguments.of("arrondi au centime", Montant.euros("1234.56"), Montant.euros("1.23")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casDeProportionnalite")
    @DisplayName("applique 0,1 % dans le palier intermédiaire")
    void devrait_appliquer_le_taux_quand_le_montant_est_dans_le_palier_intermediaire(
            String cas, Montant montant, Montant fraisAttendus) {
        assertThat(GrilleFrais.calculer(montant)).isEqualTo(fraisAttendus);
    }

    @Test
    @DisplayName("refuse un montant nul")
    void devrait_refuser_quand_le_montant_est_nul() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GrilleFrais.calculer(Montant.euros("0.00")));
    }

    @Test
    @DisplayName("refuse un montant négatif")
    void devrait_refuser_quand_le_montant_est_negatif() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> GrilleFrais.calculer(Montant.euros("-10.00")));
    }
}
