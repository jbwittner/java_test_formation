package fr.formation.banque.api;

import fr.formation.banque.domaine.Compte;
import java.math.BigDecimal;

/** Corps de la réponse GET /api/comptes/{iban}. */
public record ReponseCompte(String iban, BigDecimal solde) {

    public static ReponseCompte depuis(Compte compte) {
        return new ReponseCompte(compte.iban(), compte.solde().valeur());
    }
}
