package fr.formation.banque.domaine;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Calcule la date de valeur d'un virement.
 *
 * <p>LEÇON CENTRALE : le {@link Clock} est injecté, jamais appelé en statique.
 * Écrire {@code LocalDateTime.now()} au cœur de cette règle rendrait le test
 * dépendant de l'heure d'exécution — vert le matin, rouge après 16 h, rouge le
 * week-end. Avec un {@code Clock}, chaque cas limite devient un test déterministe
 * d'une ligne : {@code Clock.fixed(...)}.
 *
 * <p>Règles :
 * <ul>
 *   <li>avant l'heure de coupure (16 h) : date de valeur = aujourd'hui</li>
 *   <li>à partir de 16 h : date de valeur = jour ouvré suivant</li>
 *   <li>samedi et dimanche ne sont jamais des dates de valeur</li>
 * </ul>
 */
public class HorodatageVirement {

    public static final LocalTime HEURE_DE_COUPURE = LocalTime.of(16, 0);

    private final Clock horloge;

    public HorodatageVirement(Clock horloge) {
        this.horloge = Objects.requireNonNull(horloge, "horloge");
    }

    public LocalDate dateDeValeur() {
        ZonedDateTime maintenant = ZonedDateTime.now(horloge);
        LocalDate candidate = maintenant.toLocalDate();
        if (!maintenant.toLocalTime().isBefore(HEURE_DE_COUPURE)) {
            candidate = candidate.plusDays(1);
        }
        return prochainJourOuvre(candidate);
    }

    private static LocalDate prochainJourOuvre(LocalDate date) {
        LocalDate resultat = date;
        while (resultat.getDayOfWeek() == DayOfWeek.SATURDAY
                || resultat.getDayOfWeek() == DayOfWeek.SUNDAY) {
            resultat = resultat.plusDays(1);
        }
        return resultat;
    }
}
