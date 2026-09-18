package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Devise;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.TypeCompte;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 6 — tester les accesseurs.
 *
 * <p><b>Symptôme</b> : des tests qui construisent un objet puis vérifient que
 * {@code getX()} renvoie ce qui a été passé au constructeur.
 *
 * <p><b>Pourquoi c'est grave</b> : ces tests ne peuvent échouer que si le
 * compilateur est cassé. Ils gonflent la couverture — c'est souvent leur
 * véritable raison d'être — et donnent l'illusion d'une base bien testée. Ils
 * coûtent en plus de la maintenance à chaque renommage.
 *
 * <p><b>Correction</b> : tester des <b>comportements</b>, c'est-à-dire du code
 * qui contient une décision (un {@code if}, un calcul, une exception). Un
 * accesseur n'en contient aucune.
 *
 * <p><b>À retenir sur la couverture</b> : la couverture indique ce qui n'est
 * <i>pas</i> testé, jamais ce qui est <i>bien</i> testé. Les deux blocs
 * ci-dessous couvrent les mêmes lignes ; un seul détecte une régression.
 */
@DisplayName("Anti-pattern 6 — tester les accesseurs")
class AntiPattern06TesterLesAccesseursTest {

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @DisplayName("vérifie que le constructeur affecte bien ses champs")
        void testGetters() {
            Compte compte = new Compte("FR76-A", TypeCompte.PREMIUM,
                    Montant.euros("100.00"), Montant.euros("50.00"));

            // Aucune décision n'est testée : ce test vérifie le langage Java.
            assertThat(compte.iban()).isEqualTo("FR76-A");
            assertThat(compte.type()).isEqualTo(TypeCompte.PREMIUM);
            assertThat(compte.solde()).isEqualTo(Montant.euros("100.00"));
            assertThat(compte.decouvertAutorise()).isEqualTo(Montant.euros("50.00"));
        }

        @Test
        @DisplayName("vérifie que le record expose ses composants")
        void testMontantGetters() {
            Montant montant = new Montant(new BigDecimal("12.34"), Devise.EUR);

            assertThat(montant.valeur()).isEqualByComparingTo("12.34");
            assertThat(montant.devise()).isEqualTo(Devise.EUR);
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("vérifie la décision prise par le calcul du disponible")
        void devrait_additionner_solde_et_decouvert_pour_le_montant_disponible() {
            // montantDisponible() contient un vrai calcul : il peut être faux.
            Compte compte = new Compte("FR76-A", TypeCompte.PREMIUM,
                    Montant.euros("100.00"), Montant.euros("50.00"));

            assertThat(compte.montantDisponible()).isEqualTo(Montant.euros("150.00"));
        }

        @Test
        @DisplayName("vérifie la règle de refus au-delà du disponible")
        void devrait_refuser_le_debit_quand_il_depasse_le_disponible() {
            Compte compte = new Compte("FR76-A", TypeCompte.PREMIUM,
                    Montant.euros("100.00"), Montant.euros("50.00"));

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> compte.debiter(Montant.euros("150.01")));
        }

        @Test
        @DisplayName("vérifie la normalisation d'échelle, qui est une décision du record")
        void devrait_normaliser_l_echelle_a_deux_decimales() {
            // Ici on ne teste pas l'accesseur : on teste le compact constructor
            // qui arrondit. C'est du comportement, pas de la plomberie.
            Montant montant = new Montant(new BigDecimal("12.3456"), Devise.EUR);

            assertThat(montant.valeur()).isEqualByComparingTo("12.35");
        }
    }
}
