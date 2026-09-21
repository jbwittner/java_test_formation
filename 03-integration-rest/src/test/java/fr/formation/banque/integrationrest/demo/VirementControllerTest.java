package fr.formation.banque.integrationrest.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.formation.banque.api.VirementController;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteIntrouvableException;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.Virement;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * DÉMO 5 — la slice web {@code @WebMvcTest}.
 *
 * <p><b>Ce qui est chargé</b> : le {@link VirementController}, la sérialisation
 * JSON, la validation, le {@code @RestControllerAdvice}, le routage Spring MVC.
 * <br><b>Ce qui ne l'est pas</b> : les services, les dépôts, la
 * {@code DataSource}, le serveur web. Aucune requête réseau réelle n'est émise —
 * Spring appelle directement la chaîne de traitement MVC.
 *
 * <p><b>Conséquence sur la vitesse</b> : ce contexte démarre en une poignée de
 * centaines de millisecondes, contre plusieurs secondes pour un
 * {@code @SpringBootTest} avec base de données. C'est ce qui permet de couvrir
 * finement <b>tout</b> le contrat HTTP — chaque code d'erreur, chaque champ
 * invalide — sans alourdir la suite.
 *
 * <p><b>Le suffixe est {@code Test}, pas {@code IT}</b> : aucune dépendance
 * externe, donc cette classe tourne dans {@code mvn test}, sans Docker.
 *
 * <p><b>⚠️ Spring Boot 4</b>
 * <pre>
 * {@code @MockBean} n'existe plus  -> {@code @MockitoBean}
 *     (org.springframework.test.context.bean.override.mockito.MockitoBean)
 * {@code @WebMvcTest} a change de package
 *     Boot 3 : org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
 *     Boot 4 : org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
 * </pre>
 *
 * <p>{@link MockMvcTester} est l'API AssertJ de MockMvc : elle remplace les
 * {@code andExpect(status().isOk())} par des assertions {@code assertThat}
 * chaînées, avec les mêmes messages d'échec lisibles que partout ailleurs.
 */
@WebMvcTest(VirementController.class)
@DisplayName("VirementController — contrat HTTP (slice @WebMvcTest)")
class VirementControllerTest {

    @Autowired
    private MockMvcTester client;

    // Les collaborateurs du contrôleur sont remplacés par des mocks : on teste
    // la traduction HTTP, pas la règle métier (déjà couverte au chapitre 01).
    @MockitoBean
    private ServiceVirement virements;

    @MockitoBean
    private CompteRepository comptes;

    private static final String DEMANDE_VALIDE = """
            {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
            """;

    @Test
    @DisplayName("renvoie 201, l'en-tête Location et le corps du virement")
    void devrait_renvoyer_201_quand_le_virement_est_accepte() {
        when(virements.executer(eq("FR76-SOURCE"), eq("FR76-DEST"), any()))
                .thenReturn(new Virement("VIR-1", "FR76-SOURCE", "FR76-DEST",
                        Montant.euros("1000.00"), Montant.euros("1.00"),
                        LocalDate.of(2025, 6, 3)));

        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEMANDE_VALIDE))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/virements/VIR-1")
                .bodyJson()
                .hasPathSatisfying("$.reference", valeur -> valeur.assertThat().isEqualTo("VIR-1"))
                .hasPathSatisfying("$.montant", valeur -> valeur.assertThat().isEqualTo(1000.00))
                .hasPathSatisfying("$.frais", valeur -> valeur.assertThat().isEqualTo(1.00))
                .hasPathSatisfying("$.totalDebite", valeur -> valeur.assertThat().isEqualTo(1001.00))
                .hasPathSatisfying("$.dateDeValeur", valeur -> valeur.assertThat().isEqualTo("2025-06-03"));
    }

    @Test
    @DisplayName("renvoie 409 quand le solde est insuffisant")
    void devrait_renvoyer_409_quand_le_solde_est_insuffisant() {
        when(virements.executer(any(), any(), any()))
                .thenThrow(new SoldeInsuffisantException("FR76-SOURCE",
                        Montant.euros("1000.00"), Montant.euros("10.00")));

        // 409 et non 400 : la requête est bien formée, c'est l'état du compte
        // qui empêche l'opération. Ce choix fait partie du contrat public et
        // mérite donc un test qui le verrouille.
        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEMANDE_VALIDE))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", valeur -> valeur.assertThat().isEqualTo("Solde insuffisant"))
                .hasPathSatisfying("$.iban", valeur -> valeur.assertThat().isEqualTo("FR76-SOURCE"));
    }

    @Test
    @DisplayName("renvoie 404 quand le compte est inconnu")
    void devrait_renvoyer_404_quand_le_compte_est_inconnu() {
        when(virements.executer(any(), any(), any()))
                .thenThrow(new CompteIntrouvableException("FR76-FANTOME"));

        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEMANDE_VALIDE))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("renvoie 400 et le détail des champs quand la demande est invalide")
    void devrait_renvoyer_400_quand_le_montant_est_negatif() {
        String demandeInvalide = """
                {"ibanSource":"","ibanDestination":"FR76-DEST","montant":-5}
                """;

        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(demandeInvalide))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.champs.ibanSource", v -> v.assertThat().isNotNull())
                .hasPathSatisfying("$.champs.montant", v -> v.assertThat().isNotNull());

        // Point essentiel : la validation coupe AVANT le service. Un test qui
        // ne vérifierait que le code 400 laisserait passer une implémentation
        // qui exécute quand même le virement puis renvoie 400.
        verifyNoInteractions(virements);
    }

    @Test
    @DisplayName("renvoie 400 quand le corps JSON est illisible")
    void devrait_renvoyer_400_quand_le_corps_est_malforme() {
        assertThat(client.post().uri("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ceci n'est pas du JSON"))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(virements, never()).executer(any(), any(), any());
    }

    @Test
    @DisplayName("expose le solde d'un compte")
    void devrait_renvoyer_le_compte_quand_l_iban_existe() {
        when(comptes.parIban("FR76-A")).thenReturn(Optional.of(
                new Compte("FR76-A", Montant.euros("100.00"))));

        assertThat(client.get().uri("/api/comptes/FR76-A"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .hasPathSatisfying("$.iban", v -> v.assertThat().isEqualTo("FR76-A"))
                .hasPathSatisfying("$.solde", v -> v.assertThat().isEqualTo(100.00));
    }

    @Test
    @DisplayName("renvoie 404 sur la consultation d'un IBAN inconnu")
    void devrait_renvoyer_404_quand_la_consultation_porte_sur_un_iban_inconnu() {
        when(comptes.parIban("FR76-FANTOME")).thenReturn(Optional.empty());

        assertThat(client.get().uri("/api/comptes/FR76-FANTOME"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
