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
 * <p><b>Consignes</b>
 * <ol>
 *   <li>Retirer le {@code @Disabled}.</li>
 *   <li>Envoyer un virement d'un compte vers lui-même et vérifier le 400.</li>
 *   <li><b>Surtout</b> : vérifier ensuite en base qu'aucun solde n'a bougé.
 *       C'est la seule chose que ce test apporte par rapport à la slice.</li>
 *   <li>N'écrire ici que ce qui a besoin de la base. Ne PAS rejouer les cinq
 *       cas de validation de la partie 1.</li>
 * </ol>
 *
 * <p><b>Questions de fin d'exercice</b>
 * <ol>
 *   <li>Comparer les durées affichées par Maven pour la classe {@code ...Test}
 *       (slice) et la classe {@code ...IT} (bout en bout). Quel rapport ?</li>
 *   <li>Si l'équipe n'avait le budget que pour une seule des deux classes,
 *       laquelle garder — et qu'accepterait-elle de perdre ?</li>
 * </ol>
 *
 * <p>Corrigé :
 * {@code fr.formation.banque.integrationrest.corrige.ValidationVirementCorrigeIT}
 */
@Disabled("TODO exercice 4 (bout en bout) — retirer cette annotation puis écrire les tests")
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
        comptes.enregistrer(Compte.standard("FR76-SOURCE", Montant.euros("5000.00")));
        comptes.enregistrer(Compte.standard("FR76-DEST", Montant.euros("0.00")));
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
