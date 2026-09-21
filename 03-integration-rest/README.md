# 03 — Intégration REST

Deux niveaux de test pour le même contrôleur, et la comparaison de ce que chacun
prouve.

## Code de production

| Élément | Rôle |
|---|---|
| `VirementController` | `POST /api/virements`, `GET /api/comptes/{iban}` |
| `DemandeVirement` / `ReponseVirement` / `ReponseCompte` | DTO du contrat HTTP |
| `GestionnaireErreursApi` | exceptions du domaine → `ProblemDetail` (404 / 409 / 400) |
| `ConfigurationDomaine` | câblage Spring du domaine |

## Tests

| Classe | Niveau | Durée mesurée |
|---|---|---|
| `demo/VirementControllerTest` | slice `@WebMvcTest`, service mocké | ~0,6 s / 7 tests |
| `demo/ReponseVirementJsonTest` | slice `@JsonTest` | ~1 s / 3 tests |
| `demo/VirementBoutEnBoutIT` | `@SpringBootTest` + `RestTestClient` + PostgreSQL | ~4,8 s / 4 tests |
| `exercice/…ExerciceTest` / `…ExerciceIT` | **exercice 4**, aux deux niveaux — [fiche](../docs/exercices/04-validation-rest.md) | |
| `corrige/` | solutions | |

## Points clés

- `@MockBean` n'existe plus → **`@MockitoBean`**.
- `MockMvcTester` : l'API AssertJ de MockMvc, `assertThat(...).hasStatus(...)`.
- `RestTestClient` remplace `TestRestTemplate` et exige
  **`@AutoConfigureRestTestClient`** (sinon : *No qualifying bean*).
- 409 et non 400 pour un solde insuffisant : la requête est valide, c'est l'état
  du compte qui refuse.
- Répartition : beaucoup de tests de slice, peu de tests bout en bout.

## Lancer

```bash
./mvnw -pl 03-integration-rest -am verify
```
