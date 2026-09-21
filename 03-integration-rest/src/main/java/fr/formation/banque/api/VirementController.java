package fr.formation.banque.api;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.Virement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST : traduction HTTP ↔ domaine, et rien d'autre.
 *
 * <p>Aucune règle métier ici — pas de calcul de frais, pas de contrôle de solde.
 * C'est ce qui permet de le tester avec une slice {@code @WebMvcTest} : en
 * mockant {@link ServiceVirement}, on vérifie le <b>contrat HTTP</b> (route,
 * code de statut, forme du JSON, en-tête {@code Location}) sans base de données
 * ni serveur.
 *
 * <p><b>Un seul collaborateur</b> : {@link ServiceVirement}. Le contrôleur ne
 * connaît pas le dépôt de comptes, même pour une simple consultation — sinon la
 * couche web aurait deux portes d'entrée dans le domaine, donc deux mocks à
 * poser dans chaque test, et la frontière testée deviendrait floue.
 */
@RestController
@RequestMapping("/api")
public class VirementController {

    private final ServiceVirement virements;

    public VirementController(ServiceVirement virements) {
        this.virements = virements;
    }

    @PostMapping("/virements")
    public ResponseEntity<ReponseVirement> executer(@Valid @RequestBody DemandeVirement demande) {
        Virement virement = virements.executer(
                demande.ibanSource(),
                demande.ibanDestination(),
                new Montant(demande.montant()));

        // 201 + Location : la création d'une ressource se signale ainsi.
        // C'est typiquement le genre de détail qu'un test de slice verrouille.
        return ResponseEntity
                .created(URI.create("/api/virements/" + virement.reference()))
                .body(ReponseVirement.depuis(virement));
    }

    @GetMapping("/comptes/{iban}")
    public ReponseCompte consulter(@PathVariable String iban) {
        Compte compte = virements.consulter(iban);
        return ReponseCompte.depuis(compte);
    }
}
