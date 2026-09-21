# 4. Testcontainers en pratique

## Principe

Testcontainers démarre de vrais services dans Docker, le temps des tests, sur des
ports aléatoires. Le prix à payer est le temps de démarrage ; l'essentiel du
travail consiste à ne le payer qu'une fois.

## Deux façons de brancher un conteneur

### `@ServiceConnection` — quand Spring Boot connaît la technologie

```java
@TestConfiguration(proxyBeanMethods = false)
public class ConfigurationPostgres {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> conteneurPostgres() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"));
    }
}
```

Spring Boot lit l'hôte, le port aléatoire, la base et les identifiants, puis
configure la `DataSource`. **Aucune propriété à écrire.** Fonctionne pour
PostgreSQL, MySQL, Redis, Kafka, MongoDB, RabbitMQ, Elasticsearch…

### `@DynamicPropertySource` — pour tout le reste

```java
@DynamicPropertySource
static void configurer(DynamicPropertyRegistry proprietes) {
    proprietes.add("spring.cloud.gcp.pubsub.emulator-host", EMULATEUR::getEmulatorEndpoint);
}
```

Pub/Sub vient de `spring-cloud-gcp`, qui ne publie pas de `ConnectionDetails` :
il faut positionner la propriété soi-même. C'est le mécanisme générique, à
connaître pour toute technologie non couverte.

## Ne démarrer qu'une fois

### Le piège

```java
@Testcontainers
class MonTestIT {
    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(...);
}
```

JUnit **arrête** le conteneur à la fin de la classe. Avec dix classes de test, on
paie dix démarrages.

### Solution A — le conteneur comme bean (chapitre 02)

Spring met en cache le contexte entre les classes de test : toutes celles qui
importent la même `@TestConfiguration` partagent le même conteneur.

⚠️ Deux contextes **différents** (un `@DataJpaTest` et un `@SpringBootTest`) sont
deux caches différents, donc deux conteneurs. **Limiter le nombre de
configurations de test distinctes est une optimisation de premier ordre.**

### Solution B — le singleton statique (chapitre 04)

```java
public abstract class SocleIntegrationPubSub {
    protected static final PubSubEmulatorContainer EMULATEUR = new PubSubEmulatorContainer(...);

    static { EMULATEUR.start(); }
}
```

Ni `@Testcontainers` ni `@Container` : JUnit n'y touche pas. Le conteneur vit le
temps de la JVM de test, et Ryuk (le conteneur de nettoyage de Testcontainers) le
supprime à la fin.

⚠️ **Ne démarrer que ce dont le test a besoin.** Le chapitre 04 n'a pas de
conteneur PostgreSQL : ses tests s'arrêtent à la frontière de messagerie et
mockent le traitement métier. Ajouter une base « au cas où » coûte quelques
secondes à chaque exécution et brouille la question à laquelle le test répond.

## Réutilisation entre deux lancements

Sur un poste de développement :

```java
new PostgreSQLContainer<>(IMAGE).withReuse(true)
```

```properties
# ~/.testcontainers.properties
testcontainers.reuse.enable=true
```

Le conteneur survit à la fin du build : gain de 2 à 3 secondes par lancement.
**À réserver au poste de développement** — en intégration continue on veut une
base neuve et aucune trace résiduelle.

## Diagnostic

| Symptôme | Cause probable |
|---|---|
| `Could not find a valid Docker environment` | démon Docker arrêté |
| Le conteneur démarre mais n'est pas utilisé | `@AutoConfigureTestDatabase(replace = NONE)` manquant sur `@DataJpaTest` |
| `Schema validation: missing table` | Flyway n'a pas tourné (autoconfiguration absente) |
| Aucun message reçu sur Pub/Sub | souscription créée **après** la publication |
| Un seul test sur deux reçoit le message | deux consommateurs sur la même souscription |
| Premier `test` très long | téléchargement des images |

## Images utilisées

| Service | Image | Taille approximative |
|---|---|---|
| PostgreSQL | `postgres:17-alpine` | ~80 Mo |
| Émulateur Pub/Sub | `gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators` | ~1 Go |

Prévoir le premier téléchargement avant la session de formation :

```bash
docker pull postgres:17-alpine
docker pull gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators
```
