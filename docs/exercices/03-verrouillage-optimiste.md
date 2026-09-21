# Exercice 3 — Prouver ce qu'un mock ne peut pas prouver

> Module `02-integration-bdd` · **50 min** · **Docker requis**

## Objectif

Écrire trois tests d'intégration qui démontrent des comportements dont un test
unitaire est structurellement incapable : verrouillage optimiste, unicité et
contrainte `CHECK`, toutes garanties par PostgreSQL et non par Java.

**Compétence visée** : savoir quand un vrai moteur de base est indispensable, et
maîtriser le piège du rollback automatique.

## À lire d'abord

- [Fiche 3 — un bon test d'intégration](../03-bon-test-integration.md), sections
  *Le piège du rollback automatique* et *Pourquoi pas H2 ?*
- `02-integration-bdd/.../demo/RollbackTransactionnelIT.java` — **la démo clé** :
  ce que `@Transactional` masque
- `02-integration-bdd/src/main/.../persistance/CompteEntity.java` — le champ `@Version`
- `02-integration-bdd/src/main/resources/db/migration/V1__creation_table_compte.sql`

## Énoncé

**Test 1 — mise à jour perdue.** Deux instances de l'application modifient le
même compte en même temps. Sans protection, la seconde écriture écrase la
première et le débit disparaît. `CompteEntity` porte un champ `@Version` :
Hibernate ajoute `WHERE version = ?` à chaque `UPDATE`, et lève
`ObjectOptimisticLockingFailureException` si zéro ligne est affectée.

**Test 2 — unicité.** Le schéma déclare `CONSTRAINT compte_iban_unique`. Deux
comptes de même IBAN, dans deux transactions distinctes, doivent produire une
`DataIntegrityViolationException`.

**Test 3 — contrainte CHECK.** Le schéma déclare `CHECK (solde >= 0)`. La règle
existe **deux fois** : dans le domaine (chapitre 01) et dans la base. Ce test
prouve que le garde-fou de dernier recours est en place — utile le jour où une
insertion arrive par un script.

## Étapes

1. Ouvrir `exercice/VerrouillageOptimisteExerciceIT.java`, retirer le `@Disabled`.
2. Test 1 : charger deux fois l'entité dans **deux transactions distinctes**,
   enregistrer la première (succès), puis la seconde (exception attendue), et
   vérifier que le solde final est celui de la **première** écriture.
3. Test 2 : deux comptes de même IBAN, deux `transaction.execute(...)`.
4. Test 3 : un `CompteEntity` de solde négatif.

```bash
./mvnw -pl 02-integration-bdd -am verify
```

## ⚠️ Le piège principal

**Ne pas annoter la classe `@Transactional`**, et ne pas se contenter d'appeler
`depot.save(...)` directement.

Deux raisons, et ce sont les deux enseignements du chapitre :

1. Dans une seule transaction, Hibernate renvoie la **même instance** depuis son
   cache de premier niveau : le conflit ne se produit jamais et le test est vert
   sans rien prouver.
2. Avec le rollback automatique, rien n'est jamais réellement envoyé à
   PostgreSQL au `COMMIT` : les contraintes `UNIQUE` et `CHECK` ne sont pas
   évaluées. Le test 2 et le test 3 passeraient au vert sur un schéma sans
   aucune contrainte.

`TransactionTemplate` sert exactement à ouvrir de vraies transactions séparées.

## Checklist des cas attendus

- [ ] la classe n'est **pas** `@Transactional`
- [ ] test 1 : deux chargements dans deux `transaction.execute(...)` distincts
- [ ] test 1 : la première écriture réussit
- [ ] test 1 : la seconde lève `ObjectOptimisticLockingFailureException`
- [ ] test 1 : **le solde final est celui de la première écriture** (c'est la
      preuve qu'aucune mise à jour n'est perdue)
- [ ] test 2 : second IBAN identique → `DataIntegrityViolationException`
- [ ] test 2 : la base ne contient qu'**une seule** ligne à la fin
- [ ] test 3 : solde négatif → `DataIntegrityViolationException`
- [ ] `@BeforeEach` vide la base (isolation explicite)

## Vérification par sabotage

| # | Fichier | Modification | Ce qui doit devenir rouge |
|---|---|---|---|
| 1 | `CompteEntity.java` | commenter `@Version` **et** initialiser le champ : `private Long version = 0L;` | le test 1 (et lui seul) |
| 2 | `V1__creation_table_compte.sql` | retirer `CONSTRAINT compte_iban_unique UNIQUE (iban)` | le test 2 — et aussi `MigrationFlywayIT` |
| 3 | `V1__creation_table_compte.sql` | retirer `CONSTRAINT compte_solde_positif CHECK (solde >= 0)` | le test 3 (et lui seul) |
| 4 | votre classe de test | ajouter `@Transactional` sur la classe | les tests 2 et 3 — **et ils repassent au vert, ce qui est précisément le piège** |

Sur le n°1, l'initialisation à `0L` est indispensable : sans elle, la colonne
`version NOT NULL` reçoit `null` et **tout** le module tombe, ce qui ne prouve
plus rien sur le verrouillage.

Le sabotage n°4 est le plus instructif du dépôt : il montre un test qui devient
vert alors que le code testé est moins sûr.

Restaurer ensuite :

```bash
git checkout -- 02-integration-bdd/src/main
```

## Indices

<details>
<summary>Indice 1 — comment obtenir deux transactions distinctes ?</summary>

`TransactionTemplate` est déjà injecté dans la classe. Chaque appel à
`transaction.execute(statut -> ...)` ouvre une transaction, exécute le corps, et
**valide** en sortie. Deux appels = deux transactions = deux instances Hibernate
différentes de la même ligne.
</details>

<details>
<summary>Indice 2 — la structure du test 1</summary>

```java
Long id = transaction.execute(s -> depot.save(compte("FR76-CONCURRENT", "1000.00")).id());

CompteEntity premiere = transaction.execute(s -> depot.findById(id).orElseThrow());
CompteEntity seconde  = transaction.execute(s -> depot.findById(id).orElseThrow());

premiere.changerSolde(new BigDecimal("900.00"));
transaction.execute(s -> depot.save(premiere));

seconde.changerSolde(new BigDecimal("800.00"));
// TODO : cet appel doit lever
```

Les deux objets `premiere` et `seconde` portent la **même** version. La première
écriture l'incrémente en base ; la seconde arrive avec une version périmée.
</details>

<details>
<summary>Indice 3 — les assertions</summary>

```java
assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
        .isThrownBy(() -> transaction.execute(s -> depot.save(seconde)));

// La preuve que rien n'a été perdu :
assertThat(transaction.execute(s -> depot.findById(id).orElseThrow().solde()))
        .isEqualByComparingTo("900.00");
```

Pour les tests 2 et 3, même forme avec `DataIntegrityViolationException`. Le
`transaction.execute(...)` est indispensable : c'est le `COMMIT` qui déclenche
la contrainte, pas le `save()`.
</details>

## Questions de fin

1. Ces trois tests sont-ils écrivables avec un mock de `CompteRepository` ? Avec
   H2 en mémoire ?
2. Que se passerait-il si on retirait `@Version` de `CompteEntity` ? Quel test
   échouerait, et quel bug cela laisserait-il passer en production ?

## Corrigé

`02-integration-bdd/src/test/java/fr/formation/banque/integrationbdd/corrige/VerrouillageOptimisteCorrigeIT.java`
