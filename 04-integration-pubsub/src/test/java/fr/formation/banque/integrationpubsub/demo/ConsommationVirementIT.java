package fr.formation.banque.integrationpubsub.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.evenement.VirementRecuEntity;
import fr.formation.banque.evenement.VirementRecuRepository;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DÉMO 9 — tester la consommation d'un événement (flux entrant).
 *
 * <p>Le sens est inverse de la démo 8 : le test <b>publie</b>, l'application
 * <b>consomme</b>, et l'on observe l'effet de bord en base.
 *
 * <p><b>Principe à retenir</b> : sur un flux asynchrone, on ne teste pas
 * « la méthode a été appelée » — on teste <b>l'effet observable</b>. Ici, la
 * ligne écrite dans {@code virement_recu}. C'est plus lent à obtenir, mais
 * beaucoup plus solide : le test survit à une refonte du consommateur.
 *
 * <p><b>Idempotence</b> : Pub/Sub garantit une livraison <i>au moins une fois</i>.
 * Le second test publie deux fois le même événement et vérifie qu'une seule
 * ligne apparaît. Sans ce test, le doublon ne se découvre qu'en production, le
 * jour d'une redélivrance — c'est-à-dire au pire moment.
 */
@SpringBootTest
@DisplayName("Consommation d'un virement depuis Pub/Sub")
class ConsommationVirementIT extends SocleIntegrationPubSub {

    @Autowired
    private PubSubTemplate pubSub;

    @Autowired
    private VirementRecuRepository journal;

    @BeforeEach
    void viderLeJournal() {
        journal.deleteAll();
    }

    private static EvenementVirement evenement(String reference) {
        return new EvenementVirement(reference, "FR76-SOURCE", "FR76-DEST",
                new BigDecimal("1000.00"), new BigDecimal("1.00"),
                LocalDate.of(2025, 6, 3));
    }

    @Test
    @DisplayName("journalise l'événement reçu")
    void devrait_journaliser_l_evenement_quand_il_est_publie() {
        pubSub.publish("virements-executes", evenement("VIR-ENTRANT-1"));

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertThat(journal.findById("VIR-ENTRANT-1"))
                            .isPresent()
                            .get()
                            .satisfies(ligne -> {
                                assertThat(ligne.ibanSource()).isEqualTo("FR76-SOURCE");
                                assertThat(ligne.ibanDestination()).isEqualTo("FR76-DEST");
                                assertThat(ligne.montant()).isEqualByComparingTo("1000.00");
                                assertThat(ligne.recuLe()).isNotNull();
                            });
                });
    }

    @Test
    @DisplayName("reste idempotent quand le même événement est livré deux fois")
    void devrait_ne_journaliser_qu_une_fois_quand_l_evenement_est_redelivre() {
        // Simule une redélivrance : même référence, deux publications.
        pubSub.publish("virements-executes", evenement("VIR-DOUBLON"));
        pubSub.publish("virements-executes", evenement("VIR-DOUBLON"));

        // D'abord attendre que le traitement ait bien eu lieu...
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(journal.findById("VIR-DOUBLON")).isPresent());

        // ... puis laisser le temps à un éventuel second traitement de se
        // manifester avant de conclure. Asserter immédiatement « une seule
        // ligne » rendrait le test vert par pure chance de calendrier.
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(6))
                .untilAsserted(() -> assertThat(journal.count()).isEqualTo(1));
    }

    @Test
    @DisplayName("traite plusieurs événements distincts")
    void devrait_journaliser_chaque_reference_quand_plusieurs_evenements_arrivent() {
        pubSub.publish("virements-executes", evenement("VIR-A"));
        pubSub.publish("virements-executes", evenement("VIR-B"));
        pubSub.publish("virements-executes", evenement("VIR-C"));

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(journal.findAll())
                        .extracting(VirementRecuEntity::reference)
                        // Aucun ordre n'est garanti sur un flux de messages :
                        // containsExactly serait un test intermittent
                        // (anti-pattern 9).
                        .containsExactlyInAnyOrder("VIR-A", "VIR-B", "VIR-C"));
    }
}
