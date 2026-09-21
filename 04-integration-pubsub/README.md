# 04 — Intégration Google Cloud Pub/Sub

Émulateur Pub/Sub via Testcontainers. Flux sortant (publication) et flux entrant
(consommation), **avec le traitement métier mocké** : ce chapitre teste un
branchement, pas une chaîne. Les règles de virement sont testées au chapitre 01,
la persistance au chapitre 02, le contrat HTTP au chapitre 03.

## Code de production

| Élément | Rôle |
|---|---|
| `EvenementVirement` | charge utile JSON — **contrat inter-applications** : renommer un champ est une rupture |
| `PublieurVirementPubSub` | adaptateur sortant du port `NotificateurVirement` |
| `AbonneVirement` | adaptateur entrant : `ack` / `nack`, idempotence |
| `VirementRecuEntity` | journal d'idempotence (clé primaire = référence) |
| `InitialisationPubSub` | création du topic et de la souscription sur l'émulateur |
| `ConfigurationPubSub` | convertisseur JSON **obligatoire** |

## Tests

| Classe | Enseignement |
|---|---|
| `demo/PublieurVirementPubSubTest` | version unitaire (mock) — **et ses limites** |
| `demo/PublicationVirementIT` | flux sortant, souscription de contrôle, Awaitility |
| `demo/ConsommationVirementIT` | flux entrant, `ack`/`nack`, redélivrance |
| `exercice/FluxEvenementExerciceIT` | **exercice 5** — [fiche](../docs/exercices/05-flux-evenements.md) |
| `corrige/FluxEvenementCorrigeIT` | les deux sens du flux |
| `support/SocleIntegrationPubSub` | émulateur + les mocks qui délimitent le chapitre |

## Points clés

- Pas de `@ServiceConnection` pour Pub/Sub → **`@DynamicPropertySource`** sur
  `spring.cloud.gcp.pubsub.emulator-host`.
- Une souscription ne reçoit que les messages publiés **après** sa création.
- Une souscription distribue chaque message à **un seul** consommateur → le test
  a besoin de sa propre souscription de contrôle.
- Pub/Sub livre **au moins une fois** → le consommateur doit être idempotent.
- Prouver une absence : `await().during(...)` avant de conclure.
- **Frontière du test** : le traitement métier du flux entrant est un
  `@MockitoBean`. Ce que le test vérifie, c'est *qu'il est appelé*, avec quelle
  charge utile et combien de fois — pas ce qu'il écrit en base.
- `verify(...)` est **synchrone** : sur un flux asynchrone, il s'enveloppe dans
  un `await().untilAsserted(...)`.
- **`ack` / `nack`** : le message n'est acquitté qu'après un traitement réussi.
  Le test le prouve en faisant échouer le mock une fois, puis en vérifiant que le
  traitement est rappelé. Acquitter avant de traiter perdrait l'événement au
  premier incident — bug invisible tant que rien n'échoue.
- **Idempotence** : `existsById` puis `save` est un « vérifier-puis-agir ».
  Pub/Sub livre sur plusieurs threads, donc deux copies peuvent passer le
  contrôle ensemble ; la garantie réelle vient de la **clé primaire**, et se
  teste là où elle vit — au chapitre 02.
- **Un `@MockitoBean` de plus dans une classe de test = un second contexte
  Spring**, donc un second abonné en concurrence sur la même souscription. Les
  remplacements sont déclarés une fois, dans le socle.
- **Le cas d'école du chapitre** : le test unitaire avec `PubSubTemplate` mocké
  passait, alors que la publication réelle échouait faute de convertisseur JSON
  (`PubSubMessageConversionException`). Un mock accepte n'importe quel objet.

## Lancer

```bash
./mvnw -pl 04-integration-pubsub -am test
```
