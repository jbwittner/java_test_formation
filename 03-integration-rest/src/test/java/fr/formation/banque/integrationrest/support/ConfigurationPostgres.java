package fr.formation.banque.integrationrest.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Le conteneur PostgreSQL, déclaré comme un <b>bean Spring</b>.
 *
 * <p><b>{@code @ServiceConnection} est le point clé</b> : Spring Boot lit les
 * coordonnées du conteneur démarré (hôte, port aléatoire, base, identifiants) et
 * configure la {@code DataSource} tout seul. Aucun {@code spring.datasource.url}
 * à écrire, aucun {@code @DynamicPropertySource}, aucun port figé dans le code.
 * C'est la différence majeure avec l'approche « historique » de Testcontainers.
 *
 * <p><b>Pourquoi un bean plutôt qu'un champ {@code @Container} statique ?</b>
 * Spring met en cache le contexte d'application entre les classes de test :
 * toutes les classes qui importent cette configuration <b>partagent le même
 * conteneur</b>, démarré une seule fois. Avec {@code @Testcontainers} et un
 * champ statique, JUnit arrête le conteneur à la fin de chaque classe — on paie
 * alors 2 à 3 secondes de démarrage par classe de test.
 *
 * <p>Corollaire à connaître : deux contextes <b>différents</b> (par exemple un
 * {@code @DataJpaTest} et un {@code @SpringBootTest}) sont deux caches
 * différents, donc deux conteneurs. Plus une suite de tests utilise de
 * configurations distinctes, plus elle démarre de contextes — et plus elle est
 * lente. Limiter le nombre de configurations de test est une optimisation de
 * premier ordre.
 *
 * <p><b>Réutilisation entre deux lancements</b> : ajouter {@code .withReuse(true)}
 * et activer {@code testcontainers.reuse.enable=true} dans
 * {@code ~/.testcontainers.properties} garde le conteneur vivant entre deux
 * exécutions de Maven. Gain typique : 2 à 3 secondes par lancement. À réserver
 * au poste de développement — jamais en intégration continue, où l'on veut une
 * base neuve et une empreinte propre.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfigurationPostgres {

    /** Version d'image FIGÉE : « postgres:latest » rendrait le build non reproductible. */
    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:17-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> conteneurPostgres() {
        return new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName("banque")
                .withUsername("banque")
                .withPassword("banque");
    }
}
