# 01 — Tests unitaires

Domaine métier pur : **aucune dépendance Spring**, aucune base, aucun réseau.
C'est cette absence de dépendances qui rend les tests instantanés.

> **Vous n'avez pas besoin de tout lire.** Les exercices 1 et 2 ne demandent que
> trois classes : `Montant`, `GrilleFrais` et `HorodatageVirement`. Le reste
> arrive plus tard, quand la démo 3 en a besoin.

## Le code, par vagues

| Vague | Classes | Pour quoi faire |
|---|---|---|
| 1 | `Montant` | value object (`BigDecimal`) : égalité par valeur, arrondi à 2 décimales — démo 1 |
| 2 | `Compte`, `SoldeInsuffisantException` | débit, crédit, cas limites de solde — démo 2 |
| 3 | `GrilleFrais`, `HorodatageVirement` | une fonction pure et une règle dépendant du temps — **exercices 1 et 2** |
| 4 | `ServiceVirement`, `Virement`, `CompteRepository`, `NotificateurVirement`, `GenerateurReference`, `CompteIntrouvableException` | l'orchestration et ses trois ports : quoi mocker, quoi ne pas mocker — démo 3 |

Les trois **ports** de la vague 4 (`CompteRepository`, `NotificateurVirement`,
`GenerateurReference`) sont les seules dépendances légitimes à mocker. Tout le
reste est du domaine pur : l'instancier coûte moins cher que le mocker.

## Tests

| Classe | Enseignement |
|---|---|
| `demo/MontantTest` | AAA, nommage, AssertJ, pièges `BigDecimal` |
| `demo/CompteTest` | `@Nested`, cas limites, `assertSoftly`, `assertThatThrownBy` |
| `demo/ServiceVirementTest` | mocks de ports, `ArgumentCaptor`, `never()` |
| `exercice/GrilleFraisExerciceTest` | **exercice 1** — [fiche](../docs/exercices/01-grille-frais.md) |
| `exercice/HorodatageVirementExerciceTest` | **exercice 2** — [fiche](../docs/exercices/02-horodatage.md) |
| `corrige/` | solutions commentées |

## Lancer

```bash
./mvnw -pl 01-tests-unitaires test
```

Durée attendue : moins d'une seconde d'exécution des tests.
