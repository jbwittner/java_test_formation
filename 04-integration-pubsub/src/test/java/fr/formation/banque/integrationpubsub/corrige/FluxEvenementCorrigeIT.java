package fr.formation.banque.integrationpubsub.corrige;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.Virement;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.evenement.VirementRecuEntity;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * CORRIGÉ — exercice 5 : les deux sens du flux, à la frontière de messagerie.
 *
 * <p>Trois tests, et aucune base de données. Ce que cette classe prouve et
 * qu'aucun test plus petit ne prouve : que les <b>deux adaptateurs</b> —
 * publieur et abonné — sont réellement branchés sur un broker, que le JSON
 * qu'ils échangent est le même contrat, et que l'échec d'un traitement entrant
 * ne fait pas disparaître le message.
 *
 * <p>Ce qu'elle ne cherche pas à prouver : les règles de virement (chapitre 01),
 * la persistance (chapitre 02), le contrat HTTP (chapitre 03). Chaque couche est
 * testée une fois, là où elle est le moins chère à tester.
 */
@SpringBootTest
@DisplayName("CORRIGÉ 5 — les deux sens du flux d'événements")
class FluxEvenementCorrigeIT extends SocleIntegrationPubSub {

    private static final String SOUSCRIPTION_DE_CONTROLE = "test-controle-corrige-5";

    @Autowired
    private NotificateurVirement notificateur;

    @Autowired
    private PubSubTemplate pubSub;

    @Autowired
    private PubSubAdmin admin;

    @BeforeEach
    void preparer() {
        if (admin.getSubscription(SOUSCRIPTION_DE_CONTROLE) == null) {
            admin.createSubscription(SOUSCRIPTION_DE_CONTROLE, "virements-executes");
        }
        pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 100, true).forEach(message -> message.ack());
    }

    @Test
    @DisplayName("sortant : l'appel au port publie un événement complet")
    void devrait_publier_un_evenement_quand_le_port_est_appele() {
        notificateur.virementExecute(new Virement("VIR-CO-5", "FR76-SOURCE", "FR76-DEST",
                Montant.euros("1000.00"), Montant.euros("1.00"), LocalDate.of(2025, 6, 3)));

        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<ConvertedAcknowledgeablePubsubMessage<EvenementVirement>> messages =
                            pubSub.pullAndConvert(SOUSCRIPTION_DE_CONTROLE, 10, true, EvenementVirement.class);

                    assertThat(messages).hasSize(1);
                    EvenementVirement evenement = messages.get(0).getPayload();
                    assertThat(evenement.reference()).isEqualTo("VIR-CO-5");
                    assertThat(evenement.ibanSource()).isEqualTo("FR76-SOURCE");
                    assertThat(evenement.ibanDestination()).isEqualTo("FR76-DEST");
                    assertThat(evenement.montant()).isEqualByComparingTo("1000.00");
                    assertThat(evenement.frais()).isEqualByComparingTo("1.00");
                });
    }

    @Test
    @DisplayName("entrant : le message reçu déclenche le traitement avec sa charge utile")
    void devrait_appeler_le_traitement_quand_un_evenement_est_recu() {
        pubSub.publish("virements-executes", new EvenementVirement(
                "VIR-CO-5-ENTRANT", "FR76-SOURCE", "FR76-DEST",
                new BigDecimal("250.00"), new BigDecimal("1.00"), LocalDate.of(2025, 6, 3)));

        ArgumentCaptor<VirementRecuEntity> traite = ArgumentCaptor.forClass(VirementRecuEntity.class);
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(journal).save(traite.capture()));

        assertThat(traite.getValue().reference()).isEqualTo("VIR-CO-5-ENTRANT");
        assertThat(traite.getValue().ibanSource()).isEqualTo("FR76-SOURCE");
        assertThat(traite.getValue().montant()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("entrant : un traitement en échec ne perd pas le message")
    void devrait_rejouer_le_message_quand_le_traitement_leve_une_erreur() {
        when(journal.save(any()))
                .thenThrow(new IllegalStateException("panne simulée"))
                .thenReturn(null);

        pubSub.publish("virements-executes", new EvenementVirement(
                "VIR-CO-5-ECHEC", "FR76-SOURCE", "FR76-DEST",
                new BigDecimal("10.00"), new BigDecimal("1.00"), LocalDate.of(2025, 6, 3)));

        // L'abonné fait un nack, donc Pub/Sub redélivre : le traitement est
        // rappelé. Acquitter avant de traiter perdrait l'événement au premier
        // incident transitoire.
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> verify(journal, atLeast(2)).save(any()));
    }
}
