# 0. Parcours autonome — se former seul

Cette fiche est le **point d'entrée quand il n'y a pas de formateur**. Elle
explique comment travailler, et surtout comment savoir que votre travail est bon
sans ouvrir le corrigé.

---

## 1. Préflight — 10 minutes, une seule fois

```bash
bash outils/verifier-environnement.sh
```

Le script vérifie le JDK, Docker, télécharge les deux images et lance la suite
unitaire. Tant qu'il n'est pas vert, inutile d'attaquer les exercices : les
modules 02 à 05 ne démarreront pas.

En cas d'échec, voir la section *Dépannage* du [README racine](../README.md).

---

## 2. La méthode — quatre temps, toujours les mêmes

C'est le cœur du mode autonome. Le troisième temps remplace le formateur.

| # | Temps | Ce que vous faites |
|---|---|---|
| 1 | **Lire** | la fiche théorique du chapitre, puis les classes `demo/` du module. Elles sont commentées pour être lues, pas seulement exécutées. |
| 2 | **Écrire** | l'exercice, en ne suivant que sa fiche `docs/exercices/`. Cochez la checklist au fur et à mesure. |
| 3 | **Saboter** | cassez volontairement la ligne indiquée par la fiche dans le code de production, relancez. **Au moins un de vos tests doit passer au rouge.** Puis restaurez la ligne (`git checkout -- <fichier>`). |
| 4 | **Comparer** | ouvrez le corrigé et lisez-le **à côté** du vôtre. Ne recopiez pas : cherchez les cas que vous n'aviez pas vus, et ajoutez-les. |

### Pourquoi le sabotage

Un test vert ne prouve rien : il peut être vert parce qu'il n'assère rien
(anti-pattern 2), parce qu'il teste le mock (anti-pattern 1), ou parce qu'il
teste le milieu du palier et jamais les bornes. Le sabotage pose la seule
question qui compte : **mes tests échouent-ils quand le code devient faux ?**

Un test qui reste vert sur un code cassé est un coût sans contrepartie.

---

## 3. L'ordre et le budget temps

| Étape | Module | Docker | Durée |
|---|---|---|---|
| 1 | [Fiche 1 — pyramide et vocabulaire](01-pyramide-et-vocabulaire.md) | non | 15 min |
| 2 | [Fiche 2 — un bon test unitaire](02-bon-test-unitaire.md) | non | 20 min |
| 3 | `01-tests-unitaires` — démos + **exercices 1 et 2** | non | 1 h 30 |
| 4 | `00-antipatterns` — les 10 cas | non | 45 min |
| 5 | [Fiche 3 — un bon test d'intégration](03-bon-test-integration.md) + [fiche 4 — Testcontainers](04-testcontainers.md) | non | 30 min |
| 6 | `02-integration-bdd` — démos + **exercice 3** | oui | 1 h 30 |
| 7 | `03-integration-rest` — démos + **exercice 4** | oui | 1 h 30 |
| 8 | `04-integration-pubsub` — démos + **exercice 5** | oui | 1 h 30 |

Total : environ **8 heures**, réparties sur deux demi-journées. Les étapes 1 à 4
ne demandent aucun Docker et tiennent dans un train.

---

## 4. Les cinq exercices

| # | Fiche | Module | Sujet | Durée |
|---|---|---|---|---|
| 1 | [Barème de frais](exercices/01-grille-frais.md) | 01 | tests paramétrés, bornes | 40 min |
| 2 | [Date de valeur](exercices/02-horodatage.md) | 01 | `Clock` figé, déterminisme | 40 min |
| 3 | [Verrouillage optimiste](exercices/03-verrouillage-optimiste.md) | 02 | ce qu'un mock ne peut pas prouver | 50 min |
| 4 | [Validation REST](exercices/04-validation-rest.md) | 03 | slice **et** bout en bout | 50 min |
| 5 | [Chaîne complète](exercices/05-chaine-complete.md) | 04 | HTTP → PostgreSQL → Pub/Sub | 60 min |

Chaque classe d'exercice porte un `@Disabled` : le dépôt est donc vert dès le
clone. **Retirer l'annotation est la première étape de chaque exercice.**

---

## 5. Quand vous êtes bloqué

Dans cet ordre, et pas dans un autre :

1. **Relisez la classe `demo/` correspondante.** 80 % des exercices sont une
   variation sur une démo du même module.
2. **Indice 1** de la fiche — il oriente sans donner la réponse.
3. **Indice 2** — il donne la structure.
4. **Indice 3** — il donne presque le code.
5. **Le corrigé**, en dernier. Et dans ce cas, appliquez quand même le temps 3
   (sabotage) : recopier un corrigé sans vérifier qu'il détecte une régression
   n'apprend rien.
6. **Toujours bloqué après 15 minutes sur un problème technique** (Docker,
   compilation, Spring) : ce n'est plus de la pédagogie, c'est de
   l'environnement. Voir *Dépannage* dans le README racine.

---

## 6. Les deux commandes, et pourquoi il y en a deux

```bash
./mvnw test      # suffixe *Test  — unitaires   — aucun Docker  — ~7 s
./mvnw verify    # suffixe *IT    — intégration — Docker requis — ~35 s
```

Cette séparation est elle-même un enseignement : la suite rapide doit rester
lançable en permanence, sans dépendance externe. Pendant les exercices 1 et 2,
utilisez `./mvnw -pl 01-tests-unitaires test` — moins d'une seconde par boucle.

Pour ne lancer qu'un module d'intégration :

```bash
./mvnw -pl 02-integration-bdd -am verify
```

Le `-am` est obligatoire : il construit d'abord le module `01` dont les autres
dépendent.

---

## 7. Attention : ce projet utilise Spring Boot 4

Les exemples trouvés sur le web visent Spring Boot 3 et **ne compilent pas ici**
(`@MockBean` a disparu, `@WebMvcTest` et `@DataJpaTest` ont changé de package).
Avant de copier un extrait trouvé en ligne, vérifiez le tableau des changements
dans l'[aide-mémoire](99-aide-memoire.md).
