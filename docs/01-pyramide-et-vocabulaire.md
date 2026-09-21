# 1. Pyramide et vocabulaire

## Le seul critère qui compte

« Test unitaire » et « test d'intégration » sont des mots piégés : chacun a sa
définition. Plutôt que de débattre, on retient un critère opérationnel unique :

> **Un test est unitaire s'il s'exécute en mémoire, sans aucune dépendance
> externe, en quelques millisecondes.**
> **Il est d'intégration dès qu'il a besoin de quelque chose qui vit en dehors de
> la JVM** : une base, un broker, un serveur HTTP, un système de fichiers.

Ce critère est vérifiable, il ne dépend d'aucune opinion, et il se traduit
directement dans le build : `*Test` pour les uns, `*IT` pour les autres.

## Les trois questions à se poser

| Question | Test unitaire | Test d'intégration |
|---|---|---|
| Qu'est-ce que je vérifie ? | une **règle métier** | un **branchement** |
| Qui me le dit ? | le code que j'ai écrit | une technologie extérieure |
| Combien ça coûte ? | quelques millisecondes | de 0,5 à plusieurs secondes |

## La pyramide, et ce qu'elle veut dire

```
        /\        Frontière complète      -> quelques-uns
       /  \       (03 serveur HTTP réel, 04 émulateur Pub/Sub)
      /----\
     /      \     Intégration ciblée      -> quelques dizaines
    /        \    (02 base, 03 slice web, 04 adaptateurs)
   /----------\
  /            \  Unitaires               -> des centaines
 /______________\ (01 domaine, 00 anti-patterns)
```

**Ce dépôt ne contient volontairement aucun test bout en bout.** Chaque
adaptateur est testé contre sa vraie technologie — PostgreSQL au chapitre 02, un
serveur HTTP au chapitre 03, un broker Pub/Sub au chapitre 04 — mais **avec ses
voisins mockés**. Chaque couche est ainsi prouvée une fois, là où elle est la
moins chère à prouver. Le prix de ce choix est explicite : aucun test ne vérifie
que les trois adaptateurs fonctionnent *ensemble*. C'est un arbitrage assumé, et
une bonne question à se poser sur son propre projet.

La forme n'est pas un dogme esthétique, c'est une conséquence économique.
Chiffres **mesurés sur ce dépôt** :

| Suite | Commande | Tests | Durée | Docker |
|---|---|---|---|---|
| Unitaires | `./mvnw test` | 100 | **~7 s** | non |
| Tout | `./mvnw verify` | 140 | **~25 s** | oui |

Les 40 tests d'intégration coûtent plus cher que les 100 unitaires réunis. Si
l'on inversait les proportions, la suite passerait à plusieurs minutes — et une
suite lente finit par ne plus être lancée.

## Ce que chaque niveau attrape

Exemples réels rencontrés pendant la construction de ce dépôt :

| Bug | Attrapé par |
|---|---|
| Frais faux au-delà de 10 000 € | test unitaire de `GrilleFrais` |
| Date de valeur fausse vendredi 17 h | test unitaire avec `Clock.fixed` |
| Migration Flyway non appliquée | test d'intégration base (`missing table [compte]`) |
| Mise à jour perdue en concurrence | test d'intégration base (`@Version`) |
| 500 au lieu de 409 sur solde insuffisant | slice `@WebMvcTest` |
| Objet non sérialisable pour Pub/Sub | test d'intégration Pub/Sub — **invisible avec un mock** |

Aucun niveau ne remplace l'autre. Le dernier cas est particulièrement parlant :
le test unitaire de `PublieurVirementPubSub` passait, avec un mock qui acceptait
n'importe quel objet. Le vrai broker, lui, exigeait un convertisseur JSON.

## Le vocabulaire des doublures

| Terme | Définition | Exemple dans ce dépôt |
|---|---|---|
| **Mock** | objet programmé, dont on vérifie les appels | `@Mock CompteRepository` |
| **Stub** | renvoie une réponse figée, sans vérification | `when(...).thenReturn(...)` |
| **Fake** | implémentation réelle mais simplifiée | `DepotComptes` (en mémoire) |
| **Spy** | objet réel dont on observe certains appels | `@MockitoSpyBean` |
| **Dummy** | valeur de remplissage, jamais utilisée | `v -> { }` comme notificateur |

Règle pratique : **préférer un fake à un mock** dès que la doublure est
sollicitée plusieurs fois. Le test décrit alors un état, pas une suite d'appels,
et il survit aux remaniements (cf. anti-pattern 8).

## Ce qui ne se mocke jamais

- les **value objects** (`Montant`) : on les instancie ;
- les **entités du domaine** (`Compte`) : les mocker supprime la règle testée ;
- les **fonctions pures** (`GrilleFrais`) : elles sont déjà rapides et déterministes.

On mocke ce qui **sort du processus** : base, broker, HTTP, horloge système,
générateur aléatoire.
