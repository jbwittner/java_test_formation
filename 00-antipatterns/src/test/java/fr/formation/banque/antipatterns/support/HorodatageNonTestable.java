package fr.formation.banque.antipatterns.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Version « réaliste » — et non testable — de l'horodatage : l'horloge est
 * appelée en statique, au cœur de la règle.
 *
 * <p>Ce code n'est pas faux, il est <b>impossible à tester correctement</b> :
 * aucun test ne peut choisir l'instant d'exécution. C'est exactement ce qu'on
 * trouve dans la plupart des bases de code, et la première chose à refactorer
 * quand on veut ajouter des tests.
 */
public class HorodatageNonTestable {

    public static final LocalTime HEURE_DE_COUPURE = LocalTime.of(16, 0);

    public LocalDate dateDeValeur() {
        LocalDateTime maintenant = LocalDateTime.now(); // <- la dépendance cachée
        LocalDate candidate = maintenant.toLocalDate();
        if (!maintenant.toLocalTime().isBefore(HEURE_DE_COUPURE)) {
            candidate = candidate.plusDays(1);
        }
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
                || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
