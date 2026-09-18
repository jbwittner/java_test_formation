package fr.formation.banque.integrationpubsub.corrige;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * CORRIGÉ — exercice 5 : la chaîne complète, HTTP → base → Pub/Sub.
 *
 * <p>C'est le test le plus coûteux du projet : contexte Spring complet, serveur
 * web, PostgreSQL et émulateur Pub/Sub. Il doit donc rester <b>rare</b> et ne
 * couvrir que le parcours principal. Tous les cas d'erreur sont déjà traités
 * plus bas dans la pyramide (chapitres 01 et 03).
 *
 * <p>Ce qu'il apporte et qu'aucun test plus petit n'apporte : la preuve que les
 * trois adaptateurs — HTTP, JPA, Pub/Sub — sont branchés <b>ensemble</b> sur le
 * même domaine, et qu'une requête réelle produit les trois effets attendus.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@DisplayName("CORRIGÉ 5 — chaîne complète HTTP → PostgreSQL → Pub/Sub")
class VirementCompletCorrigeIT extends SocleIntegrationPubSub {

    private static final String SOUSCRIPTION_DE_CONTROLE = "test-controle-chaine-complete";

    @Autowired
    private RestTestClient client;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private CompteJpaRepository jpa;

    @Autowired
    private PubSubTemplate pubSub;

    @Autowired
    private PubSubAdmin admin;

    @BeforeEach
    void preparer() {
        jpa.deleteAll();
        comptes.enregistrer(Compte.standard("FR76-SOURCE", Montant.euros("5000.00")));
        comptes.enregistrer(Compte.standard("FR76-DEST", Montant.euros("0.00")));

        if (admin.getSubscription(SOUSCRIPTION_DE_CONTROLE) == null) {
            admin.createSubscription(SOUSCRIPTION_DE_CONTROLE, "virements-executes");
        }
        pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 100, true).forEach(message -> message.ack());
    }

    @Test
    @DisplayName("un POST produit la réponse HTTP, le débit en base et l'événement publié")
    void devrait_produire_les_trois_effets_quand_un_virement_est_demande() {
        // 1. Effet HTTP
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.frais").isEqualTo(1.00);

        // 2. Effet base : synchrone, donc assertion directe.
        assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                .isEqualTo(Montant.euros("3999.00"));
        assertThat(comptes.parIban("FR76-DEST").orElseThrow().solde())
                .isEqualTo(Montant.euros("1000.00"));

        // 3. Effet Pub/Sub : asynchrone, donc attente d'une condition.
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<ConvertedAcknowledgeablePubsubMessage<EvenementVirement>> messages =
                            pubSub.pullAndConvert(SOUSCRIPTION_DE_CONTROLE, 10, true, EvenementVirement.class);

                    assertThat(messages).hasSize(1);
                    EvenementVirement evenement = messages.get(0).getPayload();
                    assertThat(evenement.ibanSource()).isEqualTo("FR76-SOURCE");
                    assertThat(evenement.ibanDestination()).isEqualTo("FR76-DEST");
                    assertThat(evenement.montant()).isEqualByComparingTo("1000.00");
                    assertThat(evenement.frais()).isEqualByComparingTo("1.00");
                });
    }

    @Test
    @DisplayName("un virement refusé ne produit ni débit ni événement")
    void devrait_ne_produire_aucun_effet_quand_le_virement_est_refuse() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":999999.00}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                .isEqualTo(Montant.euros("5000.00"));

        // Un événement publié pour un virement qui n'a pas eu lieu déclencherait
        // des traitements en aval sur une opération inexistante : c'est le type
        // de bug que ce test rend impossible.
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(6))
                .untilAsserted(() -> assertThat(
                        pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 10, true)).isEmpty());
    }
}
