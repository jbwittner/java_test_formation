package fr.formation.banque.domaine;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Orchestration d'un virement : charger, appliquer les règles, enregistrer, notifier.
 *
 * <p>Le service ne contient AUCUNE règle métier : les frais viennent de
 * {@link GrilleFrais}, le contrôle de solde de {@link Compte}, la date de
 * {@link HorodatageVirement}. Il se contente de coordonner. C'est pour cela que
 * son test unitaire est court et que ses collaborateurs se mockent sans gêne.
 */
public class ServiceVirement {

    private final CompteRepository comptes;
    private final HorodatageVirement horodatage;
    private final GenerateurReference references;
    private final NotificateurVirement notificateur;

    public ServiceVirement(CompteRepository comptes,
                           HorodatageVirement horodatage,
                           GenerateurReference references,
                           NotificateurVirement notificateur) {
        this.comptes = Objects.requireNonNull(comptes, "comptes");
        this.horodatage = Objects.requireNonNull(horodatage, "horodatage");
        this.references = Objects.requireNonNull(references, "references");
        this.notificateur = Objects.requireNonNull(notificateur, "notificateur");
    }

    /**
     * Exécute un virement entre deux comptes.
     *
     * @throws CompteIntrouvableException si l'un des deux IBAN est inconnu
     * @throws SoldeInsuffisantException  si le compte source ne couvre pas montant + frais
     * @throws IllegalArgumentException   si les deux IBAN sont identiques ou le montant invalide
     */
    public Virement executer(String ibanSource, String ibanDestination, Montant montant) {
        if (Objects.equals(ibanSource, ibanDestination)) {
            throw new IllegalArgumentException("Un virement doit relier deux comptes distincts");
        }
        Compte source = charger(ibanSource);
        Compte destination = charger(ibanDestination);

        Montant frais = GrilleFrais.calculer(montant);

        // Le débit est tenté AVANT le crédit : si le solde est insuffisant,
        // l'exception remonte et aucun compte n'a été modifié.
        source.debiter(montant.plus(frais));
        destination.crediter(montant);

        comptes.enregistrer(source);
        comptes.enregistrer(destination);

        LocalDate dateDeValeur = horodatage.dateDeValeur();
        Virement virement = new Virement(
                references.suivante(), ibanSource, ibanDestination, montant, frais, dateDeValeur);

        notificateur.virementExecute(virement);
        return virement;
    }

    private Compte charger(String iban) {
        return comptes.parIban(iban).orElseThrow(() -> new CompteIntrouvableException(iban));
    }
}
