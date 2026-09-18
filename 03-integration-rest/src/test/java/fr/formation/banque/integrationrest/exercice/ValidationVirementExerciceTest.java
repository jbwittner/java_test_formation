package fr.formation.banque.integrationrest.exercice;

import fr.formation.banque.api.VirementController;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.ServiceVirement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * EXERCICE 4 (partie 1/2) — tester les refus au niveau de la slice web.
 *
 * <p><b>Contrat à vérifier</b> — {@code POST /api/virements} doit répondre 400 :
 * <ul>
 *   <li>montant à trois décimales ({@code @Digits(fraction = 2)}) ;</li>
 *   <li>montant nul ou négatif ({@code @DecimalMin("0.01")}) ;</li>
 *   <li>montant absent ({@code @NotNull}) ;</li>
 *   <li>IBAN source identique à l'IBAN de destination — refus du <b>domaine</b>,
 *       pas de la validation.</li>
 * </ul>
 *
 * <p><b>Consignes</b>
 * <ol>
 *   <li>Retirer le {@code @Disabled}.</li>
 *   <li>Écrire les tests de validation. Vérifier le code 400 <b>et</b> la
 *       présence du champ fautif dans {@code $.champs}.</li>
 *   <li>Ajouter {@code verifyNoInteractions(virements)} sur les cas de
 *       validation. <b>Pourquoi est-ce important ?</b> Réfléchir à ce que
 *       laisserait passer un test qui vérifierait seulement le code 400.</li>
 *   <li>Pour le cas « deux IBAN identiques », faire lever
 *       {@code IllegalArgumentException} par le service mocké, puis vérifier
 *       que le {@code @RestControllerAdvice} renvoie bien un 400.</li>
 *   <li>Regrouper les trois cas de montant invalide dans un
 *       {@code @ParameterizedTest}.</li>
 * </ol>
 *
 * <p><b>Question de fin d'exercice</b> : les deux familles d'erreurs produisent
 * un 400. Par quel chemin passent-elles ? Le service est-il appelé dans les
 * deux cas ? Un seul test pourrait-il couvrir les deux ?
 *
 * <p>Corrigé :
 * {@code fr.formation.banque.integrationrest.corrige.ValidationVirementCorrigeTest}
 */
@Disabled("TODO exercice 4 (slice) — retirer cette annotation puis écrire les tests")
@WebMvcTest(VirementController.class)
@DisplayName("EXERCICE 4 — validation (slice)")
class ValidationVirementExerciceTest {

    @Autowired
    private MockMvcTester client;

    @MockitoBean
    private ServiceVirement virements;

    @MockitoBean
    private CompteRepository comptes;

    @Test
    @DisplayName("amorçage : montant à trois décimales")
    void devrait_renvoyer_400_quand_le_montant_a_trois_decimales() {
        // Act
        client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":0.001}
                        """)
                .exchange();

        // TODO : asserter le statut 400
        // TODO : asserter la presence de $.champs.montant
        // TODO : asserter qu'aucune interaction n'a eu lieu avec le service
    }

    // TODO : montant nul, montant negatif, montant absent

    // TODO : IBAN source == IBAN destination (refus du domaine, pas de la validation)
}
