package fr.formation.banque.integrationrest.corrige;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
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
 * CORRIGÉ — exercice 4, niveau bout en bout.
 *
 * <p>Deux tests seulement, là où la slice en compte cinq : c'est volontaire.
 * Le bout en bout coûte un contexte complet et un conteneur PostgreSQL ; on y
 * met ce que la slice ne peut pas prouver — ici, <b>qu'aucun mouvement n'est
 * resté en base après un refus</b>.
 *
 * <p>Vérifier une nouvelle fois chaque message de validation à ce niveau
 * coûterait des secondes sans rien apporter : c'est exactement ainsi qu'une
 * suite de tests devient lente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(ConfigurationPostgres.class)
@DisplayName("CORRIGÉ 4 — refus de virement, bout en bout")
class ValidationVirementCorrigeIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private CompteJpaRepository jpa;

    @BeforeEach
    void preparerLesComptes() {
        jpa.deleteAll();
        comptes.enregistrer(Compte.standard("FR76-SOURCE", Montant.euros("5000.00")));
        comptes.enregistrer(Compte.standard("FR76-DEST", Montant.euros("0.00")));
    }

    @Test
    @DisplayName("refuse un virement d'un compte vers lui-même sans rien modifier")
    void devrait_renvoyer_400_et_ne_rien_modifier_quand_les_deux_ibans_sont_identiques() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-SOURCE","montant":10.00}
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                .isEqualTo(Montant.euros("5000.00"));
    }

    @Test
    @DisplayName("rejette un montant à trois décimales sans rien modifier")
    void devrait_renvoyer_400_et_ne_rien_modifier_quand_le_montant_a_trois_decimales() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":10.001}
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                .isEqualTo(Montant.euros("5000.00"));
        assertThat(comptes.parIban("FR76-DEST").orElseThrow().solde())
                .isEqualTo(Montant.euros("0.00"));
    }
}
