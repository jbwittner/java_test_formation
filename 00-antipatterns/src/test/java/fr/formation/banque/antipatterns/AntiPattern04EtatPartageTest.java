package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * ANTI-PATTERN 4 — état partagé entre les tests.
 *
 * <p><b>Symptôme</b> : un champ {@code static}, ou un champ d'instance modifié
 * par plusieurs tests, ou une base de données jamais nettoyée entre deux tests.
 *
 * <p><b>Pourquoi c'est grave</b> : les tests ne sont plus indépendants. Ils
 * passent dans l'ordre alphabétique et échouent en parallèle ; ils passent en
 * suite complète et échouent quand on lance un seul test depuis l'IDE. Le
 * diagnostic coûte des heures parce que le test qui échoue n'est pas celui qui
 * est fautif.
 *
 * <p><b>Correction</b> : chaque test construit ses propres données, dans un
 * {@code @BeforeEach} ou directement dans le bloc Arrange. JUnit crée d'ailleurs
 * une <b>nouvelle instance</b> de la classe pour chaque méthode : un champ
 * d'instance réinitialisé dans {@code @BeforeEach} est sûr, un champ
 * {@code static} ne l'est jamais.
 *
 * <p>Cette règle est encore plus critique au chapitre 02 : une base de données
 * partagée entre tests reproduit exactement ce problème, en plus difficile à voir.
 */
@DisplayName("Anti-pattern 4 — état partagé entre les tests")
class AntiPattern04EtatPartageTest {

    @Nested
    @DisplayName("✘ MAUVAIS")
    @TestMethodOrder(MethodOrderer.MethodName.class)
    @Disabled("""
            Volontairement desactive : ces deux tests ne passent que dans
            l'ordre alphabetique. Retirer le @Disabled, puis lancer
            'aRetraitInitial' SEUL depuis l'IDE : il passe. Lancer ensuite
            'bSoldeRestant' SEUL : il echoue. Le test fautif n'est pas celui
            qui echoue -- c'est ce qui rend ce bug si couteux a diagnostiquer.""")
    class Mauvais {

        // Partagé par toutes les méthodes, jamais réinitialisé.
        private static final Compte COMPTE =
                new Compte("FR76-PARTAGE", Montant.euros("1000.00"));

        @Test
        @DisplayName("retire 400 EUR")
        void aRetraitInitial() {
            COMPTE.debiter(Montant.euros("400.00"));

            assertThat(COMPTE.solde()).isEqualTo(Montant.euros("600.00"));
        }

        @Test
        @DisplayName("suppose que le test précédent a déjà retiré 400 EUR")
        void bSoldeRestant() {
            // Dépendance implicite au test précédent : le « 600 » ne vient pas
            // de ce test, il vient d'ailleurs.
            assertThat(COMPTE.solde()).isEqualTo(Montant.euros("600.00"));
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        private Compte compte;

        @BeforeEach
        void preparerCompte() {
            // Réinitialisé avant CHAQUE test : aucun ordre ne peut les faire échouer.
            compte = new Compte("FR76-ISOLE", Montant.euros("1000.00"));
        }

        @Test
        @DisplayName("diminue le solde après un retrait")
        void devrait_ramener_le_solde_a_600_quand_on_retire_400() {
            compte.debiter(Montant.euros("400.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("600.00"));
        }

        @Test
        @DisplayName("part toujours d'un solde de 1000 EUR")
        void devrait_partir_d_un_solde_neuf_quel_que_soit_l_ordre_d_execution() {
            assertThat(compte.solde()).isEqualTo(Montant.euros("1000.00"));
        }
    }
}
