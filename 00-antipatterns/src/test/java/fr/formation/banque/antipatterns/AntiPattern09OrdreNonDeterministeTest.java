package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 9 — asserter sur un ordre non garanti.
 *
 * <p><b>Symptôme</b> : {@code containsExactly} sur le résultat d'un
 * {@link HashSet}, d'une {@code HashMap}, d'un {@code parallelStream}, ou d'une
 * requête SQL sans {@code ORDER BY}.
 *
 * <p><b>Pourquoi c'est grave</b> : l'ordre observé est un accident — de la
 * fonction de hachage, de la taille interne de la table, de la version de la
 * JVM, du plan d'exécution choisi par la base. Le test passe cent fois puis
 * échoue après un simple ajout de donnée. Comme l'échec n'est pas reproductible
 * localement, il est classé « bizarrerie de l'intégration continue ».
 *
 * <p>Ce piège revient au chapitre 02 : <b>un {@code findAll()} sans
 * {@code ORDER BY} ne garantit aucun ordre</b>, même si PostgreSQL semble
 * toujours renvoyer les lignes dans le même sens.
 *
 * <p><b>Correction</b> : soit rendre l'ordre explicite (trier avant d'asserter,
 * ajouter un {@code ORDER BY}), soit asserter sans ordre —
 * {@code containsExactlyInAnyOrder}.
 */
@DisplayName("Anti-pattern 9 — asserter sur un ordre non garanti")
class AntiPattern09OrdreNonDeterministeTest {

    private static Set<String> ibansEnSet() {
        Set<String> ibans = new HashSet<>();
        ibans.add("FR76-C");
        ibans.add("FR76-A");
        ibans.add("FR76-B");
        return ibans;
    }

    private static List<Compte> comptes() {
        return Stream.of("FR76-C", "FR76-A", "FR76-B")
                .map(iban -> new Compte(iban, Montant.euros("100.00")))
                .toList();
    }

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @Disabled("""
                Volontairement desactive : l'assertion depend de l'ordre
                d'iteration d'un HashSet, qui n'est garanti par aucun contrat.
                Retirer le @Disabled et ajouter d'autres IBAN pour voir
                l'ordre changer -- et le test devenir rouge sans raison metier.""")
        @DisplayName("assère l'ordre d'itération d'un HashSet")
        void testIbans() {
            assertThat(ibansEnSet()).containsExactly("FR76-A", "FR76-B", "FR76-C");
        }

        @Test
        @DisplayName("assère l'ordre d'un flux parallèle")
        void testComptesParallele() {
            // forEach sur un parallelStream n'a AUCUN ordre garanti ; ce test
            // ne passe ici que parce que .toList() reconstruit l'ordre source.
            // Il basculerait au rouge si quelqu'un remplaçait toList() par
            // une collecte dans un Set ou par forEach + ajout concurrent.
            List<String> ibans = comptes().parallelStream().map(Compte::iban).toList();

            assertThat(ibans).containsExactly("FR76-C", "FR76-A", "FR76-B");
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("assère le contenu sans présumer de l'ordre")
        void devrait_contenir_les_trois_ibans_quel_que_soit_l_ordre() {
            assertThat(ibansEnSet())
                    .containsExactlyInAnyOrder("FR76-A", "FR76-B", "FR76-C");
        }

        @Test
        @DisplayName("rend l'ordre explicite quand l'ordre fait partie du contrat")
        void devrait_renvoyer_les_ibans_tries_quand_le_tri_est_demande() {
            // Si l'ordre compte vraiment, il doit être PRODUIT par le code testé,
            // pas espéré. Ici on trie explicitement, puis on peut asserter l'ordre.
            List<String> ibans = comptes().stream().map(Compte::iban).sorted().toList();

            assertThat(ibans).containsExactly("FR76-A", "FR76-B", "FR76-C");
        }
    }
}
