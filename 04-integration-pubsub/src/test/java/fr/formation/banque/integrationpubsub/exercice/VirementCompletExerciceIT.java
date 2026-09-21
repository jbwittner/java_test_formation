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
 * <p>Un {@code POST /api/virements} produit trois effets : une réponse 201, deux
 * soldes en base (synchrones, assertion directe) et un événement publié
 * (asynchrone, donc Awaitility — jamais {@code Thread.sleep}).
 *
 * <p>⚠️ La souscription de contrôle doit être créée dans le {@code @BeforeEach} :
 * une souscription ne reçoit que les messages publiés après sa création.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/05-chaine-complete.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.integrationpubsub.corrige.VirementCompletCorrigeIT}
 */
@Disabled("Exercice 5 — voir docs/exercices/05-chaine-complete.md, puis retirer cette annotation")
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
        comptes.enregistrer(new Compte("FR76-SOURCE", Montant.euros("5000.00")));
        comptes.enregistrer(new Compte("FR76-DEST", Montant.euros("0.00")));

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
