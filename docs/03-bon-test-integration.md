# 3. Qu'est-ce qu'un bon test d'intégration ?

Support : modules `02-integration-bdd`, `03-integration-rest`,
`04-integration-pubsub`.

## Le principe directeur

> Un test d'intégration ne doit vérifier **que ce qui ne peut pas l'être
> autrement**.

Il coûte 10 à 100 fois plus cher qu'un test unitaire. Tout ce qu'il vérifie et
qui aurait pu l'être en mémoire est du temps perdu à chaque exécution, par chaque
développeur, à chaque commit.

Exemple concret dans ce dépôt : le corrigé de l'exercice 4 contient **cinq**
tests de validation au niveau slice, et **deux** seulement sur serveur HTTP réel
— un par chemin de refus, pour prouver que le rejet survit à une vraie traversée
HTTP. Corollaire du même principe : ces deux tests mockent le service, car ce que
fait le domaine ensuite est déjà prouvé ailleurs.

## Les quatre règles

### 1. Périmètre minimal

| Ce que je veux prouver | Outil |
|---|---|
| Une requête SQL, une cartographie JPA | `@DataJpaTest` (~1,3 s pour 5 tests) |
| Un contrat HTTP, un code d'erreur | `@WebMvcTest` (~0,6 s pour 7 tests) |
| Un format JSON | `@JsonTest` (~0,9 s pour 3 tests) |
| Un serveur HTTP réel, un broker | `@SpringBootTest` (~1,8 s pour 5 tests) |

Ces durées sont mesurées sur ce dépôt. L'écart entre la slice web et le test sur
serveur réel est d'un facteur 3 par classe — et il grimpe à un facteur 10 dès
qu'une base de données entre dans le contexte. D'où la règle suivie ici : on ne
démarre que la technologie dont le test a réellement besoin, et on mocke le
reste.

### 2. Isolation explicite

Une base de données est un état partagé : l'anti-pattern 4 s'y applique en pire,
car la dépendance est invisible dans le code du test.

Trois stratégies, par ordre de préférence :

1. **Nettoyer au `@BeforeEach`** (`jpa.deleteAll()`) — explicite, fonctionne
   aussi hors transaction ;
2. **Rollback automatique** (`@DataJpaTest`) — gratuit, mais masque les
   contraintes de base (voir plus bas) ;
3. **Données uniques par test** (IBAN dérivé du nom du test) — quand le nettoyage
   coûte trop cher.

Même problème sur un broker : les messages non consommés d'un test polluent le
suivant. D'où le `pull` + `ack` de purge dans `@BeforeEach` au chapitre 04.

### 3. Attendre une condition, jamais une durée

```java
// MAUVAIS : pari sur la vitesse de la machine
Thread.sleep(500);
assertThat(journal.count()).isEqualTo(1);

// BON : se termine dès que c'est vrai, échoue avec le détail sinon
await().atMost(Duration.ofSeconds(15))
       .untilAsserted(() -> assertThat(journal.findById("VIR-1")).isPresent());
```

Cas particulier — **prouver une absence**. Il faut laisser une vraie chance à
l'événement d'arriver avant de conclure :

```java
await().during(Duration.ofSeconds(2))   // la condition doit rester vraie 2 s
       .atMost(Duration.ofSeconds(6))
       .untilAsserted(() -> assertThat(pubSub.pull(...)).isEmpty());
```

### 4. Versions d'images figées

```java
DockerImageName.parse("postgres:17-alpine")
DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators")
```

`latest` rend le build non reproductible : la suite passe aujourd'hui et échoue
demain sans qu'une ligne de code ait changé.

## Le piège du rollback automatique

`@DataJpaTest` ouvre une transaction et l'annule après chaque test. Pratique —
mais Hibernate retarde ses écritures jusqu'au `flush`, et le `flush` n'arrive
qu'au commit… qui n'a jamais lieu.

Conséquence : **les contraintes de base ne se déclenchent pas**.
`RollbackTransactionnelIT` le démontre : deux insertions du même IBAN ne lèvent
rien tant qu'on ne force pas le `flush`.

Les parades :

| Parade | Quand |
|---|---|
| `depot.flush()` explicite | prouver qu'une contrainte se déclenche |
| `@Commit` | reproduire le comportement réel (nettoyage manuel requis) |
| `@SpringBootTest` sans `@Transactional` | chaque appel valide sa transaction, comme en production |

Corollaire : un test de **concurrence** ne peut pas être transactionnel. Deux
transactions simulées dans une seule transaction n'en font qu'une, et le conflit
ne se produit jamais. D'où `TransactionTemplate` dans
`VerrouillageOptimisteCorrigeIT`.

## Pourquoi pas H2 ?

H2 en mémoire démarre plus vite. Il ne reproduit pas :

- le comportement exact du verrouillage optimiste ;
- les types (`NUMERIC(19,2)` contre un flottant, précision comptable) ;
- la syntaxe et les contraintes PostgreSQL (`CHECK`, séquences, `ON CONFLICT`) ;
- les niveaux d'isolation et les verrous réels.

Tester sur H2 pour livrer sur PostgreSQL, c'est tester un autre logiciel. Les
quatre tests de `MigrationFlywayIT` seraient tout simplement impossibles.

## Ce qu'un mock ne prouvera jamais

Liste, tirée de ce dépôt, de bugs qu'aucun test unitaire n'aurait pu voir :

- une migration Flyway non appliquée (`missing table [compte]`) ;
- une `@Version` absente → mise à jour perdue en concurrence ;
- un objet non sérialisable par Pub/Sub → `PubSubMessageConversionException` ;
- un topic mal nommé dans la configuration ;
- une souscription créée après la publication → aucun message reçu ;
- un `@AutoConfigureTestDatabase` manquant → le conteneur démarre et n'est pas utilisé ;
- un « vérifier-puis-agir » (`existsById` puis `save`) qui laisse passer un
  doublon quand deux messages sont traités en parallèle. En mémoire, avec un
  seul thread, le code paraît correct ; c'est le test d'intégration Pub/Sub qui
  a révélé la course, et la **contrainte de clé primaire** qui la corrige
  réellement.
