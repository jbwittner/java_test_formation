package fr.formation.banque.unitaire.exercice;

import fr.formation.banque.domaine.HorodatageVirement;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EXERCICE 2 — rendre le temps testable.
 *
 * <p><b>Règles implémentées par {@link HorodatageVirement} :</b>
 * <ul>
 *   <li>avant 16 h : date de valeur = aujourd'hui</li>
 *   <li>à partir de 16 h (borne incluse) : date de valeur = jour suivant</li>
 *   <li>samedi et dimanche ne sont jamais des dates de valeur : on reporte au lundi</li>
 * </ul>
 *
 * <p><b>Consignes</b>
 * <ol>
 *   <li>Retirer le {@code @Disabled}.</li>
 *   <li>Écrire un test pour chacun de ces cas, en figeant l'horloge avec
 *       {@code Clock.fixed(Instant.parse("..."), ZoneId.of("Europe/Paris"))} :
 *       <ul>
 *         <li>mardi 10 h</li>
 *         <li>mardi 15 h 59 min 59 s</li>
 *         <li>mardi 16 h 00 pile</li>
 *         <li>vendredi 17 h</li>
 *         <li>samedi 10 h</li>
 *       </ul>
 *   </li>
 *   <li>Regrouper ensuite ces cas dans un seul {@code @ParameterizedTest}.</li>
 * </ol>
 *
 * <p><b>Attention au fuseau</b> : un {@code Instant} est exprimé en UTC. En juin,
 * Paris est à UTC+2 — {@code 2025-06-03T14:00:00Z} correspond donc à 16 h 00
 * locales. Ne jamais utiliser {@code ZoneId.systemDefault()} dans un test : cela
 * réintroduit la dépendance à l'environnement que l'on cherche à éliminer.
 *
 * <p><b>Question de fin d'exercice</b> : si {@code HorodatageVirement} appelait
 * {@code LocalDateTime.now()} au lieu de recevoir un {@code Clock}, comment
 * testeriez-vous la règle du vendredi 17 h ? Combien de temps le test
 * prendrait-il, et serait-il fiable en intégration continue ?
 *
 * <p>Corrigé : {@code fr.formation.banque.unitaire.corrige.HorodatageVirementCorrigeTest}
 */
@Disabled("TODO exercice 2 — retirer cette annotation puis écrire les tests")
@DisplayName("EXERCICE 2 — HorodatageVirement")
class HorodatageVirementExerciceTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Test
    @DisplayName("amorçage : mardi 10 h, la date de valeur est le jour même")
    void devrait_dater_au_jour_meme_quand_l_heure_est_avant_la_coupure() {
        // Arrange
        // TODO : Clock horloge = Clock.fixed(Instant.parse("2025-06-03T08:00:00Z"), PARIS);
        Clock horloge = Clock.systemDefaultZone(); // <- à remplacer : non déterministe !
        HorodatageVirement horodatage = new HorodatageVirement(horloge);

        // Act
        horodatage.dateDeValeur();

        // Assert
        // TODO : asserter que la date de valeur vaut LocalDate.of(2025, 6, 3)
    }

    // TODO : mardi 15 h 59 min 59 s -> jour même

    // TODO : mardi 16 h 00 pile -> lendemain

    // TODO : vendredi 17 h -> lundi suivant

    // TODO : samedi 10 h -> lundi suivant
}
