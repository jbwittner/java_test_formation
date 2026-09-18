package fr.formation.banque.configuration;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Crée le topic et la souscription au démarrage, quand la configuration le demande.
 *
 * <p>Sur l'émulateur Pub/Sub, rien n'existe au lancement : publier sur un topic
 * absent échoue, s'abonner à une souscription absente aussi. Cette classe rend
 * le démarrage déterministe, en test comme en développement local.
 *
 * <p>En production, {@code creerAuDemarrage} vaut {@code false} : les ressources
 * sont provisionnées par l'infrastructure, et l'application n'a pas les droits
 * — ni la légitimité — pour les créer.
 */
@Component
public class InitialisationPubSub implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(InitialisationPubSub.class);

    private final PubSubAdmin admin;
    private final ProprietesPubSub proprietes;

    public InitialisationPubSub(PubSubAdmin admin, ProprietesPubSub proprietes) {
        this.admin = admin;
        this.proprietes = proprietes;
    }

    @Override
    public void afterPropertiesSet() {
        if (!proprietes.creerAuDemarrage()) {
            return;
        }
        if (admin.getTopic(proprietes.topicSortant()) == null) {
            admin.createTopic(proprietes.topicSortant());
            LOG.info("Topic créé : {}", proprietes.topicSortant());
        }
        if (admin.getSubscription(proprietes.souscriptionEntrante()) == null) {
            admin.createSubscription(proprietes.souscriptionEntrante(), proprietes.topicSortant());
            LOG.info("Souscription créée : {}", proprietes.souscriptionEntrante());
        }
    }
}
