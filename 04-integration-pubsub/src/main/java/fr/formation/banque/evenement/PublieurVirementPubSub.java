package fr.formation.banque.evenement;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import fr.formation.banque.configuration.ProprietesPubSub;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.Virement;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : implémente le port {@link NotificateurVirement} du
 * domaine au-dessus de Google Cloud Pub/Sub.
 *
 * <p>Le domaine (chapitre 01) ignore jusqu'à l'existence de Pub/Sub : il appelle
 * un port. C'est ce qui permet de tester unitairement la règle de virement en
 * quelques millisecondes, puis de tester <b>ici seulement</b> la partie qui a
 * réellement besoin d'un broker.
 *
 * <p>{@code publish} est asynchrone et rend la main immédiatement. Cette classe
 * n'attend pas la confirmation : c'est ce qui rend les tests d'intégration
 * sensibles au temps — et impose Awaitility plutôt que {@code Thread.sleep}
 * (anti-pattern 3).
 */
@Component
public class PublieurVirementPubSub implements NotificateurVirement {

    private final PubSubTemplate pubSub;
    private final ProprietesPubSub proprietes;

    public PublieurVirementPubSub(PubSubTemplate pubSub, ProprietesPubSub proprietes) {
        this.pubSub = pubSub;
        this.proprietes = proprietes;
    }

    @Override
    public void virementExecute(Virement virement) {
        // Le convertisseur Jackson configuré par spring-cloud-gcp sérialise
        // l'objet en JSON. Ce que vérifie le test d'intégration : que la
        // sérialisation fonctionne réellement, avec les bons noms de champs.
        pubSub.publish(proprietes.topicSortant(), EvenementVirement.depuis(virement));
    }
}
