package fr.formation.banque.api;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteIntrouvableException;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.Devise;
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
 */
@RestController
@RequestMapping("/api")
public class VirementController {

    private final ServiceVirement virements;
    private final CompteRepository comptes;

    public VirementController(ServiceVirement virements, CompteRepository comptes) {
        this.virements = virements;
        this.comptes = comptes;
    }

    @PostMapping("/virements")
    public ResponseEntity<ReponseVirement> executer(@Valid @RequestBody DemandeVirement demande) {
        Virement virement = virements.executer(
                demande.ibanSource(),
                demande.ibanDestination(),
                new Montant(demande.montant(), Devise.EUR));

        // 201 + Location : la création d'une ressource se signale ainsi.
        // C'est typiquement le genre de détail qu'un test de slice verrouille.
        return ResponseEntity
                .created(URI.create("/api/virements/" + virement.reference()))
                .body(ReponseVirement.depuis(virement));
    }

    @GetMapping("/comptes/{iban}")
    public ReponseCompte consulter(@PathVariable String iban) {
        Compte compte = comptes.parIban(iban).orElseThrow(() -> new CompteIntrouvableException(iban));
        return ReponseCompte.depuis(compte);
    }
}
