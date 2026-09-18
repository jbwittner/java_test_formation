package fr.formation.banque.integrationbdd.demo;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.integrationbdd.support.ConfigurationPostgres;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * DÉMO 4 — les migrations sont du code de production, donc elles se testent.
 *
 * <p>Ce test répond à une question que ni un test unitaire ni un mock ne peuvent
 * traiter : <b>le schéma livré est-il celui que le code attend ?</b>
 *
 * <p>Il apporte trois garanties :
 * <ol>
 *   <li>les migrations s'appliquent sans erreur sur une base vierge — c'est
 *       exactement ce que fera la production au prochain déploiement ;</li>
 *   <li>le schéma obtenu correspond aux entités JPA : avec
 *       {@code spring.jpa.hibernate.ddl-auto=validate}, un écart fait échouer le
 *       démarrage du contexte, donc ce test, <b>avant</b> la production ;</li>
 *   <li>les contraintes que l'on croit avoir écrites existent réellement — une
 *       contrainte oubliée dans la migration ne se voit sur aucun autre test.</li>
 * </ol>
 *
 * <p>Interroger {@code information_schema} plutôt que de relire le fichier SQL
 * est délibéré : on vérifie ce que la base a <b>compris</b>, pas ce que le
 * fichier <b>dit</b>.
 */
@SpringBootTest
@Import(ConfigurationPostgres.class)
@DisplayName("Migrations Flyway — le schéma livré")
class MigrationFlywayIT {

    private final JdbcClient jdbc;

    MigrationFlywayIT(@Autowired DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    @Test
    @DisplayName("applique toutes les migrations avec succès")
    void devrait_appliquer_toutes_les_migrations_quand_la_base_est_vierge() {
        // Flyway trace ses exécutions : une migration en échec laisse success = false.
        Integer echecs = jdbc.sql("SELECT count(*) FROM flyway_schema_history WHERE success = false")
                .query(Integer.class)
                .single();

        assertThat(echecs).isZero();
    }

    @Test
    @DisplayName("stocke les montants en NUMERIC(19,2)")
    void devrait_declarer_le_solde_en_numeric_19_2() {
        // Si quelqu'un remplaçait NUMERIC par DOUBLE PRECISION « pour simplifier »,
        // les arrondis deviendraient faux en comptabilité. Ce test l'interdit.
        var colonne = jdbc.sql("""
                        SELECT data_type, numeric_precision, numeric_scale
                        FROM information_schema.columns
                        WHERE table_name = 'compte' AND column_name = 'solde'
                        """)
                .query()
                .singleRow();

        assertThat(colonne.get("data_type")).isEqualTo("numeric");
        assertThat(colonne.get("numeric_precision")).isEqualTo(19);
        assertThat(colonne.get("numeric_scale")).isEqualTo(2);
    }

    @Test
    @DisplayName("impose l'unicité de l'IBAN")
    void devrait_declarer_une_contrainte_d_unicite_sur_l_iban() {
        Integer contraintes = jdbc.sql("""
                        SELECT count(*)
                        FROM information_schema.table_constraints
                        WHERE table_name = 'compte'
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name = 'compte_iban_unique'
                        """)
                .query(Integer.class)
                .single();

        assertThat(contraintes).isEqualTo(1);
    }

    @Test
    @DisplayName("crée la séquence d'identifiants avec le même pas que l'entité")
    void devrait_creer_la_sequence_avec_un_increment_de_50() {
        // allocationSize=50 côté JPA doit correspondre à INCREMENT BY 50 en base.
        // Un écart ne se voit qu'en production, sous forme de clés dupliquées.
        Long increment = jdbc.sql("SELECT increment_by FROM pg_sequences WHERE sequencename = 'compte_seq'")
                .query(Long.class)
                .single();

        assertThat(increment).isEqualTo(50L);
    }
}
