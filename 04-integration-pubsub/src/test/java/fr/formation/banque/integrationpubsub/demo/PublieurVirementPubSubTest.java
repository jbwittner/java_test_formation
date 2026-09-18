package fr.formation.banque.integrationpubsub.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import fr.formation.banque.configuration.ProprietesPubSub;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.Virement;
import fr.formation.banque.evenement.EvenementVirement;
import fr.formation.banque.evenement.PublieurVirementPubSub;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DÉMO 10 — la version unitaire du même publieur, et ce qu'elle ne prouve pas.
 *
 * <p>Ce test s'exécute en quelques millisecondes, sans Docker, dans
 * {@code mvn test}. Il vérifie la seule chose qui relève vraiment du code de
 * cette classe : <b>la transformation {@code Virement} → {@code EvenementVirement}</b>,
 * et le fait que le topic configuré est bien celui qui est utilisé.
 *
 * <p><b>Ce qu'il ne peut pas prouver</b>, et qui a coûté une vraie erreur pendant
 * l'écriture de cette formation :
 * <ul>
 *   <li>que l'objet est <b>sérialisable</b> par Pub/Sub. Le mock accepte
 *       n'importe quel objet ; le vrai {@code PubSubTemplate} exigeait un
 *       {@code PubSubMessageConverter} JSON et échouait avec
 *       {@code PubSubMessageConversionException}. Seul
 *       {@link PublicationVirementIT} l'a révélé ;</li>
 *   <li>que le topic {@code virements-executes} existe réellement ;</li>
 *   <li>que le JSON produit est relisible par un autre service.</li>
 * </ul>
 *
 * <p><b>Conclusion pédagogique</b> : les deux tests sont utiles et se complètent.
 * Le test unitaire couvre la logique de transformation, vite et pour tous les
 * cas ; le test d'intégration couvre le branchement, une fois, sur le parcours
 * principal. Remplacer l'un par l'autre, c'est soit une suite lente, soit une
 * suite aveugle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublieurVirementPubSub — version unitaire (mock)")
class PublieurVirementPubSubTest {

    @Mock
    private PubSubTemplate pubSub;

    @Captor
    private ArgumentCaptor<EvenementVirement> evenementPublie;

    @Test
    @DisplayName("transforme le virement en événement et vise le topic configuré")
    void devrait_publier_l_evenement_sur_le_topic_configure() {
        PublieurVirementPubSub publieur = new PublieurVirementPubSub(pubSub,
                new ProprietesPubSub("topic-de-test", "souscription-de-test", false));
        Virement virement = new Virement("VIR-1", "FR76-SOURCE", "FR76-DEST",
                Montant.euros("1000.00"), Montant.euros("1.00"), LocalDate.of(2025, 6, 3));

        publieur.virementExecute(virement);

        verify(pubSub).publish(org.mockito.ArgumentMatchers.eq("topic-de-test"),
                evenementPublie.capture());
        assertThat(evenementPublie.getValue())
                .returns("VIR-1", EvenementVirement::reference)
                .returns("EUR", EvenementVirement::devise)
                .returns(LocalDate.of(2025, 6, 3), EvenementVirement::dateDeValeur);
        assertThat(evenementPublie.getValue().montant()).isEqualByComparingTo("1000.00");
        assertThat(evenementPublie.getValue().frais()).isEqualByComparingTo("1.00");
    }
}
