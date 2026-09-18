package fr.formation.banque.evenement;

import org.springframework.data.jpa.repository.JpaRepository;

/** Dépôt des événements consommés, servant de journal d'idempotence. */
public interface VirementRecuRepository extends JpaRepository<VirementRecuEntity, String> {
}
