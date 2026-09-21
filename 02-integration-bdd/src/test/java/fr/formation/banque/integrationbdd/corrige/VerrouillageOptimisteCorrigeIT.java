package fr.formation.banque.integrationbdd.corrige;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.formation.banque.integrationbdd.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteEntity;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CORRIGÉ — exercice 3 : concurrence et contraintes de base.
 *
 * <p>Ces deux tests sont l'argument le plus solide en faveur de Testcontainers.
 * Aucun des deux ne peut être écrit avec un mock, et H2 ne reproduit pas
 * fidèlement les deux comportements.
 *
 * <p><b>Point de méthode important : pas de {@code @Transactional} sur la classe.</b>
 * Simuler deux transactions concurrentes impose de les contrôler soi-même, avec
 * {@link TransactionTemplate}. Si le test était lui-même transactionnel, les
 * deux « transactions » n'en feraient qu'une et le conflit ne se produirait jamais
 * — c'est l'erreur classique sur ce type de test.
 */
@SpringBootTest
@Import(ConfigurationPostgres.class)
@DisplayName("CORRIGÉ 3 — verrouillage optimiste et contrainte d'unicité")
class VerrouillageOptimisteCorrigeIT {

    @Autowired
    private CompteJpaRepository depot;

    @Autowired
    private TransactionTemplate transaction;

    @BeforeEach
    void viderLaBase() {
        depot.deleteAll();
    }

    private static CompteEntity compte(String iban, String solde) {
        return new CompteEntity(iban, new BigDecimal(solde));
    }

    @Test
    @DisplayName("refuse la seconde écriture concurrente sur le même compte")
    void devrait_lever_ObjectOptimisticLockingFailureException_quand_deux_transactions_modifient_le_meme_compte() {
        Long id = transaction.execute(statut -> depot.save(compte("FR76-CONCURRENT", "1000.00")).id());

        // Deux transactions lisent la MÊME version (0), comme deux instances de
        // l'application traitant deux virements simultanés sur le même compte.
        CompteEntity vueA = transaction.execute(statut -> depot.findById(id).orElseThrow());
        CompteEntity vueB = transaction.execute(statut -> depot.findById(id).orElseThrow());
        assertThat(vueA.version()).isEqualTo(vueB.version());

        // La transaction A gagne : l'UPDATE passe, la version devient 1.
        transaction.execute(statut -> {
            vueA.changerSolde(new BigDecimal("900.00"));
            return depot.save(vueA);
        });

        // La transaction B écrit sur la base de la version 0, qui n'existe plus.
        // Hibernate émet UPDATE ... WHERE id = ? AND version = 0, zéro ligne est
        // affectée, et Spring lève l'exception. SANS @Version, cet UPDATE
        // écraserait silencieusement le débit de A : c'est la « mise à jour
        // perdue », un bug invisible en test unitaire et très coûteux en production.
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> transaction.execute(statut -> {
                    vueB.changerSolde(new BigDecimal("800.00"));
                    return depot.save(vueB);
                }));

        // L'écriture perdante n'a rien modifié : le solde est bien celui de A.
        assertThat(depot.findById(id).orElseThrow().solde()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("incrémente la version à chaque mise à jour validée")
    void devrait_incrementer_la_version_quand_la_transaction_est_validee() {
        Long id = transaction.execute(statut -> depot.save(compte("FR76-VERSION", "1000.00")).id());
        assertThat(depot.findById(id).orElseThrow().version()).isZero();

        transaction.execute(statut -> {
            CompteEntity entite = depot.findById(id).orElseThrow();
            entite.changerSolde(new BigDecimal("500.00"));
            return depot.save(entite);
        });

        assertThat(depot.findById(id).orElseThrow().version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("refuse deux comptes portant le même IBAN")
    void devrait_lever_DataIntegrityViolationException_quand_l_iban_est_deja_utilise() {
        transaction.execute(statut -> depot.save(compte("FR76-UNIQUE", "100.00")));

        // Chaque execute() valide sa transaction : la contrainte
        // compte_iban_unique est donc réellement soumise à PostgreSQL.
        // Le même test dans une classe @Transactional passerait au vert sans
        // rien prouver (cf. RollbackTransactionnelIT).
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> transaction.execute(statut -> depot.save(compte("FR76-UNIQUE", "200.00"))));

        assertThat(depot.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("rejette un solde négatif via la contrainte CHECK")
    void devrait_rejeter_quand_le_solde_est_negatif() {
        // La règle existe DEUX fois : dans le domaine (chapitre 01, testée
        // unitairement) et dans la base (contrainte CHECK). Ce test prouve que
        // le garde-fou de dernier recours est bien en place — utile le jour où
        // une insertion arrive par un script ou un autre service.
        CompteEntity invalide = new CompteEntity("FR76-CHECK", new BigDecimal("-1.00"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> transaction.execute(statut -> depot.save(invalide)));
    }
}
