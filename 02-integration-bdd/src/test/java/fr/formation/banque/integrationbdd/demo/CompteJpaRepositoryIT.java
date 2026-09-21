package fr.formation.banque.integrationbdd.demo;

import static org.assertj.core.api.Assertions.assertThat;

import fr.formation.banque.integrationbdd.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteEntity;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * DÉMO 2 — la slice {@code @DataJpaTest}.
 *
 * <p><b>Ce que la slice charge</b> : la couche JPA (entités, dépôts Spring Data,
 * {@code DataSource}, Flyway, {@code TestEntityManager}). <b>Ce qu'elle ne charge
 * pas</b> : les {@code @Service}, les {@code @RestController}, la sécurité, le
 * serveur web. Le contexte démarre donc nettement plus vite qu'un
 * {@code @SpringBootTest}, et un test qui échoue ici met en cause la persistance,
 * pas autre chose.
 *
 * <p><b>⚠️ Piège n°1 : {@code @AutoConfigureTestDatabase}</b><br>
 * {@code @DataJpaTest} remplace par défaut la {@code DataSource} par une base
 * embarquée (H2 si elle est au classpath). Sans
 * {@code @AutoConfigureTestDatabase(replace = NONE)}, le conteneur PostgreSQL
 * serait démarré… puis ignoré. On testerait alors un dialecte qui n'est pas
 * celui de la production.
 *
 * <p><b>⚠️ Piège n°2 : en Spring Boot 4, les annotations ont changé de package.</b>
 * <pre>
 * Boot 3 : org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
 * Boot 4 : org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 *
 * Boot 3 : ...test.autoconfigure.jdbc.AutoConfigureTestDatabase
 * Boot 4 : org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
 *
 * Boot 3 : ...test.autoconfigure.orm.jpa.TestEntityManager
 * Boot 4 : org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
 * </pre>
 *
 * <p><b>⚠️ Piège n°3 : {@code @DataJpaTest} est transactionnel</b>, et chaque
 * test est annulé (rollback) à la fin. Voir {@link RollbackTransactionnelIT}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ConfigurationPostgres.class)
@DisplayName("CompteJpaRepository — slice @DataJpaTest")
class CompteJpaRepositoryIT {

    @Autowired
    private CompteJpaRepository depot;

    @Autowired
    private TestEntityManager entityManager;

    private static CompteEntity compte(String iban, String solde) {
        return new CompteEntity(iban, new BigDecimal(solde));
    }

    @Test
    @DisplayName("retrouve un compte par son IBAN")
    void devrait_retrouver_le_compte_quand_l_iban_existe() {
        // persistAndFlush : force l'écriture immédiate en base. Sans le flush,
        // l'INSERT resterait dans le cache de premier niveau d'Hibernate et
        // le test ne prouverait rien de la base (cf. RollbackTransactionnelIT).
        entityManager.persistAndFlush(compte("FR76-A", "100.00"));

        assertThat(depot.findByIban("FR76-A"))
                .isPresent()
                .get()
                .satisfies(entite -> assertThat(entite.solde()).isEqualByComparingTo("100.00"));
    }

    @Test
    @DisplayName("renvoie vide quand l'IBAN n'existe pas")
    void devrait_renvoyer_vide_quand_l_iban_n_existe_pas() {
        assertThat(depot.findByIban("FR76-FANTOME")).isEmpty();
    }

    @Test
    @DisplayName("initialise la version à 0 à l'insertion")
    void devrait_initialiser_la_version_a_zero_quand_le_compte_est_insere() {
        // La colonne @Version est gérée par Hibernate, pas par notre code :
        // ce test documente le comportement sur lequel s'appuie le
        // verrouillage optimiste (exercice 3).
        CompteEntity persiste = entityManager.persistAndFlush(compte("FR76-V", "100.00"));

        assertThat(persiste.version()).isZero();
    }

    @Test
    @DisplayName("trie explicitement les comptes par IBAN")
    void devrait_renvoyer_les_comptes_tries_quand_le_tri_est_dans_la_requete() {
        entityManager.persist(compte("FR76-C", "30.00"));
        entityManager.persist(compte("FR76-A", "10.00"));
        entityManager.persist(compte("FR76-B", "20.00"));
        entityManager.flush();

        List<CompteEntity> comptes = depot.findAllByOrderByIbanAsc();

        // containsExactly n'est légitime QUE parce que la requête impose
        // l'ORDER BY. Sur un findAll() nu, il faudrait
        // containsExactlyInAnyOrder (cf. anti-pattern 9).
        assertThat(comptes).extracting(CompteEntity::iban)
                .containsExactly("FR76-A", "FR76-B", "FR76-C");
    }

    @Test
    @DisplayName("isole chaque test : la base est vide au démarrage du test")
    void devrait_demarrer_sur_une_base_vide_grace_au_rollback() {
        // Les comptes insérés par les autres tests de cette classe ont été
        // annulés : c'est l'avantage du rollback automatique de @DataJpaTest.
        assertThat(depot.count()).isZero();
    }
}
