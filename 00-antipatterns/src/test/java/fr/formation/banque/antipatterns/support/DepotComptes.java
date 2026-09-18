package fr.formation.banque.antipatterns.support;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Faux dépôt en mémoire (<i>fake</i>, pas un mock) : une vraie implémentation du
 * port, simplement plus simple que la vraie.
 *
 * <p>Un fake est souvent préférable à un mock pour un dépôt : il se comporte
 * comme une collection, il n'a pas besoin d'être stubé cas par cas, et il ne
 * casse pas quand on ajoute un appel.
 */
public class DepotComptes implements CompteRepository {

    private final Map<String, Compte> comptes = new LinkedHashMap<>();

    public DepotComptes(Compte... initiaux) {
        for (Compte compte : initiaux) {
            comptes.put(compte.iban(), compte);
        }
    }

    @Override
    public Optional<Compte> parIban(String iban) {
        return Optional.ofNullable(comptes.get(iban));
    }

    @Override
    public void enregistrer(Compte compte) {
        comptes.put(compte.iban(), compte);
    }
}
