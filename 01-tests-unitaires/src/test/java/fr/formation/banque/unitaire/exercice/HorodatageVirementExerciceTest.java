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
 * <p>Figer l'horloge avec {@link Clock#fixed} pour tester la coupure de 16 h et
 * le report du week-end, sans jamais dépendre de l'instant d'exécution.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/02-horodatage.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.unitaire.corrige.HorodatageVirementCorrigeTest}
 */
@Disabled("Exercice 2 — voir docs/exercices/02-horodatage.md, puis retirer cette annotation")
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
