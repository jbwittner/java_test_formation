package fr.formation.banque.integrationpubsub.exercice;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.Virement;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * EXERCICE 5 — les deux sens du flux, à la frontière de messagerie.
 *
 * <p>Deux tests, un par sens, et rien d'autre :
 * <ul>
 *   <li><b>sortant</b> — appeler le port {@link NotificateurVirement} et prouver
 *       qu'un message correctement sérialisé arrive sur le topic ;</li>
 *   <li><b>entrant</b> — publier un message et prouver que le traitement métier
 *       (mocké : le champ hérité {@code journal}) est appelé avec la charge
 *       utile reçue.</li>
 * </ul>
 *
 * <p>Ni base de données ni HTTP ici : les règles de virement se testent au
 * chapitre 01, la persistance au chapitre 02, le contrat REST au chapitre 03.
 * La question posée est celle du <b>branchement</b>, et d'elle seule.
 *
 * <p>⚠️ La souscription de contrôle doit être créée dans le {@code @BeforeEach} :
 * une souscription ne reçoit que les messages publiés après sa création.
 *
 * <p>⚠️ Ne pas ajouter de {@code @MockitoBean} dans cette classe : un jeu de
 * remplacements différent de celui du socle créerait un second contexte Spring,
 * donc un second abonné en concurrence sur la même souscription.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/05-flux-evenements.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.integrationpubsub.corrige.FluxEvenementCorrigeIT}
 */
@Disabled("Exercice 5 — voir docs/exercices/05-flux-evenements.md, puis retirer cette annotation")
@SpringBootTest
@DisplayName("EXERCICE 5 — les deux sens du flux d'événements")
class FluxEvenementExerciceIT extends SocleIntegrationPubSub {

    // TODO : choisir un nom de souscription de controle propre a cette classe
    private static final String SOUSCRIPTION_DE_CONTROLE = "test-controle-exercice-5";

    @Autowired
    private NotificateurVirement notificateur;

    @Autowired
    private PubSubTemplate pubSub;

    @Autowired
    private PubSubAdmin admin;

    @BeforeEach
    void preparer() {
        // TODO : creer SOUSCRIPTION_DE_CONTROLE sur le topic "virements-executes"
        //        si elle n'existe pas, puis la vider (pull + ack).
    }

    @Test
    @DisplayName("amorçage : sens sortant")
    void devrait_publier_un_evenement_quand_le_port_est_appele() {
        Virement virement = new Virement("VIR-EX-5", "FR76-SOURCE", "FR76-DEST",
                Montant.euros("1000.00"), Montant.euros("1.00"), LocalDate.of(2025, 6, 3));

        notificateur.virementExecute(virement);

        // TODO 1 : attendre le message avec Awaitility sur SOUSCRIPTION_DE_CONTROLE
        //          (pullAndConvert vers EvenementVirement)
        // TODO 2 : asserter la reference, les deux IBAN, le montant et les frais
    }

    @Test
    @DisplayName("amorçage : sens entrant")
    void devrait_appeler_le_traitement_quand_un_evenement_est_recu() {
        pubSub.publish("virements-executes", new EvenementVirement(
                "VIR-EX-5-ENTRANT", "FR76-SOURCE", "FR76-DEST",
                new BigDecimal("1000.00"), new BigDecimal("1.00"), LocalDate.of(2025, 6, 3)));

        // TODO 3 : attendre, avec Awaitility, que journal.save(...) soit appele
        //          (ArgumentCaptor<VirementRecuEntity>)
        // TODO 4 : asserter le contenu capture — c'est la preuve que le JSON a
        //          ete relu avec les bons noms de champs
    }

    // TODO : troisieme test, au choix — que se passe-t-il si le traitement leve
    //        une exception ? Le message est-il perdu, ou redelivre ?
}
