package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.formation.banque.antipatterns.support.DepotComptes;
import fr.formation.banque.antipatterns.support.NotificateurAsynchrone;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.HorodatageVirement;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.Virement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 3 — {@code Thread.sleep} pour attendre un traitement asynchrone.
 *
 * <p><b>Symptôme</b> : {@code Thread.sleep(200)} suivi d'une assertion, avec un
 * commentaire du type « on attend que le message parte ».
 *
 * <p><b>Pourquoi c'est grave</b> : le délai est un pari sur la vitesse de la
 * machine. Trop court, le test est <i>flaky</i> — il échoue une fois sur vingt
 * sur la machine d'intégration continue, et l'équipe finit par relancer le build
 * sans lire l'erreur. Trop long, la suite de tests s'allonge de plusieurs
 * minutes. Un test flaky est pire qu'un test absent : il détruit la confiance
 * dans tous les autres.
 *
 * <p><b>Correction</b> : Awaitility — on déclare une condition et un délai
 * <b>maximum</b>. Le test rend la main dès que la condition est vraie (souvent
 * en quelques millisecondes) et échoue avec un message clair si le délai expire.
 * Awaitility est déjà fourni par {@code spring-boot-starter-test}.
 *
 * <p>Le même réflexe s'applique au chapitre 04 (attente d'un message Pub/Sub).
 */
@DisplayName("Anti-pattern 3 — Thread.sleep")
class AntiPattern03ThreadSleepTest {

    private static final Montant MILLE = Montant.euros("1000.00");
    private static final Clock HORLOGE =
            Clock.fixed(Instant.parse("2025-06-03T08:00:00Z"), ZoneId.of("Europe/Paris"));

    private static ServiceVirement service(NotificateurAsynchrone notificateur) {
        DepotComptes comptes = new DepotComptes(
                new Compte("FR76-SOURCE", Montant.euros("5000.00")),
                new Compte("FR76-DEST", Montant.euros("0.00")));
        return new ServiceVirement(comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", notificateur);
    }

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @Disabled("""
                Volontairement desactive : ce test est FLAKY par construction.
                Le notificateur met 300 ms, le test n'attend que 50 ms.
                Retirer ce @Disabled pendant la formation pour le voir echouer,
                puis porter le sleep a 1000 ms pour le voir passer... et
                constater que la suite de tests a pris une seconde de plus.""")
        @DisplayName("attend une durée fixe, au petit bonheur")
        void testNotification() throws InterruptedException {
            try (NotificateurAsynchrone notificateur = new NotificateurAsynchrone(Duration.ofMillis(300))) {
                service(notificateur).executer("FR76-SOURCE", "FR76-DEST", MILLE);

                // Calibré sur le poste du développeur, un jour où la machine était calme.
                Thread.sleep(50);

                assertThat(notificateur.recus()).hasSize(1);
            }
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("attend une condition, avec un délai maximum")
        void devrait_publier_la_notification_quand_le_virement_est_execute() {
            try (NotificateurAsynchrone notificateur = new NotificateurAsynchrone(Duration.ofMillis(300))) {
                service(notificateur).executer("FR76-SOURCE", "FR76-DEST", MILLE);

                // untilAsserted : la condition est un bloc d'assertions. En cas
                // d'expiration, Awaitility rapporte la DERNIERE erreur d'assertion,
                // donc on sait ce qui n'allait pas, pas seulement « timeout ».
                await().atMost(Duration.ofSeconds(5))
                        .pollInterval(Duration.ofMillis(20))
                        .untilAsserted(() -> assertThat(notificateur.recus())
                                .hasSize(1)
                                .first()
                                .returns("VIR-1", Virement::reference)
                                .returns(MILLE, Virement::montant));
            }
        }
    }
}
