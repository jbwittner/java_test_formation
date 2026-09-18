package fr.formation.banque.unitaire.corrige;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.domaine.HorodatageVirement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * CORRIGÉ — exercice 2 : rendre le temps déterministe.
 *
 * <p>Sans {@link Clock} injecté, ces sept cas seraient <b>intestables</b> : il
 * faudrait attendre vendredi 16 h 01 pour vérifier une règle. Avec un
 * {@code Clock.fixed}, chacun devient une ligne de CSV.
 *
 * <p>Deux pièges à souligner en formation :
 * <ol>
 *   <li>Un {@link Instant} est un point sur l'axe du temps, sans fuseau. C'est la
 *       {@link ZoneId} qui décide s'il est 16 h ou 18 h. Fixer l'horloge sur
 *       {@code systemDefault()} ramènerait la dépendance à l'environnement que
 *       l'on cherche justement à supprimer — la zone est donc explicite.</li>
 *   <li>En juin, Paris est à UTC+2 : 14 h 00 UTC correspond à 16 h 00 locales.
 *       Les instants ci-dessous sont écrits en UTC, l'heure locale est en commentaire.</li>
 * </ol>
 */
@DisplayName("HorodatageVirement — date de valeur")
class HorodatageVirementCorrigeTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static LocalDate dateDeValeurPour(String instantUtc) {
        Clock horloge = Clock.fixed(Instant.parse(instantUtc), PARIS);
        return new HorodatageVirement(horloge).dateDeValeur();
    }

    @ParameterizedTest(name = "{2} -> date de valeur {1}")
    @CsvSource({
            // instant UTC,           date attendue, description (heure locale Paris)
            "2025-06-03T06:00:00Z,    2025-06-03,    'mardi 08h00 : bien avant la coupure'",
            "2025-06-03T13:59:59Z,    2025-06-03,    'mardi 15h59m59 : dernière seconde avant la coupure'",
            "2025-06-03T14:00:00Z,    2025-06-04,    'mardi 16h00 pile : la coupure est INCLUSIVE, on bascule'",
            "2025-06-03T20:00:00Z,    2025-06-04,    'mardi 22h00 : après la coupure'",
            "2025-06-06T15:00:00Z,    2025-06-09,    'vendredi 17h00 : J+1 tombe samedi, report au lundi'",
            "2025-06-07T08:00:00Z,    2025-06-09,    'samedi 10h00 : jamais de date de valeur le week-end'",
            "2025-06-08T08:00:00Z,    2025-06-09,    'dimanche 10h00 : report au lundi'"
    })
    @DisplayName("applique la coupure de 16 h et saute les week-ends")
    void devrait_calculer_la_date_de_valeur_selon_l_heure_et_le_jour(
            String instantUtc, LocalDate dateAttendue, String description) {
        assertThat(dateDeValeurPour(instantUtc))
                .as(description)
                .isEqualTo(dateAttendue);
    }

    @Test
    @DisplayName("reste déterministe : deux appels successifs donnent la même date")
    void devrait_renvoyer_la_meme_date_quand_l_horloge_est_figee() {
        // Cette propriété est ce que le Clock injecté achète : le test ne dépend
        // plus de l'instant où l'intégration continue l'exécute.
        HorodatageVirement horodatage =
                new HorodatageVirement(Clock.fixed(Instant.parse("2025-06-06T15:00:00Z"), PARIS));

        assertThat(horodatage.dateDeValeur()).isEqualTo(horodatage.dateDeValeur());
    }
}
