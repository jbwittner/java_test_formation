package fr.formation.banque.api;

import fr.formation.banque.domaine.Virement;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Corps de la réponse 201.
 *
 * <p>DTO distinct de {@link Virement} : le contrat HTTP peut ainsi évoluer
 * indépendamment du domaine, et un test de sérialisation ({@code @JsonTest})
 * porte sur une classe stable.
 */
public record ReponseVirement(
        String reference,
        String ibanSource,
        String ibanDestination,
        BigDecimal montant,
        BigDecimal frais,
        BigDecimal totalDebite,
        String devise,
        LocalDate dateDeValeur) {

    public static ReponseVirement depuis(Virement virement) {
        return new ReponseVirement(
                virement.reference(),
                virement.ibanSource(),
                virement.ibanDestination(),
                virement.montant().valeur(),
                virement.frais().valeur(),
                virement.totalDebite().valeur(),
                virement.montant().devise().name(),
                virement.dateDeValeur());
    }
}
