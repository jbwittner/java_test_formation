package fr.formation.banque.integrationbdd.exercice;

import fr.formation.banque.integrationbdd.support.ConfigurationPostgres;
import fr.formation.banque.persistance.CompteEntity;
import fr.formation.banque.persistance.CompteJpaRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * EXERCICE 3 — prouver ce qu'un mock ne peut pas prouver.
 *
 * <p>Trois garanties assurées par PostgreSQL et non par Java : verrouillage
 * optimiste ({@code @Version}), unicité de l'IBAN, contrainte {@code CHECK}.
 *
 * <p>⚠️ Ne pas annoter cette classe {@code @Transactional} : le rollback
 * automatique masquerait les contraintes. Utiliser {@link TransactionTemplate}
 * pour ouvrir de vraies transactions distinctes.
 *
 * <p>Énoncé complet, checklist, indices et vérification par sabotage :
 * <b>{@code docs/exercices/03-verrouillage-optimiste.md}</b>
 *
 * <p>Corrigé (en dernier recours) :
 * {@code fr.formation.banque.integrationbdd.corrige.VerrouillageOptimisteCorrigeIT}
 */
@Disabled("Exercice 3 — voir docs/exercices/03-verrouillage-optimiste.md, puis retirer cette annotation")
@SpringBootTest
@Import(ConfigurationPostgres.class)
@DisplayName("EXERCICE 3 — verrouillage optimiste et contraintes")
class VerrouillageOptimisteExerciceIT {

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
    @DisplayName("amorçage : deux écritures concurrentes sur le même compte")
    void devrait_refuser_la_seconde_ecriture_concurrente() {
        // Arrange
        Long id = transaction.execute(statut -> depot.save(compte("FR76-CONCURRENT", "1000.00")).id());

        // TODO : charger deux fois l'entité dans deux transactions distinctes
        // TODO : enregistrer la premiere modification (succes attendu)
        // TODO : enregistrer la seconde et asserter ObjectOptimisticLockingFailureException
        // TODO : verifier que le solde final est celui de la PREMIERE ecriture
    }

    // TODO : test 2 — deux comptes avec le meme IBAN

    // TODO : test 3 — solde negatif refuse par la contrainte CHECK
}
