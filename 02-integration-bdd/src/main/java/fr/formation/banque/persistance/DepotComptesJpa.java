package fr.formation.banque.persistance;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptateur : implémente le port {@link CompteRepository} du domaine au-dessus
 * de Spring Data JPA.
 *
 * <p>C'est la seule classe du module qui connaît <b>à la fois</b> le domaine et
 * JPA. C'est donc elle — et la cartographie entité ↔ domaine qu'elle porte —
 * qui a réellement besoin d'un test d'intégration : ni un mock ni un test
 * unitaire ne peuvent prouver que la colonne {@code NUMERIC(19,2)} restitue
 * bien le montant attendu.
 */
@Repository
public class DepotComptesJpa implements CompteRepository {

    private final CompteJpaRepository jpa;

    public DepotComptesJpa(CompteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Compte> parIban(String iban) {
        return jpa.findByIban(iban).map(CompteEntity::versDomaine);
    }

    @Override
    @Transactional
    public void enregistrer(Compte compte) {
        // Mise à jour si la ligne existe, insertion sinon : le domaine ignore
        // cette distinction, qui est un détail de persistance.
        CompteEntity entite = jpa.findByIban(compte.iban())
                .orElseGet(() -> CompteEntity.depuisDomaine(compte));
        entite.mettreAJourDepuis(compte);
        jpa.save(entite);
    }
}
