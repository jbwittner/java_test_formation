package fr.formation.banque.integrationpubsub.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.Virement;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DÉMO 8 — tester la publication d'un événement (flux sortant).
 *
 * <p>La chaîne exercée : {@code ServiceVirement} (domaine) → port
 * {@code NotificateurVirement} → {@code PublieurVirementPubSub} → sérialisation
 * JSON → émulateur Pub/Sub → souscription de contrôle → désérialisation.
 *
 * <p><b>Ce que ce test prouve, qu'un mock de {@code PubSubTemplate} ne prouve pas :</b>
 * <ul>
 *   <li>que le topic visé existe et porte le nom attendu — une faute de frappe
 *       dans la configuration passe tous les tests unitaires ;</li>
 *   <li>que la charge utile est réellement sérialisable, avec les bons noms de
 *       champs : c'est le contrat que liront les autres services ;</li>
 *   <li>qu'un consommateur peut la relire et la reconstruire.</li>
 * </ul>
 *
 * <p><b>Asynchronisme</b> : {@code publish} rend la main avant que le message ne
 * soit parti. Le test attend donc une <b>condition</b> avec Awaitility, jamais une
 * durée fixe (anti-pattern 3). Awaitility est fourni par
 * {@code spring-boot-starter-test}.
 *
 * <p><b>Souscription de contrôle dédiée</b> : le test crée sa propre souscription
 * sur le topic, distincte de celle que consomme l'application. Sans cela, les
 * deux se disputeraient les messages et le test serait intermittent — une
 * souscription Pub/Sub distribue chaque message à UN seul de ses consommateurs.
 */
@SpringBootTest
@DisplayName("Publication d'un virement sur Pub/Sub")
class PublicationVirementIT extends SocleIntegrationPubSub {

    private static final String SOUSCRIPTION_DE_CONTROLE = "test-controle-publication";

    @Autowired
    private ServiceVirement virements;

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

        // Créée AVANT la publication : une souscription Pub/Sub ne reçoit que les
        // messages publiés après sa création. C'est la cause n°1 de test
        // « qui ne reçoit rien » sur l'émulateur.
        if (admin.getSubscription(SOUSCRIPTION_DE_CONTROLE) == null) {
            admin.createSubscription(SOUSCRIPTION_DE_CONTROLE, "virements-executes");
        }
        viderLaSouscriptionDeControle();
    }

    @AfterEach
    void nettoyer() {
        viderLaSouscriptionDeControle();
    }

    private void viderLaSouscriptionDeControle() {
        // Isolation entre tests : les messages non consommés d'un test
        // précédent fausseraient le suivant (anti-pattern 4, version broker).
        pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 100, true)
                .forEach(message -> message.ack());
    }

    @Test
    @DisplayName("publie un événement complet quand le virement est exécuté")
    void devrait_publier_l_evenement_quand_le_virement_est_execute() {
        Virement virement = virements.executer("FR76-SOURCE", "FR76-DEST", Montant.euros("1000.00"));

        // Attente d'une CONDITION, pas d'une durée : le test se termine dès que
        // le message est là, et échoue avec la dernière erreur d'assertion si
        // le délai maximum est dépassé.
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    List<ConvertedAcknowledgeablePubsubMessage<EvenementVirement>> messages =
                            pubSub.pullAndConvert(SOUSCRIPTION_DE_CONTROLE, 10, true, EvenementVirement.class);

                    assertThat(messages).hasSize(1);
                    EvenementVirement evenement = messages.get(0).getPayload();
                    assertThat(evenement.reference()).isEqualTo(virement.reference());
                    assertThat(evenement.ibanSource()).isEqualTo("FR76-SOURCE");
                    assertThat(evenement.ibanDestination()).isEqualTo("FR76-DEST");
                    assertThat(evenement.montant()).isEqualByComparingTo("1000.00");
                    assertThat(evenement.frais()).isEqualByComparingTo("1.00");
                    assertThat(evenement.devise()).isEqualTo("EUR");
                    assertThat(evenement.dateDeValeur()).isEqualTo(virement.dateDeValeur());
                });
    }

    @Test
    @DisplayName("ne publie rien quand le virement est refusé")
    void devrait_ne_rien_publier_quand_le_solde_est_insuffisant() {
        try {
            virements.executer("FR76-SOURCE", "FR76-DEST", Montant.euros("999999.00"));
        } catch (RuntimeException attendue) {
            // L'exception est le sujet d'un autre test ; ici on vérifie l'absence
            // d'effet de bord.
        }

        // Prouver une ABSENCE demande de laisser une vraie chance au message
        // d'arriver : on attend un court instant, puis on vérifie que rien n'est
        // venu. C'est le seul cas où une attente fixe se justifie — et Awaitility
        // l'exprime proprement avec during().
        await().during(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(
                        pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 10, true)).isEmpty());
    }
}
