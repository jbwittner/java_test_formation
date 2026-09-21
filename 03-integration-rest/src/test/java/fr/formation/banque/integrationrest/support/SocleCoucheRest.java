package fr.formation.banque.integrationrest.support;

import fr.formation.banque.domaine.CompteRepository;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Socle des tests sur serveur HTTP réel : l'application démarre sur un vrai
 * port, <b>sans base de données</b>.
 *
 * <p><b>Le périmètre testé est la couche REST, et rien en dessous.</b> Le
 * contrôleur n'a qu'un seul collaborateur — {@code ServiceVirement} — que chaque
 * classe de test remplace par un {@code @MockitoBean}. Ce qui est exercé, c'est
 * la traversée HTTP complète : socket, désérialisation JSON, validation,
 * routage, {@code @RestControllerAdvice}, sérialisation de la réponse. Pas les
 * règles métier ni la persistance, déjà couvertes aux chapitres 01 et 02.
 *
 * <p><b>Pourquoi exclure l'auto-configuration JDBC/JPA/Flyway ?</b> Le module
 * dépend de {@code 02-integration-bdd}, donc {@code spring-boot-starter-data-jpa}
 * est sur le classpath : sans ces exclusions, Spring Boot réclamerait une
 * {@code DataSource} au démarrage et il faudrait un conteneur PostgreSQL pour un
 * test qui ne touche jamais la base. Les exclure rend le contexte de test
 * <b>honnête</b> : impossible d'écrire ici, par inadvertance, une assertion sur
 * l'état persisté.
 *
 * <p><b>Noms de classes Spring Boot 4</b> : les auto-configurations ont été
 * réparties dans des modules dédiés, les packages ne sont plus ceux de Boot 3.
 * <pre>
 * Boot 3 : org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
 * Boot 4 : org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
 * </pre>
 *
 * <p><b>Et le bout en bout avec une vraie base ?</b> Il a sa place — au chapitre
 * 02 pour la persistance, au chapitre 04 pour la messagerie. Le dupliquer ici
 * coûterait un conteneur PostgreSQL par exécution sans rien prouver que ces
 * chapitres ne prouvent déjà.
 *
 * <p><b>⚠️ {@code @AutoConfigureRestTestClient} est obligatoire.</b> En Boot 3, un
 * {@code TestRestTemplate} était injectable dès que {@code webEnvironment} valait
 * {@code RANDOM_PORT}. En Boot 4, le bean {@code RestTestClient} n'est créé que si
 * cette annotation est présente ; sans elle, l'erreur est un laconique
 * « No qualifying bean of type RestTestClient available ».
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = """
                spring.autoconfigure.exclude=\
                org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,\
                org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,\
                org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,\
                org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration""")
@AutoConfigureRestTestClient
public abstract class SocleCoucheRest {

    /**
     * Plomberie, et rien d'autre : ce dépôt n'est <b>jamais</b> stubé ni vérifié
     * par les tests. Le contrôleur ne le connaît pas — il passe par
     * {@code ServiceVirement}. Mais l'application le câble derrière le service,
     * et son implémentation JPA réclamerait une {@code DataSource} au démarrage.
     * Le remplacer ici, une fois, garde les classes de test concentrées sur leur
     * seul vrai collaborateur.
     */
    @MockitoBean
    protected CompteRepository comptes;
}
