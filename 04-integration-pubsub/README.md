# 04 — Intégration Google Cloud Pub/Sub

Émulateur Pub/Sub via Testcontainers. Flux sortant (publication) et flux entrant
(consommation), avec la question de l'idempotence.

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
| `demo/ConsommationVirementIT` | flux entrant, redélivrance, idempotence |
| `exercice/VirementCompletExerciceIT` | **exercice 5** — [fiche](../docs/exercices/05-chaine-complete.md) |
| `corrige/VirementCompletCorrigeIT` | HTTP → PostgreSQL → Pub/Sub |

## Points clés

- Pas de `@ServiceConnection` pour Pub/Sub → **`@DynamicPropertySource`** sur
  `spring.cloud.gcp.pubsub.emulator-host`.
- Une souscription ne reçoit que les messages publiés **après** sa création.
- Une souscription distribue chaque message à **un seul** consommateur → le test
  a besoin de sa propre souscription de contrôle.
- Pub/Sub livre **au moins une fois** → le consommateur doit être idempotent.
- Prouver une absence : `await().during(...)` avant de conclure.
- **Idempotence** : `existsById` puis `save` est un « vérifier-puis-agir ».
  Pub/Sub livre sur plusieurs threads, donc deux copies peuvent passer le
  contrôle ensemble. La garantie vient de la **clé primaire** ; l'exception
  qu'elle lève est traitée comme un doublon, pas comme une erreur.
- **Le cas d'école du chapitre** : le test unitaire avec `PubSubTemplate` mocké
  passait, alors que la publication réelle échouait faute de convertisseur JSON
  (`PubSubMessageConversionException`). Un mock accepte n'importe quel objet.

## Lancer

```bash
./mvnw -pl 04-integration-pubsub -am verify
```
