package fr.formation.banque.integrationpubsub.exercice;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import fr.formation.banque.persistance.CompteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * EXERCICE 5 — la chaîne complète : HTTP → PostgreSQL → Pub/Sub.
 *
 * <p><b>Objectif</b> : un {@code POST /api/virements} doit produire trois effets.
 * Les trois doivent être vérifiés, et pas de la même façon :
 * <ol>
 *   <li>une réponse HTTP 201 avec les frais calculés — <b>synchrone</b> ;</li>
 *   <li>les soldes mis à jour en base — <b>synchrone</b>, assertion directe ;</li>
 *   <li>un événement publié sur le topic — <b>asynchrone</b>, donc Awaitility.</li>
 * </ol>
 *
 * <p><b>Consignes</b>
 * <ol>
 *   <li>Retirer le {@code @Disabled}.</li>
 *   <li>Créer une souscription de contrôle sur {@code virements-executes} dans
 *       le {@code @BeforeEach}, et la vider — sinon le test lit les messages
 *       laissés par le test précédent.</li>
 *   <li>Écrire le test du parcours nominal (les trois effets).</li>
 *   <li>Écrire le test du refus : 409, aucun solde modifié, <b>et aucun
 *       événement publié</b>.</li>
 * </ol>
 *
 * <p><b>⚠️ Trois pièges classiques</b>
 * <ol>
 *   <li><b>Souscription créée trop tard</b> : une souscription ne reçoit que les
 *       messages publiés APRÈS sa création. Si elle est créée dans le test plutôt
 *       que dans le {@code @BeforeEach}, rien n'arrive jamais.</li>
 *   <li><b>Consommateur concurrent</b> : l'application consomme déjà
 *       {@code virements-executes-journal}. Une souscription distribue chaque
 *       message à UN seul consommateur — d'où la souscription de contrôle
 *       dédiée au test.</li>
 *   <li><b>{@code Thread.sleep}</b> pour attendre le message. Utiliser
 *       {@code await().atMost(...).untilAsserted(...)}. Pour prouver une
 *       <b>absence</b>, {@code await().during(...)} laisse au message une vraie
 *       chance d'arriver avant de conclure.</li>
 * </ol>
 *
 * <p><b>Questions de fin d'exercice</b>
 * <ol>
 *   <li>Comparer la durée de cette classe à celle de
 *       {@code PublieurVirementPubSubTest} (unitaire, mock). Quel rapport ?</li>
 *   <li>{@code PublieurVirementPubSubTest} aurait-il détecté l'absence du
 *       convertisseur JSON ({@code PubSubMessageConversionException}) ? Pourquoi ?</li>
 *   <li>Combien de tests de ce niveau une équipe peut-elle raisonnablement
 *       maintenir dans une suite exécutée à chaque commit ?</li>
 * </ol>
 *
 * <p>Corrigé :
 * {@code fr.formation.banque.integrationpubsub.corrige.VirementCompletCorrigeIT}
 */
@Disabled("TODO exercice 5 — retirer cette annotation puis écrire les tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@DisplayName("EXERCICE 5 — chaîne complète HTTP → PostgreSQL → Pub/Sub")
class VirementCompletExerciceIT extends SocleIntegrationPubSub {

    // TODO : choisir un nom de souscription de controle propre a cette classe
    private static final String SOUSCRIPTION_DE_CONTROLE = "test-controle-exercice-5";

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

        // TODO : creer SOUSCRIPTION_DE_CONTROLE sur le topic "virements-executes"
        //        si elle n'existe pas, puis la vider (pull + ack).
    }

    @Test
    @DisplayName("amorçage : parcours nominal")
    void devrait_produire_les_trois_effets_quand_un_virement_est_demande() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
                        """)
                .exchange();

        // TODO 1 : asserter le statut 201 et $.frais == 1.00
        // TODO 2 : asserter les deux soldes en base (3999.00 et 1000.00)
        // TODO 3 : attendre l'evenement avec Awaitility et asserter son contenu
    }

    // TODO : test du refus (409, aucun solde modifie, aucun evenement publie)
}
