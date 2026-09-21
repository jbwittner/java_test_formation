# Exercice 5 — La chaîne complète : HTTP → PostgreSQL → Pub/Sub

> Module `04-integration-pubsub` · **60 min** · **Docker requis**

## Objectif

Vérifier les trois effets d'un seul appel HTTP — réponse, base, message publié —
en traitant correctement le fait que le troisième est **asynchrone**.

**Compétence visée** : attendre une condition et jamais une durée, et prouver
une **absence** d'effet aussi rigoureusement qu'une présence.

## À lire d'abord

- [Fiche 3 — un bon test d'intégration](../03-bon-test-integration.md), section
  *Attendre une condition, jamais une durée*
- [Fiche 4 — Testcontainers](../04-testcontainers.md), section
  *`@DynamicPropertySource`*
- `04-integration-pubsub/.../demo/PublicationVirementIT.java` — **la démo clé** :
  souscription de contrôle + Awaitility
- `00-antipatterns/.../AntiPattern03ThreadSleepTest.java` — pourquoi pas `sleep`

## Énoncé

Un `POST /api/virements` doit produire trois effets, à vérifier différemment :

| # | Effet | Nature | Comment le vérifier |
|---|---|---|---|
| 1 | réponse 201 avec les frais calculés | synchrone | assertion directe |
| 2 | les deux soldes mis à jour en base | synchrone | assertion directe |
| 3 | un événement publié sur `virements-executes` | **asynchrone** | Awaitility |

Puis le cas du refus : **409**, aucun solde modifié, et **aucun événement
publié** — cette dernière assertion étant la plus délicate.

## Étapes

1. Ouvrir `exercice/VirementCompletExerciceIT.java`, retirer le `@Disabled`.
2. Dans le `@BeforeEach` : créer la souscription de contrôle sur le topic
   `virements-executes` si elle n'existe pas, puis la **vider** (pull + ack).
3. Écrire le test du parcours nominal (les trois effets).
4. Écrire le test du refus (409, soldes intacts, aucun événement).

```bash
./mvnw -pl 04-integration-pubsub -am verify
```

## ⚠️ Les trois pièges du chapitre

1. **Souscription créée trop tard.** Une souscription ne reçoit que les messages
   publiés **après** sa création. Créée dans le corps du test plutôt que dans le
   `@BeforeEach`, elle ne recevra jamais rien — et le test attendra jusqu'au
   timeout d'Awaitility.
2. **Consommateur concurrent.** L'application consomme déjà
   `virements-executes-journal`. Une souscription distribue chaque message à
   **un seul** consommateur : sans souscription de contrôle dédiée, le test et
   l'application se disputent le message.
3. **`Thread.sleep`.** Calibré sur votre poste, il sera trop court sur la machine
   d'intégration continue et trop long partout. `await().atMost(...).untilAsserted(...)`
   rend la main dès que la condition est vraie.

## Checklist des cas attendus

**Parcours nominal**

- [ ] la souscription de contrôle est créée dans le `@BeforeEach`, **avant** l'appel
- [ ] elle est vidée avant chaque test (isolation)
- [ ] statut 201 et `$.frais == 1.00`
- [ ] solde source = 3999.00 **en base**
- [ ] solde destination = 1000.00 **en base**
- [ ] l'événement est attendu avec Awaitility, pas avec `sleep`
- [ ] le **contenu** de l'événement est asséré (référence, IBAN, montant, frais),
      pas seulement sa présence

**Refus**

- [ ] statut 409 (et non 400 : la requête est valide, c'est l'état du compte qui refuse)
- [ ] aucun solde modifié en base
- [ ] aucun événement publié — vérifié avec `await().during(...)`, qui laisse au
      message une vraie chance d'arriver avant de conclure

## Vérification par sabotage

| # | Fichier | Modification | Ce qui doit devenir rouge |
|---|---|---|---|
| 1 | `ConfigurationPubSub.java` | commenter le bean `convertisseurJson` | la publication échoue (`PubSubMessageConversionException`) — donc l'effet 3 |
| 2 | **votre classe de test** | dans le `@BeforeEach`, porter le solde de `FR76-SOURCE` à `9999999.00` | le test de refus : le virement passe, et **un événement part pour un virement qui n'aurait pas dû avoir lieu** |
| 3 | `PublieurVirementPubSub.java` | commenter l'appel `pubSub.publish(...)` | l'effet 3 du parcours nominal |
| 4 | `EvenementVirement.java` | renommer le champ `reference` en `ref` | le contenu de l'événement — c'est une **rupture de contrat** inter-applications |

**Le n°1 est le cas d'école du chapitre** : relancez la classe unitaire
`PublieurVirementPubSubTest` avec ce sabotage en place — elle reste **verte**,
parce qu'un `PubSubTemplate` mocké accepte n'importe quel objet. Seul le test
d'intégration voit le bug.

Le n°2 est le seul qui prouve que votre test de refus vérifie réellement une
absence de publication, et non simplement le code 409.

Les n°3 et 4 cassent aussi des tests unitaires, qui s'exécutent **avant** les
tests d'intégration : Maven s'arrêtera sur eux. C'est normal — c'est la preuve
que le bug est détecté tôt. Pour aller quand même jusqu'aux `*IT` :

```bash
./mvnw -pl 04-integration-pubsub -am verify -Dmaven.test.failure.ignore=true
```

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
<summary>Indice 2 — attendre l'événement</summary>

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
<summary>Indice 3 — prouver une absence</summary>

`atMost` ne convient pas : il réussirait immédiatement, avant même que le message
ait eu le temps d'arriver. Il faut au contraire **laisser du temps s'écouler** et
vérifier que la condition reste vraie pendant toute la durée :

```java
await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
        .untilAsserted(() -> assertThat(
                pubSub.pull(SOUSCRIPTION_DE_CONTROLE, 10, true)).isEmpty());
```

C'est la différence entre « rien n'est encore arrivé » et « rien n'arrivera ».
</details>

## Questions de fin

1. Comparer la durée de cette classe à celle de `PublieurVirementPubSubTest`
   (unitaire, mock). Quel rapport ?
2. `PublieurVirementPubSubTest` aurait-il détecté l'absence du convertisseur JSON
   (`PubSubMessageConversionException`) ? Pourquoi ? (Refaites le sabotage n°3 et
   relancez la classe unitaire pour vérifier votre réponse.)
3. Combien de tests de ce niveau une équipe peut-elle raisonnablement maintenir
   dans une suite exécutée à chaque commit ?

## Corrigé

`04-integration-pubsub/src/test/java/fr/formation/banque/integrationpubsub/corrige/VirementCompletCorrigeIT.java`
