# 2. Qu'est-ce qu'un bon test unitaire ?

Support : module `01-tests-unitaires`. Contre-exemples : module `00-antipatterns`.

## Les cinq propriétés

Un bon test unitaire est :

1. **Rapide** — quelques millisecondes. Les 106 tests unitaires de ce dépôt
   s'exécutent en 7 secondes, compilation comprise.
2. **Isolé** — aucun état partagé, aucun ordre d'exécution présumé. On doit
   pouvoir lancer n'importe quel test seul, ou tous en parallèle.
3. **Déterministe** — même résultat à 3 h du matin, un 29 février, sur la machine
   d'intégration continue. Pas d'horloge système, pas d'aléa, pas de réseau.
4. **Lisible** — le nom et le corps disent la règle métier. Un test est d'abord
   une **spécification exécutable**.
5. **Utile** — il échoue quand le comportement change. Un test qui ne peut pas
   échouer est un coût sans contrepartie (anti-patterns 2 et 6).

## La structure AAA

```java
@Test
@DisplayName("refuse un débit d'un centime de plus que le solde")
void devrait_lever_SoldeInsuffisantException_quand_le_debit_depasse_le_solde() {
    // Arrange — préparer le contexte
    Compte compte = new Compte("FR76-SOURCE", Montant.euros("100.00"));

    // Act — une seule action, celle qu'on teste
    // Assert — vérifier le résultat ET l'état
    assertThatExceptionOfType(SoldeInsuffisantException.class)
            .isThrownBy(() -> compte.debiter(Montant.euros("100.01")));
}
```

Trois blocs, dans cet ordre, séparés par une ligne vide. Si le bloc **Act**
contient deux actions, c'est qu'il y a deux tests (anti-pattern 7).

## Nommer

Convention retenue : `devrait_<comportement>_quand_<condition>`, doublée d'un
`@DisplayName` en français.

```java
@DisplayName("refuse un débit d'un centime de plus que le solde")
void devrait_lever_SoldeInsuffisantException_quand_le_debit_depasse_le_solde()
```

Pourquoi les deux ? Le `@DisplayName` rend le rapport lisible ; le nom de méthode
apparaît dans les traces, les logs de build et les rapports de couverture, où le
`@DisplayName` n'arrive pas toujours.

Test décisif : **lire les noms de tests d'une classe doit suffire à comprendre
la règle métier**, sans ouvrir le code de production.

## Tester les bornes, pas le milieu

C'est le conseil qui change le plus la valeur d'une suite de tests.

```java
"1000.00,  1.00",    // borne haute INCLUSE du palier bas
"1000.01,  1.00",    // premier montant du palier suivant
"10000.00, 10.00",   // borne haute INCLUSE du palier intermédiaire
"10000.01, 15.00",   // bascule sur le forfait
```

Tester `500` et `50 000` ne prouve presque rien : les bugs de barème vivent aux
bornes. Même principe pour l'heure de coupure (15 h 59 min 59 s / 16 h 00 pile)
et pour le solde (débit exactement égal au solde / un centime de plus).

## Les outils, et quand s'en servir

| Besoin | Outil | Exemple |
|---|---|---|
| Plusieurs jeux de données, même règle | `@ParameterizedTest` + `@CsvSource` | `GrilleFraisCorrigeTest` |
| Cas non réductibles à des chaînes | `@MethodSource` | `casDeProportionnalite` |
| Couvrir toutes les valeurs d'un enum | `@EnumSource` | devises |
| Grouper par comportement | `@Nested` | `CompteTest` |
| Plusieurs facettes d'un même état | `assertSoftly` | solde + disponible + signe |
| Vérifier une exception | `assertThatExceptionOfType` | type **et** message |
| Neutraliser le temps | `Clock.fixed` | `HorodatageVirementCorrigeTest` |

## Écrire la valeur attendue en dur

```java
// BON  — valeur calculée à la main, indépendante du code
assertThat(virement.frais()).isEqualTo(Montant.euros("1.00"));

// MAUVAIS — le test rejoue la formule de production
assertThat(virement.frais())
        .isEqualTo(GrilleFrais.calculer(montant));
```

La seconde version passe même si la formule est fausse : elle ne compare que le
code à lui-même. C'est le même piège que l'anti-pattern 5, sous une autre forme.

## Couverture : ce qu'elle dit et ne dit pas

JaCoCo est actif sur ce projet, **sans seuil bloquant**, volontairement.

- La couverture dit ce qui n'est **pas** testé. C'est une information utile.
- Elle ne dit **rien** de la qualité de ce qui est testé : les anti-patterns 2
  et 6 produisent une excellente couverture et zéro détection.
- Un seuil imposé pousse à écrire les tests qui montent le chiffre — c'est-à-dire
  exactement ceux qui ne servent à rien.

Question à poser devant un rapport de couverture : *« quelle ligne de ce code
pourrais-je casser sans faire échouer un seul test ? »*
