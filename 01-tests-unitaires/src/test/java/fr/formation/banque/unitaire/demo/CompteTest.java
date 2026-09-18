package fr.formation.banque.unitaire.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.TypeCompte;
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
 *       solde disponible doit passer, un centime de plus doit échouer. Tester
 *       uniquement le cas nominal ne prouve presque rien.</li>
 *   <li>{@code assertSoftly} quand plusieurs assertions décrivent UN SEUL état
 *       final : on veut toutes les différences d'un coup, pas la première.</li>
 *   <li>Les tests sont indépendants : aucun champ mutable partagé entre méthodes.
 *       Chaque test construit son propre compte (cf. anti-pattern n°4).</li>
 * </ol>
 */
@DisplayName("Compte — règles de débit et de crédit")
class CompteTest {

    @Nested
    @DisplayName("Débit sans découvert autorisé")
    class DebitSansDecouvert {

        @Test
        @DisplayName("diminue le solde quand le montant est couvert")
        void devrait_diminuer_le_solde_quand_le_montant_est_couvert() {
            Compte compte = Compte.standard("FR76-SOURCE", Montant.euros("100.00"));

            compte.debiter(Montant.euros("30.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("70.00"));
        }

        @Test
        @DisplayName("accepte un débit égal au solde exact")
        void devrait_accepter_quand_le_debit_egale_le_solde() {
            // Cas limite « inclusif » : la borne elle-même doit passer.
            Compte compte = Compte.standard("FR76-SOURCE", Montant.euros("100.00"));

            compte.debiter(Montant.euros("100.00"));

            assertThat(compte.solde()).isEqualTo(Montant.euros("0.00"));
        }

        @Test
        @DisplayName("refuse un débit d'un centime de plus que le solde")
        void devrait_lever_SoldeInsuffisantException_quand_le_debit_depasse_le_solde() {
            // Cas limite « exclusif » : le premier montant refusé, pas un montant
            // absurde comme 1 000 000. Un test de borne vaut dix tests génériques.
            Compte compte = Compte.standard("FR76-SOURCE", Montant.euros("100.00"));

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte.debiter(Montant.euros("100.01")))
                    .satisfies(erreur -> assertThat(erreur.iban()).isEqualTo("FR76-SOURCE"));
        }

        @Test
        @DisplayName("laisse le solde intact quand le débit est refusé")
        void devrait_laisser_le_solde_intact_quand_le_debit_est_refuse() {
            // Vérifier l'exception NE SUFFIT PAS : il faut prouver que l'état
            // n'a pas été partiellement modifié avant l'échec.
            Compte compte = Compte.standard("FR76-SOURCE", Montant.euros("100.00"));

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte.debiter(Montant.euros("500.00")));

            assertThat(compte.solde()).isEqualTo(Montant.euros("100.00"));
        }
    }

    @Nested
    @DisplayName("Débit avec découvert autorisé")
    class DebitAvecDecouvert {

        private Compte compteAvecDecouvert() {
            return new Compte("FR76-DECOUVERT", TypeCompte.STANDARD,
                    Montant.euros("100.00"), Montant.euros("200.00"));
        }

        @Test
        @DisplayName("autorise un solde négatif dans la limite du découvert")
        void devrait_autoriser_un_solde_negatif_quand_le_decouvert_le_couvre() {
            Compte compte = compteAvecDecouvert();

            compte.debiter(Montant.euros("250.00"));

            // Un seul état final, plusieurs facettes : assertSoftly les rapporte toutes.
            assertSoftly(verif -> {
                verif.assertThat(compte.solde()).isEqualTo(Montant.euros("-150.00"));
                verif.assertThat(compte.solde().estNegatif()).isTrue();
                verif.assertThat(compte.montantDisponible()).isEqualTo(Montant.euros("50.00"));
            });
        }

        @Test
        @DisplayName("refuse un débit au-delà de solde + découvert")
        void devrait_refuser_quand_le_debit_depasse_solde_plus_decouvert() {
            Compte compte = compteAvecDecouvert();

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte.debiter(Montant.euros("300.01")));
        }
    }

    @Nested
    @DisplayName("Montants invalides")
    class MontantsInvalides {

        @Test
        @DisplayName("refuse un débit de zéro")
        void devrait_refuser_quand_le_montant_est_nul() {
            Compte compte = Compte.standard("FR76-SOURCE", Montant.euros("100.00"));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> compte.debiter(Montant.euros("0.00")));
        }

        @Test
        @DisplayName("refuse un crédit négatif")
        void devrait_refuser_quand_le_credit_est_negatif() {
            // Sans cette règle, crediter(-50) serait un débit déguisé qui
            // contournerait le contrôle de solde.
            Compte compte = Compte.standard("FR76-SOURCE", Montant.euros("100.00"));

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> compte.crediter(Montant.euros("-50.00")));
        }
    }
}
