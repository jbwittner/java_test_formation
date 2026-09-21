package fr.formation.banque.integrationpubsub.support;

import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.evenement.VirementRecuRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Socle commun aux tests d'intégration du chapitre : un émulateur Pub/Sub, et
 * <b>rien d'autre de réel en dessous</b>.
 *
 * <p><b>Le périmètre testé est la frontière de messagerie.</b> Ce chapitre
 * répond à deux questions, et à deux questions seulement :
 * <ul>
 *   <li><b>sortant</b> — quand l'application doit émettre, un message
 *       correctement sérialisé part-il sur le bon topic ?</li>
 *   <li><b>entrant</b> — quand un message arrive, le traitement métier est-il
 *       déclenché avec la bonne charge utile, et le message acquitté ?</li>
 * </ul>
 * Ce que fait le métier ensuite est mocké : les règles de virement sont testées
 * au chapitre 01, la persistance au chapitre 02, le contrat HTTP au chapitre 03.
 * Rejouer ici ces couches coûterait un conteneur PostgreSQL et des secondes
 * d'attente sans rien prouver de plus.
 *
 * <p><b>Pourquoi {@code @DynamicPropertySource} et non {@code @ServiceConnection} ?</b>
 * Spring Boot fournit des {@code ConnectionDetails} pour les technologies qu'il
 * connaît (PostgreSQL, Redis, Kafka…). Pub/Sub vient de {@code spring-cloud-gcp},
 * qui n'en publie pas : il faut donc positionner soi-même
 * {@code spring.cloud.gcp.pubsub.emulator-host} à partir du port aléatoire du
 * conteneur. C'est le mécanisme générique, à connaître pour toute technologie
 * non couverte par {@code @ServiceConnection}.
 *
 * <p><b>Patron « conteneur singleton »</b> : le conteneur est un champ
 * {@code static} démarré dans un bloc statique, <b>sans</b> {@code @Testcontainers}
 * ni {@code @Container}. JUnit ne l'arrête donc jamais entre deux classes — il
 * vit tant que la JVM de test vit, et Ryuk (le conteneur de nettoyage de
 * Testcontainers) le supprime à la fin. C'est ce qui évite de payer plusieurs
 * secondes de démarrage par classe de test.
 *
 * <p>L'image de l'émulateur pèse plusieurs centaines de méga-octets : le premier
 * {@code mvn verify} d'un poste inclut son téléchargement.
 */
public abstract class SocleIntegrationPubSub {

    // Version figée : « latest » rendrait le build non reproductible.
    private static final DockerImageName IMAGE_EMULATEUR =
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators");

    protected static final PubSubEmulatorContainer EMULATEUR =
            new PubSubEmulatorContainer(IMAGE_EMULATEUR);

    static {
        EMULATEUR.start();
    }

    /**
     * La persistance des comptes n'est pas le sujet de ce chapitre, mais
     * l'application la câble : sans ce mock, le contexte réclamerait une
     * {@code DataSource}. Le remplacer ici, une fois, évite de le répéter dans
     * chaque classe de test.
     */
    @MockitoBean
    protected CompteRepository comptes;

    /**
     * <b>La frontière du flux entrant.</b> {@code AbonneVirement} reçoit le
     * message, le désérialise, puis délègue le traitement métier à ce dépôt : le
     * mocker, c'est poser exactement la question du chapitre — « la réception
     * déclenche-t-elle le bon appel, avec la bonne charge utile ? » — sans rien
     * exécuter en dessous.
     *
     * <p>Déclaré ici, et <b>ici seulement</b> : deux classes de test qui
     * déclareraient des remplacements différents auraient deux contextes Spring
     * distincts, donc deux abonnés en concurrence sur la même souscription — et
     * des tests intermittents.
     */
    @MockitoBean
    protected VirementRecuRepository journal;

    @DynamicPropertySource
    static void configurer(DynamicPropertyRegistry proprietes) {
        // LE point de branchement de l'émulateur. Tant que cette propriété est
        // renseignée, spring-cloud-gcp parle en clair au conteneur local et
        // n'exige aucune authentification Google.
        proprietes.add("spring.cloud.gcp.pubsub.emulator-host", EMULATEUR::getEmulatorEndpoint);

        // Chaque exécution travaille sur ses propres ressources, créées au
        // démarrage : aucun test ne dépend d'un topic préexistant.
        proprietes.add("banque.pubsub.creer-au-demarrage", () -> true);

        // Aucune base de données dans ce chapitre. Le module dépend pourtant du
        // chapitre 02, donc spring-boot-starter-data-jpa est sur le classpath :
        // sans ces exclusions, Spring Boot réclamerait une DataSource au
        // démarrage et il faudrait un conteneur PostgreSQL pour un test qui ne
        // touche jamais la base. Les exclure rend le contexte HONNÊTE — il
        // devient impossible d'asserter ici sur l'état persisté par mégarde.
        //
        // ⚠️ Spring Boot 4 : les auto-configurations ont été réparties dans des
        // modules dédiés, les packages ne sont plus ceux de Boot 3.
        //   Boot 3 : org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
        //   Boot 4 : org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
        proprietes.add("spring.autoconfigure.exclude", () -> String.join(",",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"));
    }
}
