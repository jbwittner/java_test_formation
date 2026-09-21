# Formation — Tests unitaires et tests d'intégration avec Spring Boot

Support de formation pratique : un projet Maven multi-modules, un module par
chapitre, entièrement exécutable.

Domaine métier fil rouge : une **banque**, volontairement réduite — comptes,
virements, frais, date de valeur, événements. Quatre classes métier suffisent à
porter tout le contenu pédagogique.

---

## Prérequis

| Outil | Version | Vérification |
|---|---|---|
| JDK | 21 ou plus | `java -version` |
| Docker | démon démarré | `docker info` |
| Maven | **inutile** — le wrapper est fourni | `./mvnw -v` |

**Avant la session**, télécharger les images (plusieurs centaines de Mo) :

```bash
docker pull postgres:17-alpine
docker pull gcr.io/google.com/cloudsdktool/google-cloud-cli:579.0.0-emulators
```

Sans cela, le premier `./mvnw verify` inclut le téléchargement et peut durer
plusieurs minutes.

## Démarrage

```bash
bash outils/verifier-environnement.sh   # préflight : JDK, Docker, images, suite unitaire
```

Sous Windows (PowerShell), utiliser l'équivalent :

```powershell
.\outils\verifier-environnement.ps1
```

Si PowerShell refuse d'exécuter le script (`ExecutionPolicy`), le lancer sans
changer la configuration de la machine :

```powershell
powershell -ExecutionPolicy Bypass -File .\outils\verifier-environnement.ps1
```

```bash
./mvnw test      # tests unitaires seuls  — ~7 s, aucun Docker
./mvnw verify    # suite complète         — ~34 s, Docker requis
```

Ces deux commandes doivent être vertes sur un dépôt fraîchement cloné.

---

## Deux modes d'usage

| Mode | Pour qui | Point d'entrée |
|---|---|---|
| **Encadré** | formateur + groupe | ce README, puis les modules dans l'ordre ci-dessous |
| **Autonome** | seul, à son rythme | **[docs/00-parcours-autonome.md](docs/00-parcours-autonome.md)** |

Le mode autonome ajoute, pour chaque exercice, une fiche complète : énoncé,
checklist des cas attendus, indices progressifs et **vérification par sabotage**
— la méthode qui permet de savoir si ses tests valent quelque chose sans ouvrir
le corrigé.

---

## Parcours

| Module | Sujet | Ce qu'on y apprend | Docker | Durée |
|---|---|---|---|---|
| [`01-tests-unitaires`](01-tests-unitaires) | Domaine pur, **zéro Spring** | AAA, nommage, AssertJ, tests paramétrés, `Clock` injecté, quoi mocker | non | 1 h 30 |
| [`00-antipatterns`](00-antipatterns) | 10 mauvais tests et leur correction | ce qu'il ne faut pas faire, et pourquoi | non | 45 min |
| [`02-integration-bdd`](02-integration-bdd) | JPA + PostgreSQL (Testcontainers) | `@DataJpaTest` vs `@SpringBootTest`, rollback, verrouillage optimiste, migrations | oui | 1 h 30 |
| [`03-integration-rest`](03-integration-rest) | Contrôleur REST | `@WebMvcTest` vs bout en bout, `MockMvcTester`, `RestTestClient`, `@JsonTest` | oui | 1 h 30 |
| [`04-integration-pubsub`](04-integration-pubsub) | Google Cloud Pub/Sub | émulateur, `@DynamicPropertySource`, Awaitility, idempotence | oui | 1 h 30 |

Ordre conseillé en formation : **01 → 00 → 02 → 03 → 04**.

## Documentation

