package fr.formation.banque.integrationpubsub.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Socle commun aux tests d'intégration du chapitre : une PostgreSQL et un
 * émulateur Pub/Sub, démarrés une seule fois pour toute la JVM.
 *
 * <p><b>Pourquoi {@code @DynamicPropertySource} et non {@code @ServiceConnection} ?</b>
 * Spring Boot fournit des {@code ConnectionDetails} pour les technologies qu'il
 * connaît (PostgreSQL, Redis, Kafka…). Pub/Sub vient de {@code spring-cloud-gcp},
 * qui n'en publie pas : il faut donc positionner soi-même
 * {@code spring.cloud.gcp.pubsub.emulator-host} à partir du port aléatoire du
 * conteneur. C'est le mécanisme générique, à connaître pour toute technologie
 * non couverte par {@code @ServiceConnection}.
 *
 * <p>Ici, PostgreSQL est branché de la même façon, par cohérence : les deux
 * conteneurs sont configurés au même endroit et la lecture reste simple.
 *
 * <p><b>Patron « conteneur singleton »</b> : les conteneurs sont des champs
 * {@code static} démarrés dans un bloc statique, <b>sans</b> {@code @Testcontainers}
 * ni {@code @Container}. JUnit ne les arrête donc jamais entre deux classes — ils
 * vivent tant que la JVM de test vit, et Ryuk (le conteneur de nettoyage de
 * Testcontainers) les supprime à la fin. C'est ce qui évite de payer plusieurs
 * secondes de démarrage par classe de test.
 *
 * <p>L'image de l'émulateur pèse plusieurs centaines de méga-octets : le premier
 * {@code mvn verify} d'un poste inclut son téléchargement.
 */
public abstract class SocleIntegrationPubSub {

    // Versions figées : « latest » rendrait le build non reproductible.
    private static final DockerImageName IMAGE_POSTGRES =
            DockerImageName.parse("postgres:17-alpine");
    private static final DockerImageName IMAGE_EMULATEUR =
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators");

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(IMAGE_POSTGRES)
                    .withDatabaseName("banque")
                    .withUsername("banque")
                    .withPassword("banque");

    protected static final PubSubEmulatorContainer EMULATEUR =
            new PubSubEmulatorContainer(IMAGE_EMULATEUR);

    static {
        POSTGRES.start();
        EMULATEUR.start();
    }

    @DynamicPropertySource
    static void configurer(DynamicPropertyRegistry proprietes) {
        proprietes.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        proprietes.add("spring.datasource.username", POSTGRES::getUsername);
        proprietes.add("spring.datasource.password", POSTGRES::getPassword);

        // LE point de branchement de l'émulateur. Tant que cette propriété est
        // renseignée, spring-cloud-gcp parle en clair au conteneur local et
        // n'exige aucune authentification Google.
        proprietes.add("spring.cloud.gcp.pubsub.emulator-host", EMULATEUR::getEmulatorEndpoint);

        // Chaque exécution travaille sur ses propres ressources, créées au
        // démarrage : aucun test ne dépend d'un topic préexistant.
        proprietes.add("banque.pubsub.creer-au-demarrage", () -> true);
    }
}
