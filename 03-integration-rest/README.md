# 03 — Intégration REST

Deux niveaux de test pour le même contrôleur, et la comparaison de ce que chacun
prouve. **Les deux s'arrêtent à la couche REST** : le service est mocké, il n'y
a ni règle métier ni base de données dans ce chapitre. Le domaine est testé au
chapitre 01, la persistance au chapitre 02, la messagerie au chapitre 04.

## Code de production

| Élément | Rôle |
|---|---|
| `VirementController` | `POST /api/virements`, `GET /api/comptes/{iban}` — **un seul collaborateur** : `ServiceVirement` |
| `DemandeVirement` / `ReponseVirement` / `ReponseCompte` | DTO du contrat HTTP |
| `GestionnaireErreursApi` | exceptions du domaine → `ProblemDetail` (404 / 409 / 400) |
| `ConfigurationDomaine` | câblage Spring du domaine |

## Tests

| Classe | Niveau | Durée mesurée |
|---|---|---|
| `demo/VirementControllerTest` | slice `@WebMvcTest`, service mocké | ~0,6 s / 7 tests |
| `demo/ReponseVirementJsonTest` | slice `@JsonTest` | ~0,9 s / 3 tests |
| `demo/VirementHttpIT` | `@SpringBootTest` + `RestTestClient`, serveur réel, service mocké | ~1,8 s / 5 tests |
| `exercice/…ExerciceTest` / `…ExerciceIT` | **exercice 4**, aux deux niveaux — [fiche](../docs/exercices/04-validation-rest.md) | |
| `corrige/` | solutions | |
| `support/SocleCoucheRest` | socle des `*IT` : serveur réel, sans JDBC/JPA/Flyway | |

## Points clés

- `@MockBean` n'existe plus → **`@MockitoBean`**.
- `MockMvcTester` : l'API AssertJ de MockMvc, `assertThat(...).hasStatus(...)`.
- `RestTestClient` remplace `TestRestTemplate` et exige
  **`@AutoConfigureRestTestClient`** (sinon : *No qualifying bean*).
- 409 et non 400 pour un solde insuffisant : la requête est valide, c'est l'état
  du compte qui refuse.
- **Frontière du test** : `@MockitoBean ServiceVirement`, et lui seul, aux deux
  niveaux. Ce qui est prouvé ici, c'est la traduction HTTP ↔ domaine — pas ce que
  fait le domaine ensuite.
- Le contrôleur passe par le service même pour consulter un compte : une porte
  d'entrée unique dans le domaine, donc **un seul mock** par test.
- Ce que le serveur réel ajoute à la slice : vraie socket, vrais en-têtes,
  serveur embarqué démarré, câblage Spring de l'application complet. Ce qu'il
  n'ajoute pas : ni métier, ni persistance.
- `spring.autoconfigure.exclude` pour couper JDBC/JPA/Flyway (hérités du module
  02) : pas de `DataSource` réclamée au démarrage, donc **pas de Docker**.
- Répartition : beaucoup de tests de slice, peu de tests sur serveur réel.

## Lancer

```bash
./mvnw -pl 03-integration-rest -am verify
```
