package fr.formation.banque.integrationrest.exercice;

import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.integrationrest.support.SocleCoucheRest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * EXERCICE 4 (partie 2/2) — le même refus, sur un vrai serveur HTTP.
 *
 * <p>Un seul cas ici : deux IBAN identiques. Ce que ce niveau apporte, et que la
 * slice ne peut pas prouver, c'est que le refus survit à une <b>vraie traversée
 * HTTP</b> — serveur embarqué démarré, requête émise sur une socket, réponse
 * désérialisée par un vrai client. Le service reste mocké : les règles métier
 * ont été testées au chapitre 01, la persistance au chapitre 02.
 *
 * <p>Ne pas rejouer les cas de validation de la partie 1.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/04-validation-rest.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.integrationrest.corrige.ValidationVirementCorrigeIT}
 */
@Disabled("Exercice 4 (serveur réel) — voir docs/exercices/04-validation-rest.md, puis retirer cette annotation")
@DisplayName("EXERCICE 4 — refus de virement, couche HTTP sur serveur réel")
class ValidationVirementExerciceIT extends SocleCoucheRest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private ServiceVirement virements;

    @Test
    @DisplayName("amorçage : virement d'un compte vers lui-même")
    void devrait_renvoyer_400_quand_les_deux_ibans_sont_identiques() {
        // TODO : faire lever IllegalArgumentException par le service mocké
        //        (c'est la règle du domaine qui refuse, pas la validation)

        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-SOURCE","montant":10.00}
                        """)
                .exchange();

        // TODO : asserter le statut 400
        // TODO : asserter que le service A BIEN été appelé — contrairement aux
        //        cas de validation de la partie 1, le refus vient d'en dessous
    }
}
