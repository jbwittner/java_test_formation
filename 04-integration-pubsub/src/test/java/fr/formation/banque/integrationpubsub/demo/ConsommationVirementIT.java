package fr.formation.banque.integrationpubsub.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.evenement.VirementRecuEntity;
import fr.formation.banque.integrationpubsub.support.SocleIntegrationPubSub;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DÉMO 9 — tester le flux <b>entrant</b> : « un message reçu déclenche le
 * traitement ».
 *
 * <p>Le sens est inverse de la démo 8 : le test <b>publie</b>, l'application
 * <b>consomme</b>. La chaîne exercée s'arrête à la frontière : émulateur Pub/Sub
 * → souscription de l'application → désérialisation JSON → {@code AbonneVirement}
 * → <b>appel au traitement métier, qui est mocké</b>.
 *
 * <p><b>Pourquoi mocker le traitement ?</b> Parce que la question de ce chapitre
 * est celle du branchement, pas celle du métier : le message arrive-t-il, est-il
 * relu avec les bonnes valeurs, l'adaptateur appelle-t-il le bon collaborateur ?
 * Ce que ce collaborateur fait ensuite en base relève du chapitre 02, où il se
 * teste sans broker et en une fraction du temps.
 *
 * <p><b>Ce que le mock rend visible et qu'une assertion en base cachait</b> : les
 * <b>interactions</b>. Combien de fois le traitement a-t-il été appelé ? Avec
 * quelle charge utile ? Que se passe-t-il quand il échoue ? Ce sont précisément
 * les questions de l'{@code ack} / {@code nack} et de l'idempotence.
 *
 * <p><b>Asynchronisme</b> : rien n'est instantané, donc chaque assertion est
 * enveloppée dans une attente de <b>condition</b> Awaitility — jamais un
 * {@code Thread.sleep} (anti-pattern 3).
 */
@SpringBootTest
@DisplayName("Flux entrant — consommation d'un virement depuis Pub/Sub")
class ConsommationVirementIT extends SocleIntegrationPubSub {

    @Autowired
    private PubSubTemplate pubSub;

    private static EvenementVirement evenement(String reference) {
        return new EvenementVirement(reference, "FR76-SOURCE", "FR76-DEST",
                new BigDecimal("1000.00"), new BigDecimal("1.00"),
                LocalDate.of(2025, 6, 3));
    }

    @Test
    @DisplayName("déclenche le traitement avec la charge utile reçue")
    void devrait_appeler_le_traitement_quand_un_evenement_est_publie() {
        pubSub.publish("virements-executes", evenement("VIR-ENTRANT-1"));

        ArgumentCaptor<VirementRecuEntity> journalise = ArgumentCaptor.forClass(VirementRecuEntity.class);
        await().atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(journal).save(journalise.capture()));

        // L'assertion qui compte : le JSON a bien été relu, champ par champ.
        // Une faute de frappe dans un nom de champ casserait ici — et nulle part
        // dans les tests unitaires du consommateur.
        VirementRecuEntity recu = journalise.getValue();
        assertThat(recu.reference()).isEqualTo("VIR-ENTRANT-1");
        assertThat(recu.ibanSource()).isEqualTo("FR76-SOURCE");
        assertThat(recu.ibanDestination()).isEqualTo("FR76-DEST");
        assertThat(recu.montant()).isEqualByComparingTo("1000.00");
        assertThat(recu.recuLe()).isNotNull();
    }

    @Test
    @DisplayName("n'appelle pas deux fois le traitement quand l'événement est déjà connu")
    void devrait_ignorer_l_evenement_quand_il_est_deja_journalise() {
        // Le journal répond « déjà vu » : c'est la situation d'une redélivrance,
        // que Pub/Sub garantit possible puisqu'il livre AU MOINS une fois.
        when(journal.existsById("VIR-DEJA-VU")).thenReturn(true);

        pubSub.publish("virements-executes", evenement("VIR-DEJA-VU"));

        // Laisser une vraie chance au message d'arriver avant de conclure à
        // l'absence d'appel : asserter immédiatement rendrait le test vert par
        // pure chance de calendrier.
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> verify(journal, never()).save(any()));

        // Et le message a bien été lu — sinon le test ci-dessus serait vert même
        // avec un abonné débranché.
        verify(journal, atLeast(1)).existsById("VIR-DEJA-VU");
    }

    @Test
    @DisplayName("redélivre le message quand le traitement échoue")
    void devrait_rejouer_le_message_quand_le_traitement_leve_une_erreur() {
        // Premier essai en échec (base indisponible, par exemple), puis succès.
        when(journal.save(any()))
                .thenThrow(new IllegalStateException("panne simulée"))
                .thenReturn(null);

        pubSub.publish("virements-executes", evenement("VIR-EN-ECHEC"));

        // L'enseignement : l'abonné fait un nack sur échec, donc Pub/Sub
        // redélivre et le traitement est rappelé. Acquitter AVANT de traiter
        // ferait disparaître l'événement à la première erreur — un bug fréquent,
        // que seul un test avec un vrai broker peut exposer.
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> verify(journal, atLeast(2))
                        .save(argThat(recu -> "VIR-EN-ECHEC".equals(recu.reference()))));
    }
}
