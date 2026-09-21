# 02 — Intégration base de données

PostgreSQL réelle via Testcontainers. Adaptateur JPA du port `CompteRepository`
défini par le domaine au chapitre 01.

## Code de production

| Élément | Rôle |
|---|---|
| `CompteEntity` | entité JPA, `@Version` (verrouillage optimiste), séquence |
| `CompteJpaRepository` | dépôt Spring Data, tri **explicite** |
| `DepotComptesJpa` | adaptateur entité ↔ domaine |
| `V1__creation_table_compte.sql` | migration Flyway : unicité de l'IBAN, `CHECK (solde >= 0)` |

## Tests

| Classe | Enseignement |
|---|---|
| `demo/DepotComptesJpaIT` | `@SpringBootTest` non transactionnel, précision `NUMERIC(19,2)` |
| `demo/CompteJpaRepositoryIT` | slice `@DataJpaTest`, `@AutoConfigureTestDatabase(NONE)` |
| `demo/RollbackTransactionnelIT` | **ce que le rollback automatique masque** |
| `demo/MigrationFlywayIT` | tester le schéma livré via `information_schema` |
| `exercice/VerrouillageOptimisteExerciceIT` | **exercice 3** — [fiche](../docs/exercices/03-verrouillage-optimiste.md) |
| `corrige/VerrouillageOptimisteCorrigeIT` | mise à jour perdue, unicité, `CHECK (solde >= 0)` |

## Points clés

- `@ServiceConnection` configure la `DataSource` à partir du conteneur : aucune URL à écrire.
- `@DataJpaTest` remplace la `DataSource` par défaut → `replace = NONE` est **obligatoire**.
- Un test de concurrence ne peut pas être transactionnel → `TransactionTemplate`.
- `spring-boot-flyway` (l'autoconfiguration) est requis **en plus** de `flyway-core`.

## Lancer

```bash
./mvnw -pl 02-integration-bdd -am test
```
