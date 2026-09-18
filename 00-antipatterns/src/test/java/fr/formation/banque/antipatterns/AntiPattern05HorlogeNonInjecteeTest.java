package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.antipatterns.support.HorodatageNonTestable;
import fr.formation.banque.domaine.HorodatageVirement;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 5 — horloge (et aléa) non injectés.
 *
 * <p><b>Symptôme</b> : {@code LocalDate.now()}, {@code new Date()},
 * {@code UUID.randomUUID()} ou {@code Math.random()} au cœur d'une règle métier.
 *
 * <p><b>Pourquoi c'est grave</b> : le test ne peut plus choisir ses entrées. Il
 * ne reste que deux issues, toutes deux mauvaises :
 * <ul>
 *   <li>rejouer la logique de production dans le test pour calculer l'attendu —
 *       le test devient une copie du code et ne détectera jamais une erreur de
 *       raisonnement, seulement une faute de frappe ;</li>
 *   <li>ne tester que le cas du moment, et découvrir le bug du vendredi 16 h 01
 *       un vendredi à 16 h 01, en production.</li>
 * </ul>
 *
 * <p><b>Correction</b> : injecter un {@link Clock} (ou un générateur). En
 * production on passe {@code Clock.systemDefaultZone()}, en test
 * {@code Clock.fixed(...)}. Le coût est d'un paramètre de constructeur.
 */
@DisplayName("Anti-pattern 5 — horloge non injectée")
class AntiPattern05HorlogeNonInjecteeTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @DisplayName("recopie la règle de production pour calculer l'attendu")
        void testDateDeValeur() {
            LocalDate resultat = new HorodatageNonTestable().dateDeValeur();

            // Le test réimplémente exactement le code testé. Si la règle de
            // coupure est fausse dans les DEUX, le test reste vert. Il ne
            // vérifie plus le comportement, seulement que deux copies du même
            // bug sont d'accord entre elles.
            LocalDateTime maintenant = LocalDateTime.now();
            LocalDate attendu = maintenant.toLocalDate();
            if (!maintenant.toLocalTime().isBefore(LocalTime.of(16, 0))) {
                attendu = attendu.plusDays(1);
            }
            while (attendu.getDayOfWeek() == DayOfWeek.SATURDAY
                    || attendu.getDayOfWeek() == DayOfWeek.SUNDAY) {
                attendu = attendu.plusDays(1);
            }

            assertThat(resultat).isEqualTo(attendu);
        }

        @Test
        @DisplayName("se contente d'une assertion vague pour rester vert à toute heure")
        void testDateDeValeurNonNulle() {
            LocalDate resultat = new HorodatageNonTestable().dateDeValeur();

            // Seule assertion possible sans connaître l'heure d'exécution :
            // une assertion qui ne prouve rien (cf. anti-pattern 2).
            assertThat(resultat).isAfterOrEqualTo(LocalDate.now());

            // Et surtout : la règle du vendredi 17 h est INTESTABLE ici.
            // Il faudrait attendre vendredi 17 h pour la vérifier.
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("fige l'horloge et vérifie la règle du vendredi 17 h")
        void devrait_reporter_au_lundi_quand_le_virement_part_vendredi_apres_la_coupure() {
            // Vendredi 6 juin 2025, 17 h 00 heure de Paris (15 h 00 UTC).
            Clock horloge = Clock.fixed(Instant.parse("2025-06-06T15:00:00Z"), PARIS);

            LocalDate dateDeValeur = new HorodatageVirement(horloge).dateDeValeur();

            // Valeur attendue écrite EN DUR, calculée à la main. C'est ce qui
            // fait du test une spécification indépendante du code.
            assertThat(dateDeValeur).isEqualTo(LocalDate.of(2025, 6, 9));
        }

        @Test
        @DisplayName("vérifie la borne exacte de la coupure de 16 h")
        void devrait_basculer_au_lendemain_quand_il_est_exactement_16h() {
            Clock horloge = Clock.fixed(Instant.parse("2025-06-03T14:00:00Z"), PARIS);

            LocalDate dateDeValeur = new HorodatageVirement(horloge).dateDeValeur();

            assertThat(dateDeValeur).isEqualTo(LocalDate.of(2025, 6, 4));
        }
    }
}
