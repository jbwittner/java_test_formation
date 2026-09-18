package fr.formation.banque.evenement;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import fr.formation.banque.configuration.InitialisationPubSub;
import fr.formation.banque.configuration.ProprietesPubSub;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Adaptateur entrant : consomme les événements de virement et les journalise.
 *
 * <p><b>Idempotence</b> : Pub/Sub garantit une livraison <i>au moins une fois</i>.
 * Un message peut donc être livré deux fois — sur un nack, sur un délai d'accusé
 * de réception dépassé, ou lors d'un redémarrage. Si le traitement n'est pas
 * idempotent, l'effet métier est appliqué deux fois. Ici, la référence sert de
 * clé primaire et le second passage est ignoré.
 *
 * <p><b>Le contrôle d'existence ne suffit pas.</b> {@code existsById} puis
 * {@code save} est un « vérifier-puis-agir » : Pub/Sub livre sur plusieurs
 * threads, et deux copies du même message peuvent passer le contrôle en même
 * temps avant que l'une n'insère. C'est exactement ce qui se produit dans
 * {@code ConsommationVirementIT}. La garantie réelle vient donc de la
 * <b>contrainte de clé primaire</b>, et l'exception qu'elle lève est traitée
 * comme un doublon — pas comme une erreur.
 *
 * <p>Enseignement transposable : une règle d'unicité vérifiée uniquement en
 * Java est une règle non garantie. Seule la base sait dire non de façon atomique
 * — et seul un test d'intégration peut le démontrer.
 *
 * <p><b>Accusé de réception</b> : le message est acquitté ({@code ack}) seulement
 * après un traitement réussi, et refusé ({@code nack}) en cas d'échec pour que
 * Pub/Sub le redélivre. Acquitter avant de traiter perdrait le message à la
 * première erreur — un bug fréquent, que seul un test d'intégration peut exposer.
 *
 * <p>Le constructeur dépend d'{@link InitialisationPubSub} uniquement pour
 * l'<b>ordre de création</b> des beans : la souscription doit exister avant qu'on
 * s'y abonne.
 */
@Component
public class AbonneVirement implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbonneVirement.class);

    private final PubSubTemplate pubSub;
    private final ProprietesPubSub proprietes;
    private final VirementRecuRepository journal;
    private final Clock horloge;

    public AbonneVirement(PubSubTemplate pubSub,
                          ProprietesPubSub proprietes,
                          VirementRecuRepository journal,
                          Clock horloge,
                          InitialisationPubSub initialisation) {
        this.pubSub = pubSub;
        this.proprietes = proprietes;
        this.journal = journal;
        this.horloge = horloge;
    }

    @Override
    public void afterPropertiesSet() {
        pubSub.subscribeAndConvert(
                proprietes.souscriptionEntrante(), this::traiter, EvenementVirement.class);
    }

    private void traiter(ConvertedBasicAcknowledgeablePubsubMessage<EvenementVirement> message) {
        try {
            enregistrer(message.getPayload());
            message.ack();
        } catch (RuntimeException erreur) {
            // nack : Pub/Sub redélivrera. Sans cela, un incident transitoire
            // (base indisponible) ferait disparaître l'événement définitivement.
            LOG.warn("Traitement en échec, message refusé pour redélivrance", erreur);
            message.nack();
        }
    }

    void enregistrer(EvenementVirement evenement) {
        if (journal.existsById(evenement.reference())) {
            // Chemin rapide : redélivrance déjà traitée, aucun accès en écriture.
            LOG.debug("Événement {} déjà journalisé, ignoré", evenement.reference());
            return;
        }
        try {
            journal.save(new VirementRecuEntity(
                    evenement.reference(),
                    evenement.ibanSource(),
                    evenement.ibanDestination(),
                    evenement.montant(),
                    Instant.now(horloge)));
        } catch (DataIntegrityViolationException doublon) {
            // Deux livraisons concurrentes ont passé le contrôle d'existence
            // ensemble. La clé primaire a tranché : le message est un doublon,
            // donc traité. On l'acquitte au lieu de le refuser — un nack ici
            // relancerait indéfiniment le même conflit.
            LOG.debug("Événement {} inséré en parallèle, doublon ignoré", evenement.reference());
        }
    }
}
