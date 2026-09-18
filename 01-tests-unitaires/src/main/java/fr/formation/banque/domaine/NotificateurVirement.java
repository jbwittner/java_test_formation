package fr.formation.banque.domaine;

/**
 * Port sortant de notification, implémenté au chapitre 04 par un publieur Pub/Sub.
 *
 * <p>Le domaine ignore totalement qu'il existe un broker de messages : il publie
 * un fait métier. C'est ce découplage qui permet de tester unitairement la règle
 * de virement sans démarrer d'émulateur.
 */
@FunctionalInterface
public interface NotificateurVirement {

    void virementExecute(Virement virement);
}
