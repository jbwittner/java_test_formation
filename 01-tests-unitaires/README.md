# 01 — Tests unitaires

Domaine métier pur : **aucune dépendance Spring**, aucune base, aucun réseau.
C'est cette absence de dépendances qui rend les tests instantanés.

## Code de production

| Classe | Rôle | Intérêt pour les tests |
|---|---|---|
| `Montant` | value object (`BigDecimal` + devise) | égalité par valeur, arrondi, refus de mélanger les devises |
| `Compte` | entité : débit, crédit, découvert | cas limites de solde |
| `GrilleFrais` | barème par paliers, fonction pure | idéal pour `@ParameterizedTest` |
| `HorodatageVirement` | date de valeur, coupure 16 h, week-ends | `Clock` **injecté** |
| `ServiceVirement` | orchestration | quoi mocker, quoi ne pas mocker |
| `CompteRepository`, `NotificateurVirement`, `GenerateurReference` | ports | les seules dépendances légitimes à mocker |

## Tests

| Classe | Enseignement |
|---|---|
| `demo/MontantTest` | AAA, nommage, AssertJ, pièges `BigDecimal` |
| `demo/CompteTest` | `@Nested`, cas limites, `assertSoftly` |
| `demo/ServiceVirementTest` | mocks de ports, `ArgumentCaptor`, `never()` |
| `exercice/GrilleFraisExerciceTest` | **exercice 1** — tests paramétrés |
| `exercice/HorodatageVirementExerciceTest` | **exercice 2** — `Clock.fixed` |
| `corrige/` | solutions commentées |

## Lancer

```bash
./mvnw -pl 01-tests-unitaires test
```

Durée attendue : moins d'une seconde d'exécution des tests.
