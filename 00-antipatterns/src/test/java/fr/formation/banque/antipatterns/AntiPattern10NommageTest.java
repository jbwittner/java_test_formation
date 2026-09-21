package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 10 — nommage muet.
 *
 * <p><b>Symptôme</b> : {@code test1}, {@code testDebiter}, {@code testDebiter2},
 * {@code testDebiterKo}. Le nom décrit la <b>méthode appelée</b>, pas la
 * <b>règle vérifiée</b>.
 *
 * <p><b>Pourquoi c'est grave</b> : le rapport de test est le seul document qui
 * reste à jour. Quand une régression survient à 3 h du matin, on lit un nom de
 * test, pas le corps du test. {@code testDebiter2 FAILED} n'apprend rien ;
 * {@code devrait_refuser_le_debit_quand_il_depasse_le_solde FAILED} dit
 * immédiatement quelle règle métier est cassée.
 *
 * <p>Second symptôme, plus subtil : quand on n'arrive pas à nommer un test, c'est
 * presque toujours qu'il en contient plusieurs (cf. anti-pattern 7).
 *
 * <p><b>Correction</b> : {@code devrait_<comportement>_quand_<condition>} pour le
 * nom de méthode, et {@code @DisplayName} en français lisible pour le rapport.
 * Les deux, car le nom de méthode apparaît dans les traces et les logs de build
 * là où le {@code @DisplayName} n'apparaît pas toujours.
 */
@DisplayName("Anti-pattern 10 — nommage muet")
class AntiPattern10NommageTest {

    private static Compte compte() {
        return new Compte("FR76-A", Montant.euros("100.00"));
    }

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        void test1() {
            Compte compte = compte();
            compte.debiter(Montant.euros("30.00"));
            assertThat(compte.solde()).isEqualTo(Montant.euros("70.00"));
        }

        @Test
        void testDebiter2() {
            Compte compte = compte();
            compte.debiter(Montant.euros("100.00"));
            assertThat(compte.solde()).isEqualTo(Montant.euros("0.00"));
        }

        @Test
        void testDebiterKo() {
            // Lequel des trois vérifie la borne du solde ?
            // Impossible à dire sans lire le corps.
            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte().debiter(Montant.euros("100.01")));
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("diminue le solde quand le débit est couvert")
        void devrait_diminuer_le_solde_quand_le_debit_est_couvert() {
            Compte compte = compte();

            compte.debiter(Montant.euros("30.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("70.00"));
        }

        @Test
        @DisplayName("ramène le solde à zéro quand le débit égale le solde")
        void devrait_ramener_le_solde_a_zero_quand_le_debit_egale_le_solde() {
            Compte compte = compte();

            compte.debiter(Montant.euros("100.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("0.00"));
        }

        @Test
        @DisplayName("refuse le débit quand il dépasse le solde")
        void devrait_refuser_le_debit_quand_il_depasse_le_solde() {
            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte().debiter(Montant.euros("100.01")));
        }
    }
}
