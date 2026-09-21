package fr.formation.banque.integrationrest.exercice;

import fr.formation.banque.api.VirementController;
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
 * <p>Quatre cas répondent 400, par deux chemins différents : la validation
 * ({@code @Digits}, {@code @DecimalMin}, {@code @NotNull}) et le domaine (deux
 * IBAN identiques). Le service n'est appelé que dans le second cas.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/04-validation-rest.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.integrationrest.corrige.ValidationVirementCorrigeTest}
 */
@Disabled("Exercice 4 (slice) — voir docs/exercices/04-validation-rest.md, puis retirer cette annotation")
@WebMvcTest(VirementController.class)
@DisplayName("EXERCICE 4 — validation (slice)")
class ValidationVirementExerciceTest {

    @Autowired
    private MockMvcTester client;

    @MockitoBean
    private ServiceVirement virements;

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
