# 00 — Anti-patterns

Dix mauvais tests, chacun accompagné de sa correction, dans la même classe :
`@Nested class Mauvais` / `@Nested class Bon`.

| # | Anti-pattern | Classe |
|---|---|---|
| 1 | Tout mocker, y compris le domaine | `AntiPattern01ToutMockerTest` |
| 2 | Ne rien assérer | `AntiPattern02NeRienAsserterTest` |
| 3 | `Thread.sleep` | `AntiPattern03ThreadSleepTest` |
| 4 | État partagé entre les tests | `AntiPattern04EtatPartageTest` |
| 5 | Horloge non injectée | `AntiPattern05HorlogeNonInjecteeTest` |
| 6 | Tester les accesseurs | `AntiPattern06TesterLesAccesseursTest` |
| 7 | Le test fourre-tout | `AntiPattern07TestFourreToutTest` |
| 8 | Mocks sur-spécifiés | `AntiPattern08MocksSurSpecifiesTest` |
| 9 | Asserter sur un ordre non garanti | `AntiPattern09OrdreNonDeterministeTest` |
| 10 | Nommage muet | `AntiPattern10NommageTest` |

## Les tests désactivés

Quatre tests portent un `@Disabled` : ce sont ceux qui **échouent réellement**
(flaky, dépendants de l'ordre). Leur message explique quoi faire pour observer
l'échec en séance. Les autres « mauvais » tests restent **actifs et verts** —
c'est précisément le propos : ils passent et ne servent à rien.

## Lancer

```bash
./mvnw -pl 00-antipatterns -am test
```
