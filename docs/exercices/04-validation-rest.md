# Exercice 4 — Le même refus, à deux niveaux

> Module `03-integration-rest` · **50 min** · **Docker requis** (pour la partie 2)

## Objectif

Tester les refus d'une API REST **deux fois** : une fois en slice `@WebMvcTest`
(rapide, service mocké) et une fois bout en bout (lent, vraie base). Puis
comparer ce que chaque niveau prouve — et ce qu'il coûte.

**Compétence visée** : placer chaque test au bon niveau de la pyramide, et
justifier ce choix par ce qu'il prouve, pas par habitude.

## À lire d'abord

- [Fiche 1 — pyramide et vocabulaire](../01-pyramide-et-vocabulaire.md)
- `03-integration-rest/.../demo/VirementControllerTest.java` — la slice
- `03-integration-rest/.../demo/VirementBoutEnBoutIT.java` — le bout en bout
- `03-integration-rest/src/main/.../api/DemandeVirement.java` — les contraintes
- `03-integration-rest/src/main/.../api/GestionnaireErreursApi.java` — la
  traduction exception → code HTTP

## Énoncé

Deux familles d'erreurs produisent toutes deux un **400**, mais par des chemins
complètement différents :

| Cas | Origine du refus | Le service est-il appelé ? |
|---|---|---|
| montant à 3 décimales | `@Digits(fraction = 2)` | non |
| montant nul ou négatif | `@DecimalMin("0.01")` | non |
| montant absent | `@NotNull` | non |
| IBAN source == IBAN destination | règle du **domaine** | oui |

C'est cette distinction que l'exercice doit rendre visible.

### Partie 1 — slice (`ValidationVirementExerciceTest`)

Les quatre cas ci-dessus, avec le service mocké.

### Partie 2 — bout en bout (`ValidationVirementExerciceIT`)

Deux cas au plus — « deux IBAN identiques » et, si vous voulez, un montant
invalide — et dans les deux cas la vérification qu'**aucun solde n'a bougé en
base**. C'est la seule chose que ce niveau apporte. Ne pas rejouer ici les cinq
cas de validation de la partie 1 : ils seraient dix fois plus lents sans rien
prouver de plus.

## Étapes

1. Retirer les deux `@Disabled`.
2. Partie 1 : écrire les cas de validation. Vérifier le **400** et la présence
   du champ fautif dans `$.champs`.
3. Partie 1 : ajouter `verifyNoInteractions(virements)` sur les cas de
   validation. Réfléchir à ce que laisserait passer un test qui ne vérifierait
   que le code 400.
4. Partie 1 : pour « deux IBAN identiques », faire lever
   `IllegalArgumentException` par le service mocké, et vérifier que le
   `@RestControllerAdvice` renvoie bien un 400.
5. Partie 1 : regrouper les trois cas de montant invalide dans un
   `@ParameterizedTest`.
6. Partie 2 : le refus bout en bout + l'assertion sur le solde inchangé.

```bash
./mvnw -pl 03-integration-rest -am verify
```

## Checklist des cas attendus

**Slice**

- [ ] montant `0.001` → 400 + `$.champs.montant`
- [ ] montant `0` → 400
- [ ] montant négatif → 400
- [ ] montant absent → 400
- [ ] les trois cas de montant tiennent dans un `@ParameterizedTest`
- [ ] `verifyNoInteractions(virements)` sur **tous** les cas de validation
- [ ] IBAN identiques → le service mocké lève, la réponse est 400
- [ ] sur ce dernier cas, le service **est** appelé (contrairement aux autres)

**Bout en bout**

- [ ] IBAN identiques → 400
- [ ] `FR76-SOURCE` vaut toujours 5000.00 **en base** après l'appel
- [ ] au plus deux cas dans cette classe : elle ne rejoue pas la partie 1

## Vérification par sabotage

| # | Fichier | Modification | Ce qui doit devenir rouge |
|---|---|---|---|
| 1 | `DemandeVirement.java` | `@Digits(fraction = 2)` → `fraction = 3` | le cas à 3 décimales |
| 2 | `DemandeVirement.java` | `@DecimalMin("0.01")` → `"-9999"` | montant nul et négatif |
| 3 | `VirementController.java` | retirer `@Valid` du paramètre | tous les cas de validation |
| 4 | `GestionnaireErreursApi.java` | `requeteInvalide` → renvoyer `HttpStatus.INTERNAL_SERVER_ERROR` | le cas IBAN identiques |

Le sabotage n°3 est le plus parlant : un `@Valid` oublié est un bug réel et
fréquent, totalement invisible sur le parcours nominal.

Restaurer ensuite :

```bash
git checkout -- 03-integration-rest/src/main
```

## Indices

<details>
<summary>Indice 1 — comment lire le corps d'erreur ?</summary>

`GestionnaireErreursApi.handleMethodArgumentNotValid` enrichit la réponse d'une
propriété `champs` : une map `nom du champ → message`. Un 400 de validation porte
donc `$.champs.montant`, là où un 400 du domaine n'a pas de `champs` du tout.

C'est cette différence qui distingue les deux chemins dans vos assertions.
</details>

<details>
<summary>Indice 2 — la forme d'un test de slice</summary>

```java
assertThat(client.post().uri("/api/virements")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":0.001}
                """))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPathSatisfying("$.champs.montant", v -> v.assertThat().isNotNull());

verifyNoInteractions(virements);
```

`MockMvcTester` est l'API AssertJ de MockMvc : `assertThat(client.post()...)`
directement, sans `andExpect`.
</details>

<details>
<summary>Indice 3 — le cas du domaine, et la partie 2</summary>

Slice — c'est le **mock** qui produit le refus :

```java
when(virements.executer(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("Un virement doit relier deux comptes distincts"));
```

Bout en bout — il n'y a pas de mock, et l'assertion qui compte est la dernière :

```java
client.post().uri("/api/virements")
        .contentType(MediaType.APPLICATION_JSON)
        .body("""
                {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-SOURCE","montant":10.00}
                """)
        .exchange()
        .expectStatus().isBadRequest();

assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
        .isEqualTo(Montant.euros("5000.00"));
```
</details>

## Questions de fin

1. Comparer les durées affichées par Maven pour la classe `...Test` (slice) et la
   classe `...IT` (bout en bout). Quel rapport ?
2. Si l'équipe n'avait le budget que pour une seule des deux classes, laquelle
   garder — et qu'accepterait-elle de perdre ?

## Corrigés

- `03-integration-rest/src/test/java/fr/formation/banque/integrationrest/corrige/ValidationVirementCorrigeTest.java`
- `03-integration-rest/src/test/java/fr/formation/banque/integrationrest/corrige/ValidationVirementCorrigeIT.java`
