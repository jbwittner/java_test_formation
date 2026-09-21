# Exercice 2 — Rendre le temps testable

> Module `01-tests-unitaires` · **40 min** · aucun Docker

## Objectif

Tester une règle qui dépend de l'heure et du jour, en figeant l'horloge — donc
sans jamais attendre, et avec le même résultat à 3 h du matin un samedi.

**Compétence visée** : reconnaître une dépendance cachée au temps et la neutraliser
par injection, au lieu de la contourner par des tests fragiles.

## À lire d'abord

- `01-tests-unitaires/src/main/.../domaine/HorodatageVirement.java` — noter que
  le `Clock` est un paramètre de constructeur, jamais un appel statique
- `00-antipatterns/.../AntiPattern05HorlogeNonInjecteeTest.java` — le même code,
  avec `LocalDateTime.now()` au milieu, et pourquoi il est intestable
- `demo/ServiceVirementTest.java`, méthode `preparerService()` — un `Clock.fixed`
  en situation

## Énoncé

`HorodatageVirement.dateDeValeur()` applique ces règles :

| Situation | Date de valeur |
|---|---|
| avant 16 h 00 | aujourd'hui |
| à partir de 16 h 00 (**borne incluse**) | jour suivant |
| samedi ou dimanche | reporté au lundi |

## Étapes

1. Ouvrir `exercice/HorodatageVirementExerciceTest.java` et retirer le `@Disabled`.
2. Remplacer `Clock.systemDefaultZone()` par un `Clock.fixed(...)` — c'est tout
   l'exercice en une ligne.
3. Écrire un test par cas limite (voir la checklist).
4. Regrouper ensuite ces cas dans un seul `@ParameterizedTest`.

```bash
./mvnw -pl 01-tests-unitaires test
```

## ⚠️ Le piège du fuseau

Un `Instant` est un point sur l'axe du temps, **sans fuseau**. C'est la `ZoneId`
qui décide s'il est 16 h ou 18 h. En juin, Paris est à UTC+2 :

```
Instant.parse("2025-06-03T14:00:00Z")  ==  mardi 3 juin, 16 h 00 à Paris
```

Ne **jamais** utiliser `ZoneId.systemDefault()` dans un test : cela réintroduit
la dépendance à l'environnement qu'on cherche justement à supprimer.

## Checklist des cas attendus

- [ ] mardi 08 h 00 → le jour même
- [ ] mardi 15 h 59 min 59 s → le jour même (dernière seconde avant la coupure)
- [ ] **mardi 16 h 00 pile → le lendemain** (la borne est inclusive)
- [ ] mardi 22 h 00 → le lendemain
- [ ] **vendredi 17 h 00 → le lundi** (J+1 tombe un samedi)
- [ ] samedi 10 h 00 → le lundi
- [ ] dimanche 10 h 00 → le lundi
- [ ] la `ZoneId` est explicite dans tous les tests
- [ ] tous les cas tiennent dans un `@ParameterizedTest`

## Vérification par sabotage

Dans `01-tests-unitaires/src/main/java/fr/formation/banque/domaine/HorodatageVirement.java` :

| # | Modification | Ce qui doit devenir rouge |
|---|---|---|
| 1 | `HEURE_DE_COUPURE = LocalTime.of(16, 0)` → `LocalTime.of(17, 0)` | le cas de 16 h 00 pile |
| 2 | `if (!maintenant.toLocalTime().isBefore(...))` → retirer le `!` | presque tous les cas |
| 3 | dans `prochainJourOuvre`, retirer le second test `resultat.getDayOfWeek() == DayOfWeek.SUNDAY` | vendredi 17 h, samedi et dimanche |

Si le sabotage n°1 laisse la suite verte, **vous avez testé 10 h et 22 h mais pas
la borne** — c'est exactement le bug que ce type de test doit attraper.

Restaurer ensuite :

```bash
git checkout -- 01-tests-unitaires/src/main/java/fr/formation/banque/domaine/HorodatageVirement.java
```

## Indices

<details>
<summary>Indice 1 — comment figer l'horloge ?</summary>

`Clock` est une classe abstraite de `java.time` avec des fabriques statiques.
Celle qui vous intéresse renvoie toujours le même instant, quoi qu'il arrive :
`Clock.fixed(Instant, ZoneId)`.

Le constructeur de `HorodatageVirement` prend un `Clock` : il accepte donc
indifféremment l'horloge système en production et une horloge figée en test.
C'est tout l'intérêt de l'injection.
</details>

<details>
<summary>Indice 2 — la structure d'un cas</summary>

```java
private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

private static LocalDate dateDeValeurPour(String instantUtc) {
    Clock horloge = Clock.fixed(Instant.parse(instantUtc), PARIS);
    return new HorodatageVirement(horloge).dateDeValeur();
}
```

Une méthode privée comme celle-ci réduit chaque cas à une ligne — et rend le
passage au `@ParameterizedTest` évident.
</details>

<details>
<summary>Indice 3 — les instants UTC à utiliser</summary>

En juin, Paris = UTC+2. Retranchez donc 2 heures à l'heure locale voulue :

| Heure locale voulue | Instant UTC |
|---|---|
| mardi 08 h 00 | `2025-06-03T06:00:00Z` |
| mardi 15 h 59 min 59 s | `2025-06-03T13:59:59Z` |
| mardi 16 h 00 pile | `2025-06-03T14:00:00Z` |
| vendredi 17 h 00 | `2025-06-06T15:00:00Z` |
| samedi 10 h 00 | `2025-06-07T08:00:00Z` |

Le `@CsvSource` peut porter une troisième colonne de description, réutilisée dans
`assertThat(...).as(description)` : le message d'échec devient auto-explicatif.
</details>

## Question de fin

Si `HorodatageVirement` appelait `LocalDateTime.now()` au lieu de recevoir un
`Clock`, comment testeriez-vous la règle du vendredi 17 h ? Combien de temps le
test prendrait-il, et serait-il fiable en intégration continue ?

## Corrigé

`01-tests-unitaires/src/test/java/fr/formation/banque/unitaire/corrige/HorodatageVirementCorrigeTest.java`
