package fr.formation.banque.api;

import fr.formation.banque.domaine.Compte;
import java.math.BigDecimal;

/** Corps de la réponse GET /api/comptes/{iban}. */
public record ReponseCompte(
        String iban,
        String type,
        BigDecimal solde,
        BigDecimal decouvertAutorise,
        BigDecimal montantDisponible,
        String devise) {

    public static ReponseCompte depuis(Compte compte) {
        return new ReponseCompte(
                compte.iban(),
                compte.type().name(),
                compte.solde().valeur(),
                compte.decouvertAutorise().valeur(),
                compte.montantDisponible().valeur(),
                compte.solde().devise().name());
    }
}
