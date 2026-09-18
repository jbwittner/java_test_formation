package fr.formation.banque.integrationbdd.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Devise;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.TypeCompte;
import fr.formation.banque.integrationbdd.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * DÉMO 1 — le test d'intégration qui prouve ce qu'un test unitaire ne peut pas prouver.
 *
 * <p><b>Ce que ce test vérifie et qu'aucun mock ne vérifiera jamais :</b>
 * <ul>
 *   <li>la cartographie entité ↔ domaine (noms de colonnes, enums en {@code VARCHAR}) ;</li>
 *   <li>la <b>précision décimale</b> : {@code NUMERIC(19,2)} arrondit à deux
 *       décimales côté base ; un mock aurait rendu la valeur intacte ;</li>
 *   <li>le fait que les migrations Flyway produisent un schéma compatible avec
 *       les entités ({@code ddl-auto=validate} fait échouer le démarrage sinon) ;</li>
 *   <li>que la mise à jour d'un compte existant fait bien un {@code UPDATE}
 *       et non un second {@code INSERT}.</li>
 * </ul>
 *
 * <p><b>Le suffixe {@code IT}</b> (et non {@code Test}) est ce qui range cette
 * classe dans failsafe : elle ne s'exécute qu'au {@code mvn verify}, avec Docker.
 * {@code mvn test} reste rapide et utilisable sans Docker.
 *
 * <p><b>Pas de {@code @Transactional} ici</b> : chaque appel du dépôt ouvre et
 * valide sa propre transaction, comme en production. Voir
 * {@link RollbackTransactionnelIT} pour ce que le rollback automatique masque.
 */
@SpringBootTest
@Import(ConfigurationPostgres.class)
@DisplayName("DepotComptesJpa — adaptateur JPA du port du domaine")
class DepotComptesJpaIT {

    @Autowired
    private CompteRepository depot;

    @Autowired
    private CompteJpaRepository jpa;

    @BeforeEach
    void viderLaBase() {
        // Isolation entre tests : sans cela, le premier test qui insère
        // « FR76-DEMO » ferait échouer tous les suivants sur la contrainte
        // d'unicité (cf. anti-pattern 4, version base de données).
        jpa.deleteAll();
    }

    @Test
    @DisplayName("relit un compte identique à celui qui a été enregistré")
    void devrait_relire_un_compte_identique_quand_il_a_ete_enregistre() {
        Compte compte = new Compte("FR76-DEMO", TypeCompte.PREMIUM,
                Montant.euros("1234.56"), Montant.euros("500.00"));

        depot.enregistrer(compte);
        Optional<Compte> relu = depot.parIban("FR76-DEMO");

        assertThat(relu).isPresent();
        assertSoftly(verif -> {
            Compte c = relu.orElseThrow();
            verif.assertThat(c.iban()).isEqualTo("FR76-DEMO");
            verif.assertThat(c.type()).isEqualTo(TypeCompte.PREMIUM);
            verif.assertThat(c.solde()).isEqualTo(Montant.euros("1234.56"));
            verif.assertThat(c.decouvertAutorise()).isEqualTo(Montant.euros("500.00"));
            verif.assertThat(c.solde().devise()).isEqualTo(Devise.EUR);
        });
    }

    @Test
    @DisplayName("restitue exactement les grands montants et l'échelle à 2 décimales")
    void devrait_restituer_la_valeur_exacte_quand_le_montant_est_tres_grand() {
        // Ce que seule une vraie base peut prouver : la colonne est bien
        // NUMERIC(19,2) et non un DOUBLE PRECISION. Avec un type flottant,
        // ce montant reviendrait arrondi — une erreur invisible en test unitaire
        // et catastrophique en comptabilité.
        Compte compte = Compte.standard("FR76-GROS",
                new Montant(new BigDecimal("99999999999999999.99"), Devise.EUR));

        depot.enregistrer(compte);

        Montant relu = depot.parIban("FR76-GROS").orElseThrow().solde();
        assertThat(relu.valeur()).isEqualByComparingTo("99999999999999999.99");
        // L'échelle est conservée : 10.10 ne doit pas revenir en 10.1.
        assertThat(relu.valeur().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("met à jour la ligne existante au lieu d'en insérer une seconde")
    void devrait_mettre_a_jour_la_ligne_quand_l_iban_existe_deja() {
        Compte compte = Compte.standard("FR76-MAJ", Montant.euros("100.00"));
        depot.enregistrer(compte);

        compte.crediter(Montant.euros("50.00"));
        depot.enregistrer(compte);

        // Le bug classique que ce test attrape : un save() qui insère une
        // deuxième ligne parce que l'entité rechargée n'a pas été réutilisée.
        assertThat(jpa.count()).isEqualTo(1);
        assertThat(depot.parIban("FR76-MAJ").orElseThrow().solde())
                .isEqualTo(Montant.euros("150.00"));
    }

    @Test
    @DisplayName("renvoie un Optional vide quand l'IBAN est inconnu")
    void devrait_renvoyer_vide_quand_l_iban_est_inconnu() {
        assertThat(depot.parIban("FR76-FANTOME")).isEmpty();
    }

    @Test
    @DisplayName("conserve un solde négatif issu d'un découvert")
    void devrait_conserver_un_solde_negatif_quand_le_compte_est_a_decouvert() {
        // Piège de schéma : une colonne déclarée UNSIGNED ou une contrainte
        // CHECK (solde >= 0) trop zélée casserait ici. Le test documente
        // qu'un solde négatif est légitime.
        Compte compte = new Compte("FR76-DECOUVERT", TypeCompte.STANDARD,
                Montant.euros("100.00"), Montant.euros("500.00"));
        compte.debiter(Montant.euros("300.00"));

        depot.enregistrer(compte);

        assertThat(depot.parIban("FR76-DECOUVERT").orElseThrow().solde())
                .isEqualTo(Montant.euros("-200.00"));
    }
}
