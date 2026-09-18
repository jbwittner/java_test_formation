package fr.formation.banque.integrationbdd.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import fr.formation.banque.domaine.Devise;
import fr.formation.banque.domaine.TypeCompte;
import fr.formation.banque.integrationbdd.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteEntity;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DÉMO 3 — ce que le rollback automatique donne… et ce qu'il cache.
 *
 * <p>{@code @DataJpaTest} (comme tout test annoté {@code @Transactional}) ouvre
 * une transaction avant chaque test et l'annule après. C'est très pratique :
 * isolation gratuite, aucun nettoyage à écrire.
 *
 * <p><b>Mais cette transaction change le comportement observé.</b> Hibernate
 * retarde ses {@code INSERT}/{@code UPDATE} jusqu'au {@code flush}, et le flush
 * n'a lieu qu'au commit… qui n'arrive jamais en test. Conséquence : toutes les
 * erreurs détectées <b>par la base</b> — contraintes d'unicité, {@code CHECK},
 * colonnes trop courtes, clés étrangères — peuvent ne jamais se déclencher.
 *
 * <p><b>Le test vert qui ment</b> est illustré ci-dessous : insérer deux fois le
 * même IBAN ne lève rien tant qu'on ne force pas le flush.
 *
 * <p><b>Les trois parades</b>, par ordre de préférence :
 * <ol>
 *   <li>{@code flush()} explicite quand on veut prouver qu'une contrainte de
 *       base se déclenche — précis, rapide, reste dans la transaction ;</li>
 *   <li>{@code @Commit} ou {@code @Transactional(propagation = NOT_SUPPORTED)}
 *       pour reproduire le comportement réel, au prix d'un nettoyage manuel ;</li>
 *   <li>un {@code @SpringBootTest} non transactionnel (voir
 *       {@link DepotComptesJpaIT}) : chaque appel valide sa propre transaction,
 *       exactement comme en production.</li>
 * </ol>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ConfigurationPostgres.class)
@DisplayName("Rollback automatique — avantage et angle mort")
class RollbackTransactionnelIT {

    @Autowired
    private CompteJpaRepository depot;

    private static CompteEntity compte(String iban, String solde) {
        return new CompteEntity(iban, TypeCompte.STANDARD,
                new BigDecimal(solde), BigDecimal.ZERO, Devise.EUR);
    }

    @Test
    @DisplayName("SANS flush : le doublon d'IBAN passe inaperçu — test vert, bug en production")
    void devrait_ne_rien_detecter_quand_le_doublon_n_est_pas_flushe() {
        // Aucune exception : les deux INSERT sont encore en attente dans le
        // contexte de persistance. La contrainte compte_iban_unique n'a jamais
        // été soumise à PostgreSQL. Ce test serait vert même si la contrainte
        // n'existait pas du tout dans la migration.
        assertThatNoException().isThrownBy(() -> {
            depot.save(compte("FR76-DOUBLON", "100.00"));
            depot.save(compte("FR76-DOUBLON", "200.00"));
        });

        // Attention : ne surtout PAS interroger le dépôt ici. Une requête JPQL
        // déclenche un flush automatique (FlushMode.AUTO) et ferait apparaître
        // l'erreur — ce qui montre au passage à quel point le moment du flush
        // est difficile à prévoir quand on ne le contrôle pas explicitement.
    }

    @Test
    @DisplayName("AVEC flush : la contrainte d'unicité se déclenche réellement")
    void devrait_lever_DataIntegrityViolationException_quand_le_doublon_est_flushe() {
        depot.save(compte("FR76-DOUBLON", "100.00"));
        depot.save(compte("FR76-DOUBLON", "200.00"));

        // flush() envoie les INSERT à PostgreSQL immédiatement : c'est là que
        // la contrainte compte_iban_unique s'exprime. C'est ce flush explicite
        // qui transforme le test précédent en test utile.
        //
        // À noter : on passe par le dépôt Spring Data, pas par l'EntityManager brut.
        // Spring traduit alors l'exception Hibernate (ConstraintViolationException,
        // spécifique au fournisseur JPA) en DataIntegrityViolationException, qui
        // appartient à la hiérarchie portable de Spring. Asserter sur le type
        // portable évite de figer le test sur Hibernate.
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> depot.flush());
    }

    @Test
    @DisplayName("le rollback annule tout : la base est vide au test suivant")
    void devrait_avoir_une_base_vide_quand_le_test_precedent_a_ete_annule() {
        // Malgré les insertions des deux tests précédents, la table est vide :
        // c'est l'isolation offerte par le rollback. Utile — à condition de
        // savoir ce qu'elle masque.
        assertThat(depot.count()).isZero();
    }
}
