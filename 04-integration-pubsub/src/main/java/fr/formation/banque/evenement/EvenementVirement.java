package fr.formation.banque.evenement;

import fr.formation.banque.domaine.Virement;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Charge utile publiée sur Pub/Sub.
 *
 * <p>C'est un <b>contrat inter-applications</b> : d'autres services le
 * désérialisent. Tout renommage de champ est une rupture, au même titre qu'un
 * changement d'URL d'API. Il mérite donc ses propres tests — et il est
 * volontairement distinct de {@link Virement}, pour que le domaine puisse
 * évoluer sans casser les consommateurs.
 */
public record EvenementVirement(
        String reference,
        String ibanSource,
        String ibanDestination,
        BigDecimal montant,
        BigDecimal frais,
        String devise,
        LocalDate dateDeValeur) {

    public static EvenementVirement depuis(Virement virement) {
        return new EvenementVirement(
                virement.reference(),
                virement.ibanSource(),
                virement.ibanDestination(),
                virement.montant().valeur(),
                virement.frais().valeur(),
                virement.montant().devise().name(),
                virement.dateDeValeur());
    }
}
