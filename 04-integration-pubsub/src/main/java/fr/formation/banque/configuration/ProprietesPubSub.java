package fr.formation.banque.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Noms de topic et de souscription, externalisés.
 *
 * <p>Les figer dans le code obligerait les tests à utiliser les mêmes noms qu'en
 * production — ou à les réécrire. Les externaliser permet à chaque test de
 * travailler sur ses propres ressources, donc de rester isolé.
 *
 * @param topicSortant       topic sur lequel les virements exécutés sont publiés
 * @param souscriptionEntrante souscription consommée par cette application
 * @param creerAuDemarrage   crée topic et souscription s'ils n'existent pas
 *                           (vrai sur l'émulateur, faux en production où c'est
 *                           l'infrastructure qui les provisionne)
 */
@ConfigurationProperties(prefix = "banque.pubsub")
public record ProprietesPubSub(
        String topicSortant,
        String souscriptionEntrante,
        boolean creerAuDemarrage) {
}
