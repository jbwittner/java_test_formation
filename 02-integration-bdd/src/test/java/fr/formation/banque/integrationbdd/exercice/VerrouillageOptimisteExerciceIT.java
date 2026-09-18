package fr.formation.banque.integrationbdd.exercice;

import fr.formation.banque.domaine.Devise;
import fr.formation.banque.domaine.TypeCompte;
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
 * <p><b>Contexte</b> : deux instances de l'application traitent simultanément un
 * virement sur le même compte. Sans protection, la seconde écriture écrase la
 * première : le débit disparaît. C'est la « mise à jour perdue ».
 * {@link CompteEntity} est protégée par un champ {@code @Version}.
 *
 * <p><b>Consignes</b>
 * <ol>
 *   <li>Retirer le {@code @Disabled}.</li>
 *   <li><b>Test 1 — verrouillage optimiste.</b> Enregistrer un compte, puis :
 *     <ul>
 *       <li>charger DEUX fois l'entité, dans deux transactions distinctes
 *           ({@code transaction.execute(...)}) : ce sont les deux « instances » ;</li>
 *       <li>modifier et enregistrer la première → succès ;</li>
 *       <li>modifier et enregistrer la seconde → doit lever
 *           {@code ObjectOptimisticLockingFailureException} ;</li>
 *       <li>vérifier enfin que le solde en base est bien celui de la PREMIÈRE
 *           écriture — c'est ce qui prouve qu'aucune mise à jour n'a été perdue.</li>
 *     </ul>
 *   </li>
 *   <li><b>Test 2 — unicité de l'IBAN.</b> Enregistrer deux comptes de même IBAN
 *       dans deux transactions distinctes et vérifier que la seconde lève
 *       {@code DataIntegrityViolationException}.</li>
 *   <li><b>Test 3 — contrainte CHECK.</b> Tenter d'enregistrer un compte dont le
 *       découvert autorisé est négatif.</li>
 * </ol>
 *
 * <p><b>⚠️ Piège principal</b> : ne pas annoter cette classe {@code @Transactional}
 * et ne pas se contenter d'appeler {@code depot.save(...)} directement. Il faut
 * DEUX transactions réellement distinctes, sinon Hibernate renvoie la même
 * instance depuis son cache de premier niveau et le conflit ne se produit jamais.
 * {@link TransactionTemplate} sert exactement à cela.
 *
 * <p><b>Questions de fin d'exercice</b>
 * <ol>
 *   <li>Ces trois tests sont-ils écrivables avec un mock de {@code CompteRepository} ?
 *       Avec H2 en mémoire ?</li>
 *   <li>Que se passerait-il si on retirait {@code @Version} de {@link CompteEntity} ?
 *       Quel test échouerait, et quel bug cela laisserait-il passer en production ?</li>
 * </ol>
 *
 * <p>Corrigé :
 * {@code fr.formation.banque.integrationbdd.corrige.VerrouillageOptimisteCorrigeIT}
 */
@Disabled("TODO exercice 3 — retirer cette annotation puis écrire les tests")
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
        return new CompteEntity(iban, TypeCompte.STANDARD,
                new BigDecimal(solde), BigDecimal.ZERO, Devise.EUR);
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

    // TODO : test 3 — decouvert autorise negatif (contrainte CHECK)
}
