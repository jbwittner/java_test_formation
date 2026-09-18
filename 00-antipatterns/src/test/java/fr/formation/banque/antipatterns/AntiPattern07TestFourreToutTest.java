package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import fr.formation.banque.antipatterns.support.DepotComptes;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.HorodatageVirement;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.TypeCompte;
import fr.formation.banque.domaine.Virement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 7 — le test fourre-tout.
 *
 * <p><b>Symptôme</b> : une méthode de cinquante lignes qui enchaîne plusieurs
 * scénarios, souvent nommée {@code testVirement} ou {@code testNominal}.
 *
 * <p><b>Pourquoi c'est grave</b> :
 * <ul>
 *   <li>JUnit s'arrête à la <b>première</b> assertion fausse : les suivantes ne
 *       sont jamais évaluées, on corrige un bug à la fois, en relançant à chaque fois ;</li>
 *   <li>le rapport affiche une seule ligne rouge pour six règles métier — on ne
 *       sait pas laquelle est cassée sans lire la trace ;</li>
 *   <li>le test devient un point de passage obligé : tout le monde y ajoute son
 *       cas, personne n'ose le découper.</li>
 * </ul>
 *
 * <p><b>Correction</b> : un test = un comportement, nommé d'après ce comportement.
 * Et quand plusieurs assertions décrivent réellement <b>un seul</b> état final,
 * {@code assertSoftly} les évalue toutes et rapporte toutes les différences d'un coup.
 */
@DisplayName("Anti-pattern 7 — le test fourre-tout")
class AntiPattern07TestFourreToutTest {

    private static final Clock HORLOGE =
            Clock.fixed(Instant.parse("2025-06-03T08:00:00Z"), ZoneId.of("Europe/Paris"));

    private static ServiceVirement service(DepotComptes comptes) {
        return new ServiceVirement(comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", v -> { });
    }

    private static DepotComptes comptesStandard() {
        return new DepotComptes(
                Compte.standard("FR76-SOURCE", Montant.euros("5000.00")),
                Compte.standard("FR76-DEST", Montant.euros("0.00")));
    }

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @DisplayName("teste six règles dans une seule méthode")
        void testVirement() {
            DepotComptes comptes = comptesStandard();
            ServiceVirement service = service(comptes);

            Virement virement = service.executer("FR76-SOURCE", "FR76-DEST", Montant.euros("1000.00"));

            // Six règles métier différentes. Si la première casse, on ne saura
            // rien des cinq autres. Si le nom du test apparaît en rouge, il
            // faudra lire la trace pour savoir laquelle a lâché.
            assertThat(virement.frais()).isEqualTo(Montant.euros("1.00"));
            assertThat(virement.dateDeValeur()).isEqualTo(LocalDate.of(2025, 6, 3));
            assertThat(virement.reference()).isEqualTo("VIR-1");

            Compte source = comptes.parIban("FR76-SOURCE").orElseThrow();
            assertThat(source.solde()).isEqualTo(Montant.euros("3999.00"));

            // ... et en prime un second scénario, glissé dans le même test.
            DepotComptes autresComptes = new DepotComptes(
                    Compte.standard("FR76-PAUVRE", Montant.euros("10.00")),
                    Compte.standard("FR76-DEST", Montant.euros("0.00")));
            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> service(autresComptes)
                            .executer("FR76-PAUVRE", "FR76-DEST", Montant.euros("1000.00")));

            // ... et un troisième.
            DepotComptes comptesPremium = new DepotComptes(
                    new Compte("FR76-VIP", TypeCompte.PREMIUM, Montant.euros("5000.00"), Montant.euros("0.00")),
                    Compte.standard("FR76-DEST", Montant.euros("0.00")));
            assertThat(service(comptesPremium)
                    .executer("FR76-VIP", "FR76-DEST", Montant.euros("1000.00")).frais())
                    .isEqualTo(Montant.euros("0.00"));
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("décrit le virement produit — un seul état, assertions groupées")
        void devrait_produire_un_virement_complet_quand_le_solde_suffit() {
            DepotComptes comptes = comptesStandard();

            Virement virement = service(comptes)
                    .executer("FR76-SOURCE", "FR76-DEST", Montant.euros("1000.00"));

            // Ces quatre assertions décrivent UN objet. assertSoftly les évalue
            // toutes : un échec rapporte les quatre écarts, pas seulement le premier.
            assertSoftly(verif -> {
                verif.assertThat(virement.reference()).isEqualTo("VIR-1");
                verif.assertThat(virement.montant()).isEqualTo(Montant.euros("1000.00"));
                verif.assertThat(virement.frais()).isEqualTo(Montant.euros("1.00"));
                verif.assertThat(virement.dateDeValeur()).isEqualTo(LocalDate.of(2025, 6, 3));
            });
        }

        @Test
        @DisplayName("débite le compte source du montant et des frais")
        void devrait_debiter_la_source_du_montant_et_des_frais() {
            DepotComptes comptes = comptesStandard();

            service(comptes).executer("FR76-SOURCE", "FR76-DEST", Montant.euros("1000.00"));

            assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                    .isEqualTo(Montant.euros("3999.00"));
        }

        @Test
        @DisplayName("refuse le virement quand le solde est insuffisant")
        void devrait_refuser_quand_le_solde_est_insuffisant() {
            DepotComptes comptes = new DepotComptes(
                    Compte.standard("FR76-PAUVRE", Montant.euros("10.00")),
                    Compte.standard("FR76-DEST", Montant.euros("0.00")));

            assertThatExceptionOfType(SoldeInsuffisantException.class)
                    .isThrownBy(() -> service(comptes)
                            .executer("FR76-PAUVRE", "FR76-DEST", Montant.euros("1000.00")));
        }

        @Test
        @DisplayName("exonère de frais un compte premium")
        void devrait_exonerer_de_frais_quand_le_compte_source_est_premium() {
            DepotComptes comptes = new DepotComptes(
                    new Compte("FR76-VIP", TypeCompte.PREMIUM, Montant.euros("5000.00"), Montant.euros("0.00")),
                    Compte.standard("FR76-DEST", Montant.euros("0.00")));

            Virement virement = service(comptes)
                    .executer("FR76-VIP", "FR76-DEST", Montant.euros("1000.00"));

            assertThat(virement.frais()).isEqualTo(Montant.euros("0.00"));
        }
    }
}
