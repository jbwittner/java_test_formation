package fr.formation.banque.integrationrest.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteIntrouvableException;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.Virement;
import fr.formation.banque.integrationrest.support.SocleCoucheRest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * DÉMO 6 — la couche REST sur un <b>vrai serveur HTTP</b>, service mocké.
 *
 * <p><b>Chaîne réellement exercée</b> : requête émise sur une socket, sur un port
 * réel → conteneur servlet → désérialisation JSON → validation → routage →
 * contrôleur → {@code @RestControllerAdvice} → sérialisation de la réponse →
 * réponse HTTP lue par le client. <b>Le domaine s'arrête au mock</b> : ni règle
 * métier, ni base de données.
 *
 * <p><b>Pourquoi s'arrêter là ?</b> Parce que chaque couche est déjà prouvée
 * ailleurs, et qu'un test qui rejoue ce qui est déjà prouvé coûte du temps sans
 * rien ajouter :
 * <ul>
 *   <li>les règles de virement et le calcul des frais — chapitre 01, en test
 *       unitaire, en quelques millisecondes ;</li>
 *   <li>la persistance et les transactions — chapitre 02, sur une vraie
 *       PostgreSQL ;</li>
 *   <li>la messagerie, publication et consommation — chapitre 04.</li>
 * </ul>
 * Ici, le sujet est le <b>contrat HTTP</b>, et uniquement lui.
 *
 * <p><b>Ce que ce test prouve et que la slice {@code @WebMvcTest} ne prouve pas</b> :
 * <ul>
 *   <li>que le câblage Spring de l'application démarre réellement — la slice ne
 *       charge que le contrôleur, un bean manquant y passe inaperçu ;</li>
 *   <li>que la requête traverse une <b>vraie pile HTTP</b> : socket, en-têtes,
 *       négociation de contenu, codage du corps. {@code MockMvc} court-circuite
 *       tout cela et appelle la chaîne MVC en mémoire ;</li>
 *   <li>que le serveur embarqué est correctement configuré.</li>
 * </ul>
 *
 * <table border="1">
 *   <caption>Slice vs serveur réel</caption>
 *   <tr><th></th><th>{@code @WebMvcTest}</th><th>{@code @SpringBootTest} + RestTestClient</th></tr>
 *   <tr><td>Contrat HTTP (codes, JSON)</td><td>✔</td><td>✔</td></tr>
 *   <tr><td>Validation des entrées</td><td>✔</td><td>✔</td></tr>
 *   <tr><td>Vraie socket, vrai serveur</td><td>✘</td><td>✔</td></tr>
 *   <tr><td>Câblage Spring de l'application</td><td>✘</td><td>✔</td></tr>
 *   <tr><td>Règles métier réelles</td><td>✘ (mockées)</td><td>✘ (mockées)</td></tr>
 *   <tr><td>Persistance réelle</td><td>✘</td><td>✘</td></tr>
 *   <tr><td>Ordre de grandeur</td><td>~0,3 s</td><td>~1,5 s</td></tr>
 * </table>
 *
 * <p>Conséquence sur la répartition : <b>beaucoup</b> de tests de slice pour les
 * variantes du contrat et les cas d'erreur, <b>peu</b> de tests sur serveur réel,
 * sur les parcours qui comptent.
 *
 * <p>{@link RestTestClient} est le client de test de Spring Boot 4 ; il remplace
 * {@code TestRestTemplate} et propose la même API fluide que {@code WebTestClient}.
 * Voir {@link SocleCoucheRest} pour la configuration du contexte.
 */
@DisplayName("Virement — couche HTTP sur un vrai serveur (service mocké)")
class VirementHttpIT extends SocleCoucheRest {

    @Autowired
    private RestTestClient client;

    // Le seul collaborateur du contrôleur, remplacé dans le contexte de
    // l'application : la frontière du test est exactement la couche REST.
    @MockitoBean
    private ServiceVirement virements;

    @Test
    @DisplayName("exécute le virement et renvoie 201 avec l'en-tête Location")
    void devrait_renvoyer_201_et_appeler_le_service_quand_le_virement_est_accepte() {
        when(virements.executer(eq("FR76-SOURCE"), eq("FR76-DEST"), eq(Montant.euros("1000.00"))))
                .thenReturn(new Virement("VIR-1", "FR76-SOURCE", "FR76-DEST",
                        Montant.euros("1000.00"), Montant.euros("1.00"),
                        LocalDate.of(2025, 6, 3)));

        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/virements/VIR-1")
                .expectBody()
                .jsonPath("$.reference").isEqualTo("VIR-1")
                .jsonPath("$.montant").isEqualTo(1000.00)
                .jsonPath("$.frais").isEqualTo(1.00)
                .jsonPath("$.totalDebite").isEqualTo(1001.00)
                .jsonPath("$.dateDeValeur").isEqualTo("2025-06-03");

        // L'assertion qui délimite la couche : le JSON reçu a bien été traduit
        // en un appel au service, avec les bons arguments. Ce que le service en
        // fait ensuite n'est pas le sujet de ce test.
        verify(virements).executer("FR76-SOURCE", "FR76-DEST", Montant.euros("1000.00"));
    }

    @Test
    @DisplayName("traduit SoldeInsuffisantException en 409 problem+json")
    void devrait_renvoyer_409_quand_le_service_signale_un_solde_insuffisant() {
        when(virements.executer(any(), any(), any()))
                .thenThrow(new SoldeInsuffisantException("FR76-SOURCE",
                        Montant.euros("1000.00"), Montant.euros("10.00")));

        // 409 et non 400 : la requête est bien formée, c'est l'état du compte
        // qui empêche l'opération. Ce choix fait partie du contrat public.
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Solde insuffisant")
                .jsonPath("$.iban").isEqualTo("FR76-SOURCE");
    }

    @Test
    @DisplayName("traduit CompteIntrouvableException en 404")
    void devrait_renvoyer_404_quand_le_service_signale_un_compte_inconnu() {
        when(virements.executer(any(), any(), any()))
                .thenThrow(new CompteIntrouvableException("FR76-FANTOME"));

        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-FANTOME","ibanDestination":"FR76-DEST","montant":10.00}
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("renvoie 400 sans appeler le service quand la demande est invalide")
    void devrait_renvoyer_400_et_ne_pas_appeler_le_service_quand_la_demande_est_invalide() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"","ibanDestination":"FR76-DEST","montant":-5}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.champs.ibanSource").exists()
                .jsonPath("$.champs.montant").exists();

        // Point essentiel : la validation coupe AVANT le service. Un test qui ne
        // vérifierait que le code 400 laisserait passer une implémentation qui
        // exécute quand même le virement puis renvoie 400.
        verifyNoInteractions(virements);
    }

    @Test
    @DisplayName("expose le compte renvoyé par le service")
    void devrait_exposer_le_compte_quand_le_service_le_renvoie() {
        when(virements.consulter("FR76-SOURCE")).thenReturn(
                new Compte("FR76-SOURCE", Montant.euros("5000.00")));

        client.get().uri("/api/comptes/FR76-SOURCE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.iban").isEqualTo("FR76-SOURCE")
                .jsonPath("$.solde").isEqualTo(5000.00);
    }
}
