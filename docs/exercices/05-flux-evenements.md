# Exercice 5 — Les deux sens du flux d'événements

> Module `04-integration-pubsub` · **60 min** · **Docker requis** (émulateur Pub/Sub)

## Objectif

Tester une frontière de messagerie **dans les deux sens** — émission et
réception — avec un vrai broker, et **le traitement métier mocké**.

**Compétence visée** : attendre une condition et jamais une durée, prouver une
**absence** d'effet aussi rigoureusement qu'une présence, et placer la frontière
du test au bon endroit — l'adaptateur, pas la chaîne entière.

## À lire d'abord

- [Fiche 3 — un bon test d'intégration](../03-bon-test-integration.md), section
  *Attendre une condition, jamais une durée*
- [Fiche 4 — Testcontainers](../04-testcontainers.md), section
  *`@DynamicPropertySource`*
- `04-integration-pubsub/.../demo/PublicationVirementIT.java` — **la démo clé** :
  souscription de contrôle + Awaitility
- `04-integration-pubsub/.../demo/ConsommationVirementIT.java` — le sens entrant
- `04-integration-pubsub/.../support/SocleIntegrationPubSub.java` — l'émulateur,
  et les mocks qui délimitent le chapitre
- `00-antipatterns/.../AntiPattern03ThreadSleepTest.java` — pourquoi pas `sleep`

## Énoncé

Ce chapitre teste **un branchement, pas une chaîne**. Deux questions, deux tests :

| # | Sens | Déclencheur | Ce qu'on vérifie | Nature |
|---|---|---|---|---|
| 1 | sortant | appel du port `NotificateurVirement` | un message correctement sérialisé arrive sur le topic | **asynchrone** → Awaitility |
| 2 | entrant | publication d'un message | le traitement mocké est appelé avec la bonne charge utile | **asynchrone** → Awaitility |
| 3 | entrant | le traitement lève une exception | le message n'est pas perdu : il est redélivré | **asynchrone** → Awaitility |

Ce qui est **volontairement hors périmètre** : les règles de virement
(chapitre 01), la persistance (chapitre 02), le contrat HTTP (chapitre 03). Le
traitement métier du flux entrant est un `@MockitoBean` déclaré une fois pour
toutes dans le socle — c'est lui qui matérialise la frontière.

## Étapes

1. Ouvrir `exercice/FluxEvenementExerciceIT.java`, retirer le `@Disabled`.
2. Dans le `@BeforeEach` : créer la souscription de contrôle sur le topic
   `virements-executes` si elle n'existe pas, puis la **vider** (pull + ack).
3. Écrire le test sortant : appeler le port, attendre le message, asserter son
   **contenu** champ par champ.
4. Écrire le test entrant : publier un événement, attendre que `journal.save(...)`
   soit appelé, et asserter l'objet capturé.
5. Écrire le troisième test : stuber `journal.save(...)` pour qu'il échoue une
   fois, puis vérifier que le traitement est rappelé.

```bash
./mvnw -pl 04-integration-pubsub -am test
```

## ⚠️ Les quatre pièges du chapitre

1. **Souscription créée trop tard.** Une souscription ne reçoit que les messages
   publiés **après** sa création. Créée dans le corps du test plutôt que dans le
   `@BeforeEach`, elle ne recevra jamais rien — et le test attendra jusqu'au
   timeout d'Awaitility.
2. **Consommateur concurrent.** L'application consomme déjà
   `virements-executes-journal`. Une souscription distribue chaque message à
   **un seul** consommateur : sans souscription de contrôle dédiée, le test et
   l'application se disputent le message.
3. **Un `@MockitoBean` de plus dans votre classe.** Le jeu de remplacements fait
   partie de la clé du cache de contexte Spring : en ajouter un crée un
   **second contexte**, donc un second abonné, donc deux consommateurs en
   concurrence sur la même souscription — et un test intermittent. Utilisez les
   mocks hérités du socle.
4. **`Thread.sleep`.** Calibré sur votre poste, il sera trop court sur la machine
   d'intégration continue et trop long partout. `await().atMost(...).untilAsserted(...)`
   rend la main dès que la condition est vraie.

## Checklist des cas attendus

**Sens sortant**

- [ ] la souscription de contrôle est créée dans le `@BeforeEach`, **avant** l'appel
- [ ] elle est vidée avant chaque test (isolation)
- [ ] l'appel se fait par le **port** `NotificateurVirement`, pas par la classe
      concrète : le test ignore Pub/Sub, comme le domaine
- [ ] l'événement est attendu avec Awaitility, pas avec `sleep`
- [ ] le **contenu** de l'événement est asséré (référence, IBAN, montant, frais),
      pas seulement sa présence

**Sens entrant**

- [ ] `verify(journal).save(...)` est enveloppé dans un `await().untilAsserted(...)`
- [ ] l'objet passé au traitement est capturé (`ArgumentCaptor`) et asséré champ
      par champ — c'est la preuve que le JSON a été relu correctement
- [ ] le cas d'échec : `save` lève une fois, et le traitement est bien rappelé
- [ ] bonus : `existsById` renvoie `true` → `save` n'est **jamais** appelé,
      vérifié avec `await().during(...)`

## Vérification par sabotage

| # | Fichier | Modification | Ce qui doit devenir rouge |
|---|---|---|---|
| 1 | `ConfigurationPubSub.java` | commenter le bean `convertisseurJson` | la publication échoue (`PubSubMessageConversionException`) — donc le sens sortant |
| 2 | `AbonneVirement.java` | déplacer `message.ack()` **avant** `enregistrer(...)` | le test du traitement en échec : le message est acquitté puis perdu, jamais redélivré |
| 3 | `PublieurVirementPubSub.java` | commenter l'appel `pubSub.publish(...)` | le sens sortant |
| 4 | `EvenementVirement.java` | renommer le champ `reference` en `ref` | le contenu de l'événement, dans les deux sens — c'est une **rupture de contrat** inter-applications |

**Le n°1 est le cas d'école du chapitre** : relancez la classe unitaire
`PublieurVirementPubSubTest` avec ce sabotage en place — elle reste **verte**,
parce qu'un `PubSubTemplate` mocké accepte n'importe quel objet. Seul le test
d'intégration voit le bug.

Le n°2 est le plus instructif sur le flux entrant : acquitter avant de traiter
est un bug fréquent, invisible tant que rien n'échoue — et qui fait disparaître
des messages en production au premier incident.

Les n°3 et 4 cassent aussi des tests unitaires : c'est normal, et c'est la
preuve que le bug est détecté au niveau le moins cher. `*Test` et `*IT` tournant
dans la même commande, le rapport montre les deux d'un coup — comparez le message
d'échec du test unitaire à celui du test d'intégration.

Restaurer ensuite :

```bash
git checkout -- 04-integration-pubsub/src/main
```

## Indices

<details>
<summary>Indice 1 — préparer la souscription de contrôle</summary>

`PubSubAdmin` est déjà injecté. Créer la souscription si elle n'existe pas, puis
la vider des messages laissés par le test précédent :

```java
if (admin.getSubscription(SOUSCRIPTION_DE_CONTROLE) == null) {
    admin.createSubscription(SOUSCRIPTION_DE_CONTROLE, "virements-executes");
}
pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 100, true)
        .forEach(AcknowledgeablePubsubMessage::ack);
```

Le troisième argument `true` de `pull` signifie « ne pas bloquer s'il n'y a rien ».
</details>

<details>
<summary>Indice 2 — attendre l'événement (sens sortant)</summary>

```java
await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
    var messages = pubSub.pullAndConvert(
            SOUSCRIPTION_DE_CONTROLE, 10, true, EvenementVirement.class);

    assertThat(messages).hasSize(1);
    EvenementVirement evenement = messages.get(0).getPayload();
    assertThat(evenement.montant()).isEqualByComparingTo("1000.00");
    // ...
});
```

`untilAsserted` réessaie tant que les assertions échouent, et rend la main dès
qu'elles passent. Voir `PublicationVirementIT` pour la version complète.
</details>

<details>
<summary>Indice 3 — vérifier un appel (sens entrant)</summary>

Le `verify` de Mockito est **synchrone** : l'appeler juste après `publish` le
ferait échouer, le message n'étant pas encore arrivé. Il faut l'envelopper dans
l'attente :

```java
ArgumentCaptor<VirementRecuEntity> recu = ArgumentCaptor.forClass(VirementRecuEntity.class);
await().atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> verify(journal).save(recu.capture()));

assertThat(recu.getValue().reference()).isEqualTo("VIR-EX-5-ENTRANT");
```

Et pour prouver une **absence** d'appel, `atMost` ne convient pas : il
réussirait immédiatement, avant même que le message ait eu le temps d'arriver.
Il faut laisser du temps s'écouler et vérifier que la condition reste vraie :

```java
await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(15))
        .untilAsserted(() -> verify(journal, never()).save(any()));
```

C'est la différence entre « rien n'est encore arrivé » et « rien n'arrivera ».
</details>

<details>
<summary>Indice 4 — faire échouer le traitement</summary>

```java
when(journal.save(any()))
        .thenThrow(new IllegalStateException("panne simulée"))
        .thenReturn(null);
```

Le premier appel échoue, le second réussit : l'abonné fait un `nack`, Pub/Sub
redélivre, et le second passage acquitte. Sans le `thenReturn`, le message
tournerait en boucle jusqu'au timeout.
</details>

## Questions de fin

1. Comparer la durée de cette classe à celle de `PublieurVirementPubSubTest`
   (unitaire, mock). Quel rapport ?
2. `PublieurVirementPubSubTest` aurait-il détecté l'absence du convertisseur JSON
   (`PubSubMessageConversionException`) ? Pourquoi ? (Refaites le sabotage n°1 et
   relancez la classe unitaire pour vérifier votre réponse.)
3. Le traitement métier est mocké ici. Quel bug resterait donc invisible dans
   **tout** le projet, et que faudrait-il ajouter pour l'attraper — à quel coût ?
4. Combien de tests de ce niveau une équipe peut-elle raisonnablement maintenir
   dans une suite exécutée à chaque commit ?

## Corrigé

`04-integration-pubsub/src/test/java/fr/formation/banque/integrationpubsub/corrige/FluxEvenementCorrigeIT.java`
