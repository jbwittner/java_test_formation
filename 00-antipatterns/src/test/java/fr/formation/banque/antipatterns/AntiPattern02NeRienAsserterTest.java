package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.domaine.GrilleFrais;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.TypeCompte;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 2 — le test qui n'assère rien.
 *
 * <p><b>Symptôme</b> : {@code assertNotNull}, {@code assertThat(x).isNotNull()},
 * ou carrément aucune assertion. Le test ne vérifie qu'une chose : « ça n'a pas
 * lancé d'exception ».
 *
 * <p><b>Pourquoi c'est grave</b> : la couverture de code monte, le tableau de
 * bord passe au vert, et aucune régression ne sera jamais détectée. C'est le
 * pire des deux mondes — le coût d'un test, la valeur d'aucun.
 *
 * <p><b>Correction</b> : chaque test doit répondre à « quelle valeur exacte,
 * quel état exact ? ». Si on ne sait pas quoi asserter, c'est que le
 * comportement testé n'est pas encore clair.
 */
@DisplayName("Anti-pattern 2 — ne rien assérer")
class AntiPattern02NeRienAsserterTest {

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @DisplayName("vérifie seulement que le résultat n'est pas nul")
        void testCalculFrais() {
            Montant frais = GrilleFrais.calculer(Montant.euros("5000.00"), TypeCompte.STANDARD);

            // GrilleFrais ne renvoie jamais null : cette assertion est toujours vraie.
            // Elle resterait verte si le barème renvoyait 0, 1 000 000, ou la mauvaise devise.
            assertThat(frais).isNotNull();
        }

        @Test
        @DisplayName("appelle le code sans rien vérifier du tout")
        void testCalculFraisPremium() {
            // Aucune assertion : ce test ne détecte que les exceptions.
            // Il contribue pourtant à la couverture, ce qui le rend dangereux.
            GrilleFrais.calculer(Montant.euros("5000.00"), TypeCompte.PREMIUM);
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("assère la valeur exacte des frais")
        void devrait_appliquer_zero_virgule_un_pourcent_quand_le_montant_est_de_5000_euros() {
            Montant frais = GrilleFrais.calculer(Montant.euros("5000.00"), TypeCompte.STANDARD);

            assertThat(frais).isEqualTo(Montant.euros("5.00"));
        }

        @Test
        @DisplayName("assère l'exonération premium, devise comprise")
        void devrait_exonerer_quand_le_compte_est_premium() {
            Montant frais = GrilleFrais.calculer(Montant.euros("5000.00"), TypeCompte.PREMIUM);

            assertThat(frais).isEqualTo(Montant.euros("0.00"));
        }
    }
}
