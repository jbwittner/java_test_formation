package fr.formation.banque.persistance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Dépôt Spring Data sur l'entité de stockage.
 *
 * <p>Noter {@code findAllByOrderByIbanAsc} : un {@code findAll()} ne garantit
 * AUCUN ordre, même si PostgreSQL semble toujours renvoyer les lignes dans le
 * même sens (cf. anti-pattern 9). Si un test assère un ordre, la requête doit
 * l'imposer explicitement.
 */
public interface CompteJpaRepository extends JpaRepository<CompteEntity, Long> {

    Optional<CompteEntity> findByIban(String iban);

    boolean existsByIban(String iban);

    List<CompteEntity> findAllByOrderByIbanAsc();
}
