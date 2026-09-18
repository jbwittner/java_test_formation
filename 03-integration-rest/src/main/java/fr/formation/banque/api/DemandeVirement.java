package fr.formation.banque.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Corps de la requête POST /api/virements.
 *
 * <p>Les contraintes de validation constituent le <b>contrat d'entrée</b> de
 * l'API : elles se testent au niveau de la slice web, sans base de données ni
 * service métier. Elles ne remplacent pas les règles du domaine — elles les
 * précèdent, pour renvoyer un 400 clair plutôt qu'une erreur interne.
 */
public record DemandeVirement(
        @NotBlank(message = "L'IBAN source est obligatoire")
        String ibanSource,

        @NotBlank(message = "L'IBAN de destination est obligatoire")
        String ibanDestination,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être d'au moins 0,01")
        @Digits(integer = 17, fraction = 2, message = "Le montant admet au plus 2 décimales")
        BigDecimal montant) {
}
