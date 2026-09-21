package fr.formation.banque.integrationpubsub.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.Virement;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DÉMO 8 — tester le flux <b>sortant</b> : « on doit émettre un message ».
 *
 * <p>La chaîne exercée s'arrête à la frontière de messagerie : port
 * {@code NotificateurVirement} → {@code PublieurVirementPubSub} → sérialisation
 * JSON → émulateur Pub/Sub → souscription de contrôle → désérialisation.
 * <b>Ni domaine, ni base de données</b> : le virement passé au port est un objet
 * construit à la main, exactement comme le ferait un test unitaire.
 *
 * <p><b>La question posée est binaire</b> : quand l'application décide d'émettre,
 * un message correctement formé part-il sur le bon topic ? Savoir <i>quand</i>
 * elle décide d'émettre relève du domaine — c'est le chapitre 01.
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
@DisplayName("Flux sortant — publication d'un virement sur Pub/Sub")
class PublicationVirementIT extends SocleIntegrationPubSub {

    private static final String SOUSCRIPTION_DE_CONTROLE = "test-controle-publication";

    /**
     * Injecté par le <b>port du domaine</b>, pas par la classe concrète : le test
     * appelle l'adaptateur exactement comme le ferait le domaine, sans rien
     * savoir de Pub/Sub.
     */
    @Autowired
    private NotificateurVirement notificateur;

    @Autowired
    private PubSubTemplate pubSub;

    @Autowired
    private PubSubAdmin admin;

    private static Virement virement(String reference) {
        return new Virement(reference, "FR76-SOURCE", "FR76-DEST",
                Montant.euros("1000.00"), Montant.euros("1.00"), LocalDate.of(2025, 6, 3));
    }

    @BeforeEach
    void preparer() {
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
    @DisplayName("publie un événement complet quand le port est appelé")
    void devrait_publier_l_evenement_quand_le_notificateur_est_appele() {
        Virement virement = virement("VIR-SORTANT-1");

        notificateur.virementExecute(virement);

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
                    assertThat(evenement.reference()).isEqualTo("VIR-SORTANT-1");
                    assertThat(evenement.ibanSource()).isEqualTo("FR76-SOURCE");
                    assertThat(evenement.ibanDestination()).isEqualTo("FR76-DEST");
                    assertThat(evenement.montant()).isEqualByComparingTo("1000.00");
                    assertThat(evenement.frais()).isEqualByComparingTo("1.00");
                    assertThat(evenement.dateDeValeur()).isEqualTo(LocalDate.of(2025, 6, 3));
                });
    }

    @Test
    @DisplayName("ne publie rien tant que le port n'est pas appelé")
    void devrait_ne_rien_publier_quand_le_notificateur_n_est_pas_appele() {
        // Le pendant du test précédent : c'est bien l'appel au port qui déclenche
        // l'émission, et rien d'autre dans le démarrage de l'application.
        //
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
