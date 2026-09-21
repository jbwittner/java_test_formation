package fr.formation.banque.integrationrest.corrige;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.formation.banque.api.VirementController;
import fr.formation.banque.domaine.ServiceVirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * CORRIGÉ — exercice 4, niveau slice.
 *
 * <p>Deux familles d'erreurs bien distinctes, souvent confondues :
 * <ul>
 *   <li>la <b>validation d'entrée</b> ({@code @DecimalMin}, {@code @Digits},
 *       {@code @NotBlank}) : elle s'exécute avant le contrôleur, le service
 *       n'est jamais appelé. C'est ce que prouve {@code verifyNoInteractions} ;</li>
 *   <li>la <b>règle métier</b> (deux IBAN identiques) : la requête est
 *       syntaxiquement valide, c'est le domaine qui la refuse. Le service EST
 *       appelé et lève {@code IllegalArgumentException}, que le
 *       {@code @RestControllerAdvice} traduit en 400.</li>
 * </ul>
 *
 * <p>Les deux produisent un 400, mais par des chemins différents : tester l'un
 * ne teste pas l'autre.
 */
@WebMvcTest(VirementController.class)
@DisplayName("CORRIGÉ 4 — validation et refus métier (slice)")
class ValidationVirementCorrigeTest {

    @Autowired
    private MockMvcTester client;

    @MockitoBean
    private ServiceVirement virements;

    private static String demande(String source, String destination, String montant) {
        return """
                {"ibanSource":%s,"ibanDestination":%s,"montant":%s}
                """.formatted(source, destination, montant);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', value = {
            "0.001    | trois décimales : viole @Digits(fraction = 2)",
            "0        | montant nul : viole @DecimalMin(0.01)",
            "-10.00   | montant négatif : viole @DecimalMin(0.01)"
    })
    @DisplayName("renvoie 400 et nomme le champ montant sans appeler le service")
    void devrait_renvoyer_400_quand_le_montant_est_invalide(String montant, String cas) {
        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(demande("\"FR76-SOURCE\"", "\"FR76-DEST\"", montant)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.champs.montant", v -> v.assertThat().isNotNull());

        // La validation coupe avant le contrôleur : aucune règle métier n'a tourné.
        verifyNoInteractions(virements);
    }

    @Test
    @DisplayName("renvoie 400 quand le montant est absent")
    void devrait_renvoyer_400_quand_le_montant_est_absent() {
        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.champs.montant", v -> v.assertThat().isNotNull());

        verifyNoInteractions(virements);
    }

    @Test
    @DisplayName("renvoie 400 quand la source et la destination sont identiques")
    void devrait_renvoyer_400_quand_les_deux_ibans_sont_identiques() {
        // Ici la requête est syntaxiquement valide : c'est le DOMAINE qui refuse.
        when(virements.executer(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Un virement doit relier deux comptes distincts"));

        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(demande("\"FR76-SOURCE\"", "\"FR76-SOURCE\"", "10.00")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.title", v -> v.assertThat().isEqualTo("Requête invalide"));
    }
}
