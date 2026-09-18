package fr.formation.banque.integrationrest.demo;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.TypeCompte;
import fr.formation.banque.integrationrest.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * DÉMO 6 — le test bout en bout.
 *
 * <p><b>Chaîne réellement exercée</b> : requête HTTP sur un vrai port →
 * sérialisation JSON → validation → contrôleur → {@code ServiceVirement} →
 * règles du domaine → adaptateur JPA → PostgreSQL (Testcontainers) →
 * réponse HTTP. Aucun mock nulle part.
 *
 * <p><b>Ce que ce test prouve et que la slice ne prouve pas</b> :
 * <ul>
 *   <li>que le câblage Spring est complet (un bean manquant casse ici, pas
 *       dans la slice où il est mocké) ;</li>
 *   <li>que les frais calculés par le domaine arrivent bien jusqu'au JSON ;</li>
 *   <li>que le débit est réellement <b>persisté</b> — une transaction mal posée
 *       se voit ici et nulle part ailleurs ;</li>
 *   <li>que les deux comptes sont cohérents après l'opération.</li>
 * </ul>
 *
 * <p><b>Ce qu'il coûte</b> : démarrage du contexte complet + conteneur
 * PostgreSQL, soit quelques secondes contre quelques centaines de millisecondes
 * pour la slice. D'où la répartition : <b>peu</b> de tests bout en bout, sur les
 * parcours qui comptent — et <b>beaucoup</b> de tests de slice pour les cas
 * d'erreur et les variantes de contrat.
 *
 * <table border="1">
 *   <caption>Slice vs bout en bout</caption>
 *   <tr><th></th><th>{@code @WebMvcTest}</th><th>{@code @SpringBootTest} + RestTestClient</th></tr>
 *   <tr><td>Contrat HTTP (codes, JSON)</td><td>✔</td><td>✔</td></tr>
 *   <tr><td>Validation des entrées</td><td>✔</td><td>✔</td></tr>
 *   <tr><td>Câblage Spring complet</td><td>✘</td><td>✔</td></tr>
 *   <tr><td>Règles métier réelles</td><td>✘ (mockées)</td><td>✔</td></tr>
 *   <tr><td>Persistance réelle</td><td>✘</td><td>✔</td></tr>
 *   <tr><td>Ordre de grandeur</td><td>~0,3 s</td><td>~3 s</td></tr>
 * </table>
 *
 * <p>{@link RestTestClient} est le client de test de Spring Boot 4 ; il remplace
 * {@code TestRestTemplate} et propose la même API fluide que {@code WebTestClient}.
 *
 * <p><b>⚠️ {@code @AutoConfigureRestTestClient} est obligatoire.</b> En Boot 3, un
 * {@code TestRestTemplate} était injectable dès que {@code webEnvironment} valait
 * {@code RANDOM_PORT}. En Boot 4, le bean {@code RestTestClient} n'est créé que si
 * cette annotation est présente ; sans elle, l'erreur est un laconique
 * « No qualifying bean of type RestTestClient available ».
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(ConfigurationPostgres.class)
@DisplayName("Virement — bout en bout HTTP → PostgreSQL")
class VirementBoutEnBoutIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private CompteJpaRepository jpa;

    @BeforeEach
    void preparerLesComptes() {
        jpa.deleteAll();
        comptes.enregistrer(new Compte("FR76-SOURCE", TypeCompte.STANDARD,
                Montant.euros("5000.00"), Montant.euros("0.00")));
        comptes.enregistrer(Compte.standard("FR76-DEST", Montant.euros("0.00")));
    }

    @Test
    @DisplayName("exécute le virement et persiste les deux soldes")
    void devrait_executer_le_virement_et_persister_les_soldes() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.montant").isEqualTo(1000.00)
                // Les frais ne sont PAS stubés ici : c'est GrilleFrais qui les a
                // calculés, et la valeur a traversé toute la chaîne jusqu'au JSON.
                .jsonPath("$.frais").isEqualTo(1.00)
                .jsonPath("$.totalDebite").isEqualTo(1001.00);

        // La preuve décisive : l'état persisté après commit.
        assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                .isEqualTo(Montant.euros("3999.00"));
        assertThat(comptes.parIban("FR76-DEST").orElseThrow().solde())
                .isEqualTo(Montant.euros("1000.00"));
    }

    @Test
    @DisplayName("renvoie 409 et ne modifie aucun solde quand le compte est trop pauvre")
    void devrait_renvoyer_409_et_ne_rien_modifier_quand_le_solde_est_insuffisant() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":999999.00}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);

        // Vérifier le code HTTP ne suffit pas : il faut prouver qu'aucun débit
        // partiel n'a été laissé en base. C'est exactement le genre de garantie
        // qu'un test de slice ne peut pas apporter.
        assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                .isEqualTo(Montant.euros("5000.00"));
        assertThat(comptes.parIban("FR76-DEST").orElseThrow().solde())
                .isEqualTo(Montant.euros("0.00"));
    }

    @Test
    @DisplayName("renvoie 404 quand l'IBAN source n'existe pas en base")
    void devrait_renvoyer_404_quand_l_iban_source_est_absent_de_la_base() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-FANTOME","ibanDestination":"FR76-DEST","montant":10.00}
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("expose le compte réellement stocké")
    void devrait_exposer_le_compte_quand_il_existe_en_base() {
        client.get().uri("/api/comptes/FR76-SOURCE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.iban").isEqualTo("FR76-SOURCE")
                .jsonPath("$.solde").isEqualTo(5000.00)
                .jsonPath("$.devise").isEqualTo("EUR");
    }
}
