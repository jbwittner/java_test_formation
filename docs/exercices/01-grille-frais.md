# Exercice 1 — Couvrir un barème avec un seul test paramétré

> Module `01-tests-unitaires` · **40 min** · aucun Docker

## Objectif

Couvrir l'intégralité d'un barème à paliers avec un `@ParameterizedTest`, en
testant les **bornes** plutôt que le milieu des intervalles.

**Compétence visée** : reconnaître les cas qui se réduisent à un tableau de
données, et écrire un rapport de test qui se lit comme une spécification.

## À lire d'abord

- [Fiche 2 — un bon test unitaire](../02-bon-test-unitaire.md), section
  *Tester les bornes, pas le milieu*
- `01-tests-unitaires/src/test/.../demo/MontantTest.java` — la structure AAA
- `01-tests-unitaires/src/main/.../domaine/GrilleFrais.java` — le code testé

## Énoncé

`GrilleFrais.calculer(Montant)` applique ce barème :

| Montant | Frais |
|---|---|
| jusqu'à 1 000 **inclus** | 1,00 forfaitaire |
| de 1 000 **exclu** à 10 000 **inclus** | 0,1 % du montant |
| au-delà de 10 000 | 15,00 forfaitaire |
| nul ou négatif | `IllegalArgumentException` |

Attention : les frais sont un `Montant`, donc arrondis à 2 décimales `HALF_UP`.

## Étapes

1. Ouvrir `exercice/GrilleFraisExerciceTest.java` et retirer le `@Disabled`.
2. Compléter le test d'amorçage : asserter que 500 EUR coûte 1,00 EUR.
3. Le transformer en `@ParameterizedTest` + `@CsvSource`, une ligne par cas.
4. Ajouter les quatre bornes : 1000 / 1000.01 et 10000 / 10000.01.
5. Ajouter les deux montants invalides (0 et négatif) — en `@Test` classiques,
   leur forme est différente.
6. Donner un `name = "..."` au test paramétré pour que le rapport nomme chaque cas.

```bash
./mvnw -pl 01-tests-unitaires test
```

## Checklist des cas attendus

- [ ] montant minimal (0.01) → 1,00
- [ ] milieu du palier bas (500) → 1,00
- [ ] **borne 1000 incluse** → 1,00
- [ ] **borne 1000.01** → premier montant proportionnel
- [ ] milieu du palier intermédiaire (5000) → 5,00
- [ ] **borne 10000 incluse** → 10,00
- [ ] **borne 10000.01** → bascule sur le forfait 15,00
- [ ] très gros montant → toujours 15,00
- [ ] un cas où l'arrondi joue (1234.56 → 1,23)
- [ ] montant nul → `IllegalArgumentException`
- [ ] montant négatif → `IllegalArgumentException`
- [ ] le rapport de test nomme chaque cas avec ses valeurs

## Vérification par sabotage

Dans `01-tests-unitaires/src/main/java/fr/formation/banque/domaine/GrilleFrais.java` :

| # | Modification | Ce qui doit devenir rouge |
|---|---|---|
| 1 | `valeur.compareTo(PALIER_HAUT) <= 0` → `< 0` | la borne 10000, qui bascule à tort sur le forfait haut |
| 2 | `FORFAIT_PALIER_BAS = new BigDecimal("1.00")` → `"2.00"` | tout le palier bas |
| 3 | `TAUX_PALIER_INTERMEDIAIRE = "0.001"` → `"0.002"` | tous les cas proportionnels |

Les trois doivent faire échouer au moins un test. Si le sabotage n°1 laisse la
suite verte, **vous n'avez pas testé la borne 10000**.

> **Curiosité à noter** : déplacer de la même façon la borne 1000
> (`compareTo(PALIER_BAS) <= 0` → `< 0`) ne casse **aucun** test, et c'est
> normal : 1000 × 0,1 % = 1,00, exactement le forfait. Le barème est continu en
> ce point, donc la borne n'y est pas observable de l'extérieur. C'est une
> limite réelle du test par l'observation du résultat — et une bonne question à
> se poser avant de conclure qu'un test « couvre » une branche.

Restaurer ensuite :

```bash
git checkout -- 01-tests-unitaires/src/main/java/fr/formation/banque/domaine/GrilleFrais.java
```

## Indices

<details>
<summary>Indice 1 — je ne vois pas comment éviter huit méthodes presque identiques</summary>

Les huit cas ne diffèrent que par **deux valeurs** : le montant en entrée et les
frais attendus. Tout ce qui les entoure est identique. C'est exactement la
situation que `@ParameterizedTest` traite : une méthode, des paramètres, une
source de données.
</details>

<details>
<summary>Indice 2 — quelle source de données, et quelle forme ?</summary>

`@CsvSource` prend un tableau de chaînes, une par cas, colonnes séparées par des
virgules. La méthode de test reçoit une colonne par paramètre :

```java
@ParameterizedTest(name = "{0} EUR -> {1} EUR de frais")
@CsvSource({
        "500.00,   1.00",
        "1000.00,  1.00",
        // ...
})
void devrait_appliquer_le_bareme_selon_le_palier(String montant, String fraisAttendus) {
    // ...
}
```

Le `name` est ce qui rend le rapport lisible : sans lui, les cas s'appellent
`[1]`, `[2]`, `[3]`.
</details>

<details>
<summary>Indice 3 — le corps de la méthode</summary>

```java
Montant frais = GrilleFrais.calculer(Montant.euros(montant));

assertThat(frais).isEqualTo(Montant.euros(fraisAttendus));
```

Pour les cas d'erreur, un `@Test` classique :

```java
assertThatIllegalArgumentException()
        .isThrownBy(() -> GrilleFrais.calculer(Montant.euros("0.00")));
```

Et si vous voulez manipuler de vrais `Montant` plutôt que des chaînes, regardez
`@MethodSource` dans le corrigé.
</details>

## Question de fin

Combien de `@Test` auriez-vous écrits sans `@ParameterizedTest` ? Lequel des deux
rapports fait mieux comprendre le barème à quelqu'un qui ne connaît pas le code ?

## Corrigé

`01-tests-unitaires/src/test/java/fr/formation/banque/unitaire/corrige/GrilleFraisCorrigeTest.java`
