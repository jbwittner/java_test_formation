# 9. Aide-mémoire

## ⚠️ Spring Boot 4 : ce qui a changé

Spring Boot 4 a éclaté les autoconfigurations et les annotations de test en
modules. **Les imports trouvés sur le web (Boot 3) ne compilent pas.**

### Annotations de test

| Annotation | Package Boot 3 | Package Boot 4 | Artifact Maven |
|---|---|---|---|
| `@WebMvcTest` | `...test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` | `spring-boot-webmvc-test` |
| `@AutoConfigureMockMvc` | `...test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` | `spring-boot-webmvc-test` |
| `@DataJpaTest` | `...test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` | `spring-boot-data-jpa-test` |
| `TestEntityManager` | `...test.autoconfigure.orm.jpa` | `org.springframework.boot.jpa.test.autoconfigure` | (transitif) |
| `@AutoConfigureTestDatabase` | `...test.autoconfigure.jdbc` | `org.springframework.boot.jdbc.test.autoconfigure` | `spring-boot-jdbc-test` |
| `@JsonTest` | `...test.autoconfigure.json` | inchangé | `spring-boot-starter-test` |
| `@ServiceConnection` | `...testcontainers.service.connection` | inchangé | `spring-boot-testcontainers` |
| `@AutoConfigureRestTestClient` | n'existait pas | `org.springframework.boot.resttestclient.autoconfigure` | `spring-boot-resttestclient` |

### Remplacements

| Boot 3 | Boot 4 |
|---|---|
| `@MockBean` | **`@MockitoBean`** (`org.springframework.test.context.bean.override.mockito`) |
| `@SpyBean` | **`@MockitoSpyBean`** |
| `TestRestTemplate` | **`RestTestClient`** (+ `@AutoConfigureRestTestClient`, obligatoire) |
| `MockMvc` + `andExpect(...)` | **`MockMvcTester`** + `assertThat(...)` |
| `com.fasterxml.jackson.databind.ObjectMapper` | **`tools.jackson.databind.ObjectMapper`** (Jackson 3) |

### Autoconfigurations en modules séparés

| Fonction | Bibliothèque seule ne suffit pas | Artifact d'autoconfiguration |
|---|---|---|
| Flyway | `flyway-core` | **`spring-boot-flyway`** |

Symptôme si l'artifact manque : `Schema validation: missing table [...]` — le
message ne mentionne jamais Flyway.

### Testcontainers 2.x

Les `artifactId` sont préfixés :

| 1.x | 2.x |
|---|---|
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| `org.testcontainers:gcloud` | `org.testcontainers:testcontainers-gcloud` |
| `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |

## AssertJ — les assertions les plus utiles

```java
assertThat(valeur).isEqualTo(attendu);
assertThat(montant.valeur()).isEqualByComparingTo("10.00");  // BigDecimal : échelle ignorée
assertThat(liste).containsExactly("a", "b");                 // ordre garanti par le code
assertThat(liste).containsExactlyInAnyOrder("a", "b");       // ordre non garanti
assertThat(optional).isPresent().get().satisfies(v -> ...);
assertThat(objet).returns("VIR-1", Virement::reference);

assertThatThrownBy(() -> ...).isInstanceOf(X.class).hasMessageContaining("...");
assertThatExceptionOfType(X.class).isThrownBy(() -> ...).withMessageContaining("...");
assertThatIllegalArgumentException().isThrownBy(() -> ...);

assertSoftly(verif -> {            // évalue TOUT, rapporte tous les écarts
    verif.assertThat(a).isEqualTo(1);
    verif.assertThat(b).isEqualTo(2);
});
```

**Piège `BigDecimal`** : `isEqualTo` compare l'échelle, `10.0` ≠ `10.00`.
Utiliser `isEqualByComparingTo`.

## Tests paramétrés — quelle source choisir

```java
@ParameterizedTest(name = "{0} EUR -> {1} EUR")   // sans `name`, le rapport dit « [1] », « [2] »
@CsvSource({"1000.00, 1.00", "1000.01, 1.00"})    // cas réductibles à des chaînes
void devrait_...(String montant, String attendu) { }

@ValueSource(strings = {"0.00", "-10.00"})        // un seul paramètre
@EnumSource(MonEnum.class)                        // toutes les valeurs d'un enum,
                                                  // y compris celles ajoutées plus tard
@MethodSource("mesCas")                           // objets réels, cas nommés
static Stream<Arguments> mesCas() {
    return Stream.of(Arguments.of("libellé du cas", Montant.euros("2000.00"), Montant.euros("2.00")));
}
```

Règle pratique : `@CsvSource` par défaut, `@MethodSource` dès qu'un paramètre
n'est pas une chaîne ou un nombre, `@EnumSource` pour couvrir mécaniquement un
enum. Les cas d'erreur restent des `@Test` : leur forme est différente.

## Mockito

```java
@ExtendWith(MockitoExtension.class)     // active les stubs stricts
@Mock CompteRepository comptes;
@Captor ArgumentCaptor<Virement> capteur;

when(comptes.parIban("X")).thenReturn(Optional.of(compte));
when(service.executer(any(), any(), any())).thenThrow(new X());

verify(notificateur).virementExecute(capteur.capture());
verify(comptes, never()).enregistrer(any());
verifyNoInteractions(service);
```

`MockitoExtension` échoue sur un stub inutilisé : c'est une aide, elle signale
les tests qui préparent plus que nécessaire.

## Awaitility

```java
await().atMost(Duration.ofSeconds(15))
       .pollInterval(Duration.ofMillis(100))
       .untilAsserted(() -> assertThat(journal.count()).isEqualTo(1));

// Prouver une absence : la condition doit RESTER vraie
await().during(Duration.ofSeconds(2))
       .atMost(Duration.ofSeconds(6))
       .untilAsserted(() -> assertThat(pubSub.pull(...)).isEmpty());
```

Fourni par `spring-boot-starter-test`, aucune dépendance à ajouter.

## Commandes

```bash
./mvnw test                               # TOUT : *Test et *IT (~23 s, Docker requis)
./mvnw test -Prapide                      # *Test seuls (~7 s, sans Docker)
./mvnw -pl 02-integration-bdd -am test    # un chapitre (-am : construit ses dépendances)
./mvnw -Dtest=MontantTest test            # une classe
./mvnw -Dtest='*Compte*' test             # un motif
```

`-am` (*also make*) est **indispensable** : les modules dépendent les uns des
autres et ne sont pas installés dans le dépôt local.

## Convention de nommage du projet

| Suffixe | Nature | Coût | Docker |
|---|---|---|---|
| `*Test` | unitaire ou slice | millisecondes | non |
| `*IT` | intégration | secondes | oui |

Les deux tournent dans `mvn test`. Le suffixe dit le **coût**, pas la commande.
