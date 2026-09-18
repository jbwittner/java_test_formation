package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.TypeCompte;
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
 * {@code devrait_refuser_le_debit_quand_il_depasse_le_decouvert FAILED} dit
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
        return new Compte("FR76-A", TypeCompte.STANDARD,
                Montant.euros("100.00"), Montant.euros("50.00"));
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
            compte.debiter(Montant.euros("120.00"));
            assertThat(compte.solde()).isEqualTo(Montant.euros("-20.00"));
        }

        @Test
        void testDebiterKo() {
            // Lequel des trois vérifie la limite du découvert ?
            // Impossible à dire sans lire le corps.
            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte().debiter(Montant.euros("150.01")));
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
        @DisplayName("passe le solde en négatif quand le découvert absorbe le débit")
        void devrait_passer_le_solde_en_negatif_quand_le_decouvert_absorbe_le_debit() {
            Compte compte = compte();

            compte.debiter(Montant.euros("120.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("-20.00"));
        }

        @Test
        @DisplayName("refuse le débit quand il dépasse solde + découvert")
        void devrait_refuser_le_debit_quand_il_depasse_le_decouvert() {
            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte().debiter(Montant.euros("150.01")));
        }
    }
}
