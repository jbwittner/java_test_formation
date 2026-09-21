package fr.formation.banque.integrationrest.corrige;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.integrationrest.support.SocleCoucheRest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * CORRIGÉ — exercice 4, sur serveur HTTP réel.
 *
 * <p>Deux tests seulement, là où la slice en compte cinq : c'est volontaire. Ce
 * niveau coûte le démarrage du contexte complet et d'un serveur embarqué ; on y
 * met ce que la slice ne peut pas prouver — ici, que les <b>deux chemins de
 * refus</b> aboutissent au même 400 après une vraie traversée HTTP :
 * <ul>
 *   <li>la validation, qui coupe <b>avant</b> le service ;</li>
 *   <li>la règle du domaine, qui remonte <b>depuis</b> le service et que le
 *       {@code @RestControllerAdvice} traduit.</li>
 * </ul>
 *
 * <p>Le service reste mocké : rejouer ici les règles métier (chapitre 01) ou la
 * persistance (chapitre 02) coûterait des secondes sans rien apporter — c'est
 * exactement ainsi qu'une suite de tests devient lente.
 */
@DisplayName("CORRIGÉ 4 — refus de virement, couche HTTP sur serveur réel")
class ValidationVirementCorrigeIT extends SocleCoucheRest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private ServiceVirement virements;

    @Test
    @DisplayName("refuse un virement d'un compte vers lui-même — le refus vient du domaine")
    void devrait_renvoyer_400_quand_les_deux_ibans_sont_identiques() {
        when(virements.executer(any(), any(), any()))
                .thenThrow(new IllegalArgumentException(
                        "Un virement doit relier deux comptes distincts"));

        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-SOURCE","montant":10.00}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // Pas de propriété « champs » : ce 400 ne vient pas de la
                // validation. C'est ce qui distingue les deux chemins.
                .jsonPath("$.champs").doesNotExist()
                .jsonPath("$.title").isEqualTo("Requête invalide");

        // Le service EST appelé ici : la requête était bien formée.
        verify(virements).executer("FR76-SOURCE", "FR76-SOURCE", Montant.euros("10.00"));
    }

    @Test
    @DisplayName("rejette un montant à trois décimales — le refus vient de la validation")
    void devrait_renvoyer_400_quand_le_montant_a_trois_decimales() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":10.001}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.champs.montant").exists();

        // L'assertion décisive : la validation a coupé avant le service.
        verifyNoInteractions(virements);
    }
}