| Fiche | Contenu |
|---|---|
| [0 — Parcours autonome](docs/00-parcours-autonome.md) | **se former seul** : méthode, ordre, budget temps, que faire quand on bloque |
| [1 — Pyramide et vocabulaire](docs/01-pyramide-et-vocabulaire.md) | unitaire vs intégration, mock/stub/fake, chiffres mesurés |
| [2 — Un bon test unitaire](docs/02-bon-test-unitaire.md) | AAA, nommage, bornes, couverture |
| [3 — Un bon test d'intégration](docs/03-bon-test-integration.md) | périmètre, isolation, attente, piège du rollback |
| [4 — Testcontainers](docs/04-testcontainers.md) | `@ServiceConnection`, singleton, réutilisation, diagnostic |
| [9 — Aide-mémoire](docs/99-aide-memoire.md) | **tableau des changements Spring Boot 4**, AssertJ, Mockito, Awaitility |

---

## Organisation des tests

Chaque module suit la même structure :

```
src/test/java/fr/formation/banque/<chapitre>/
├── demo/       tests complets et commentés — support de la démonstration
├── exercice/   à compléter (@Disabled + TODO) — travail du stagiaire
└── corrige/    solution de référence
```

Les classes `exercice/` sont désactivées : le build reste vert dès le clone.
Pour travailler un exercice, retirer le `@Disabled` et suivre sa **fiche** dans
[`docs/exercices/`](docs/exercices) — le Javadoc de la classe n'en donne que le
résumé et le lien.

| Exercice | Module | Sujet | Fiche |
|---|---|---|---|
| 1 | 01 | tests paramétrés sur le barème de frais | [01-grille-frais](docs/exercices/01-grille-frais.md) |
| 2 | 01 | rendre le temps déterministe (`Clock`) | [02-horodatage](docs/exercices/02-horodatage.md) |
| 3 | 02 | verrouillage optimiste et contraintes de base | [03-verrouillage-optimiste](docs/exercices/03-verrouillage-optimiste.md) |
| 4 | 03 | validation et refus métier, slice **et** bout en bout | [04-validation-rest](docs/exercices/04-validation-rest.md) |
| 5 | 04 | chaîne complète HTTP → PostgreSQL → Pub/Sub | [05-chaine-complete](docs/exercices/05-chaine-complete.md) |

Chaque fiche contient l'énoncé, une checklist des cas attendus, des indices
progressifs, et la **vérification par sabotage** : les lignes exactes à casser
dans le code de production pour prouver que vos tests détectent une régression.

## Convention de nommage

| Suffixe | Nature | Plugin Maven | Commande | Docker |
|---|---|---|---|---|
| `*Test` | unitaire | surefire | `./mvnw test` | non |
| `*IT` | intégration | failsafe | `./mvnw verify` | oui |

Cette séparation est elle-même un enseignement du chapitre 1 : la suite rapide
doit rester lançable en permanence, sans dépendance externe.

---

## ⚠️ Ce projet utilise Spring Boot 4

Spring Boot 4 a réorganisé les annotations de test en modules séparés. **Les
exemples trouvés sur le web visent Spring Boot 3 et ne compilent pas ici.**

Les trois pièges les plus fréquents :

```java
// @MockBean n'existe plus
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// @WebMvcTest a changé de package
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

// @DataJpaTest aussi
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
```

Le tableau complet des changements est dans
[l'aide-mémoire](docs/99-aide-memoire.md).

## Versions

| Composant | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Cloud GCP | 8.2.1 |
| Testcontainers | 2.0.5 |
| JUnit Jupiter | 6.0.3 |
| Mockito | 5.23.0 |
| AssertJ | 3.27.7 |
| PostgreSQL | 17 (alpine) |

## Dépannage

| Symptôme | Solution |
|---|---|
| `Could not find a valid Docker environment` | démarrer Docker |
| `Could not find artifact fr.formation.banque:01-...` | ajouter `-am` : `./mvnw -pl 02-integration-bdd -am verify` |
| Premier `verify` très long | téléchargement des images (voir Prérequis) |
| `cannot find symbol: MockBean` | Spring Boot 4 : utiliser `@MockitoBean` |
