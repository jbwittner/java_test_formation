package fr.formation.banque.unitaire.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DÉMO 2 — organiser une classe de test qui grossit.
 *
 * <p>Points à observer :
 * <ol>
 *   <li>{@code @Nested} regroupe par comportement métier. Le rapport devient une
 *       arborescence lisible, et chaque groupe peut avoir son propre contexte.</li>
 *   <li><b>Les cas limites sont les vrais tests</b> : ici, débiter EXACTEMENT le
 *       solde doit passer, un centime de plus doit échouer. Tester uniquement le
 *       cas nominal ne prouve presque rien.</li>
 *   <li>{@code assertSoftly} quand plusieurs assertions décrivent UN SEUL état
 *       final : on veut toutes les différences d'un coup, pas la première.</li>
 *   <li>{@code assertThatThrownBy} vérifie le TYPE <b>et</b> le MESSAGE : une
 *       exception mal typée ne doit pas faire passer le test par accident.</li>
 *   <li>Les tests sont indépendants : aucun champ mutable partagé entre méthodes.
 *       Chaque test construit son propre compte (cf. anti-pattern n°4).</li>
 * </ol>
 */
@DisplayName("Compte — règles de débit et de crédit")
class CompteTest {

    @Nested
    @DisplayName("Débit")
    class Debit {

        @Test
        @DisplayName("diminue le solde quand le montant est couvert")
        void devrait_diminuer_le_solde_quand_le_montant_est_couvert() {
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            compte.debiter(Montant.euros("30.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("70.00"));
        }

        @Test
        @DisplayName("accepte un débit égal au solde exact")
        void devrait_accepter_quand_le_debit_egale_le_solde() {
            // Cas limite « inclusif » : la borne elle-même doit passer.
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            compte.debiter(Montant.euros("100.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("0.00"));
        }

        @Test
        @DisplayName("refuse un débit d'un centime de plus que le solde")
        void devrait_lever_SoldeInsuffisantException_quand_le_debit_depasse_le_solde() {
            // Cas limite « exclusif » : le premier montant refusé, pas un montant
            // absurde comme 1 000 000. Un test de borne vaut dix tests génériques.
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte.debiter(Montant.euros("100.01")))
                    .satisfies(erreur -> assertThat(erreur.iban()).isEqualTo("FR76-SOURCE"));
        }

        @Test
        @DisplayName("nomme le compte et les montants dans le message d'erreur")
        void devrait_decrire_le_probleme_quand_le_debit_est_refuse() {
            // Le message d'exception fait partie du contrat : c'est lui que lira
            // l'exploitant. Vérifier le type SEUL laisserait passer un message vide.
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            assertThatThrownBy(() -> compte.debiter(Montant.euros("500.00")))
                    .isInstanceOf(SoldeInsuffisantException.class)
                    .hasMessageContaining("FR76-SOURCE")
                    .hasMessageContaining("500.00")
                    .hasMessageContaining("100.00");
        }

        @Test
        @DisplayName("laisse le solde intact quand le débit est refusé")
        void devrait_laisser_le_solde_intact_quand_le_debit_est_refuse() {
            // Vérifier l'exception NE SUFFIT PAS : il faut prouver que l'état
            // n'a pas été partiellement modifié avant l'échec.
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte.debiter(Montant.euros("500.00")));

            assertThat(compte.solde()).isEqualTo(Montant.euros("100.00"));
        }
    }

    @Nested
    @DisplayName("Crédit")
    class Credit {

        @Test
        @DisplayName("augmente le solde")
        void devrait_augmenter_le_solde_quand_le_compte_est_credite() {
            Compte compte = new Compte("FR76-DEST", Montant.euros("100.00"));

            compte.crediter(Montant.euros("25.50"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("125.50"));
        }

        @Test
        @DisplayName("décrit l'état final après une suite d'opérations")
        void devrait_cumuler_les_operations_quand_elles_s_enchainent() {
            Compte compte = new Compte("FR76-DEST", Montant.euros("100.00"));

            compte.crediter(Montant.euros("50.00"));
            compte.debiter(Montant.euros("30.00"));

            // Un seul état final, plusieurs facettes : assertSoftly les rapporte
            // toutes, au lieu de s'arrêter à la première qui échoue.
            assertSoftly(verif -> {
                verif.assertThat(compte.iban()).isEqualTo("FR76-DEST");
                verif.assertThat(compte.solde()).isEqualTo(Montant.euros("120.00"));
                verif.assertThat(compte.solde().estNegatif()).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("Montants invalides")
    class MontantsInvalides {

        @Test
        @DisplayName("refuse un débit de zéro")
        void devrait_refuser_quand_le_montant_est_nul() {
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> compte.debiter(Montant.euros("0.00")));
        }

        @Test
        @DisplayName("refuse un crédit négatif")
        void devrait_refuser_quand_le_credit_est_negatif() {
            // Sans cette règle, crediter(-50) serait un débit déguisé qui
            // contournerait le contrôle de solde.
            Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> compte.crediter(Montant.euros("-50.00")));
        }

        @Test
        @DisplayName("refuse un solde initial négatif")
        void devrait_refuser_quand_le_solde_initial_est_negatif() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Compte("FR76-SOURCE", Montant.euros("-1.00")));
        }
    }
}
