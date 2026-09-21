package fr.formation.banque.integrationrest.demo;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.api.DemandeVirement;
import fr.formation.banque.api.ReponseVirement;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.Virement;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

/**
 * DÉMO 7 — {@code @JsonTest} : la plus fine des slices.
 *
 * <p>Elle ne charge que la configuration de sérialisation. Utile quand le format
 * JSON est lui-même un contrat : noms de champs, format des dates, nombre de
 * décimales. Ces détails cassent facilement — un {@code LocalDate} sérialisé en
 * tableau {@code [2025,6,3]} au lieu de {@code "2025-06-03"} est un grand
 * classique — et ils méritent un test rapide et ciblé plutôt qu'un test HTTP complet.
 *
 * <p>Temps de démarrage : négligeable. Ce type de test appartient à
 * {@code mvn test}, pas à {@code mvn verify}.
 */
@JsonTest
@DisplayName("Sérialisation JSON du contrat d'API")
class ReponseVirementJsonTest {

    @Autowired
    private JacksonTester<ReponseVirement> reponseJson;

    @Autowired
    private JacksonTester<DemandeVirement> demandeJson;

    @Test
    @DisplayName("sérialise la date de valeur au format ISO, pas en tableau")
    void devrait_serialiser_la_date_au_format_iso() throws Exception {
        ReponseVirement reponse = ReponseVirement.depuis(new Virement(
                "VIR-1", "FR76-SOURCE", "FR76-DEST",
                Montant.euros("1000.00"), Montant.euros("1.00"),
                LocalDate.of(2025, 6, 3)));

        assertThat(reponseJson.write(reponse))
                .extractingJsonPathStringValue("$.dateDeValeur").isEqualTo("2025-06-03");
    }

    @Test
    @DisplayName("sérialise les montants avec deux décimales")
    void devrait_serialiser_les_montants_avec_deux_decimales() throws Exception {
        ReponseVirement reponse = ReponseVirement.depuis(new Virement(
                "VIR-1", "FR76-SOURCE", "FR76-DEST",
                Montant.euros("1000.50"), Montant.euros("1.00"),
                LocalDate.of(2025, 6, 3)));

        assertThat(reponseJson.write(reponse))
                .extractingJsonPathNumberValue("$.montant").isEqualTo(1000.50);
        assertThat(reponseJson.write(reponse))
                .extractingJsonPathNumberValue("$.totalDebite").isEqualTo(1001.50);
    }

    @Test
    @DisplayName("désérialise une demande de virement")
    void devrait_deserialiser_la_demande_quand_le_json_est_complet() throws Exception {
        String json = """
                {"ibanSource":"FR76-SOURCE","ibanDestination":"FR76-DEST","montant":1000.00}
                """;

        DemandeVirement demande = demandeJson.parseObject(json);

        assertThat(demande.ibanSource()).isEqualTo("FR76-SOURCE");
        // isEqualByComparingTo et non isEqualTo : 1000.00 et 1000.0 sont le même
        // nombre mais deux BigDecimal différents (piège classique).
        assertThat(demande.montant()).isEqualByComparingTo("1000.00");
    }
}
