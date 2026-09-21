package fr.formation.banque.integrationrest.exercice;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.integrationrest.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * EXERCICE 4 (partie 2/2) — le même refus, bout en bout.
 *
 * <p>Un seul cas ici : deux IBAN identiques. Ce que ce niveau apporte, et que la
 * slice ne peut pas prouver, c'est qu'<b>aucun solde n'a bougé en base</b>.
 * Ne pas rejouer les cas de validation de la partie 1.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/04-validation-rest.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.integrationrest.corrige.ValidationVirementCorrigeIT}
 */
@Disabled("Exercice 4 (bout en bout) — voir docs/exercices/04-validation-rest.md, puis retirer cette annotation")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(ConfigurationPostgres.class)
@DisplayName("EXERCICE 4 — refus de virement, bout en bout")
class ValidationVirementExerciceIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private CompteRepository comptes;

    @Autowired
    private CompteJpaRepository jpa;

    @BeforeEach
    void preparerLesComptes() {
        jpa.deleteAll();
        comptes.enregistrer(new Compte("FR76-SOURCE", Montant.euros("5000.00")));
        comptes.enregistrer(new Compte("FR76-DEST", Montant.euros("0.00")));
    }

    @Test
    @DisplayName("amorçage : virement d'un compte vers lui-même")
    void devrait_renvoyer_400_et_ne_rien_modifier_quand_les_deux_ibans_sont_identiques() {
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-SOURCE","montant":10.00}
                        """)
                .exchange();

        // TODO : asserter le statut 400
        // TODO : asserter que le solde de FR76-SOURCE vaut toujours 5000.00
    }
}
