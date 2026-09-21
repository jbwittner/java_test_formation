package fr.formation.banque.api;

import fr.formation.banque.domaine.CompteIntrouvableException;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduction des exceptions du domaine en réponses HTTP normalisées
 * ({@code application/problem+json}, RFC 9457).
 *
 * <p>C'est une pièce de code <b>souvent oubliée par les tests</b>, alors qu'elle
 * fait partie du contrat public de l'API : un client qui reçoit un 500 au lieu
 * d'un 409 ne peut pas réagir correctement. La slice {@code @WebMvcTest} est
 * l'outil idéal pour la couvrir, puisqu'il suffit de faire lever l'exception
 * voulue par le service mocké.
 *
 * <p>Choix des codes :
 * <ul>
 *   <li>404 — compte inconnu ;</li>
 *   <li>409 — solde insuffisant : la requête est valide, c'est l'état du compte
 *       qui l'empêche. Un 400 laisserait croire à une erreur de saisie ;</li>
 *   <li>400 — requête malformée ou incohérente (validation, IBAN identiques).</li>
 * </ul>
 */
@RestControllerAdvice
public class GestionnaireErreursApi extends ResponseEntityExceptionHandler {

    @ExceptionHandler(CompteIntrouvableException.class)
    ProblemDetail compteIntrouvable(CompteIntrouvableException erreur) {
        return probleme(HttpStatus.NOT_FOUND, "Compte introuvable", erreur.getMessage());
    }

    @ExceptionHandler(SoldeInsuffisantException.class)
    ProblemDetail soldeInsuffisant(SoldeInsuffisantException erreur) {
        ProblemDetail probleme = probleme(HttpStatus.CONFLICT, "Solde insuffisant", erreur.getMessage());
        probleme.setProperty("iban", erreur.iban());
        return probleme;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail requeteInvalide(IllegalArgumentException erreur) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", erreur.getMessage());
    }

    /**
     * Enrichit la réponse 400 de validation avec le détail champ par champ :
     * sans cela, le client reçoit un 400 sans savoir quel champ corriger.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException erreur, HttpHeaders entetes,
            HttpStatusCode statut, WebRequest requete) {
        ProblemDetail probleme = probleme(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Un ou plusieurs champs sont invalides");
        Map<String, String> champs = new LinkedHashMap<>();
        erreur.getBindingResult().getFieldErrors()
                .forEach(champ -> champs.put(champ.getField(), champ.getDefaultMessage()));
        probleme.setProperty("champs", champs);
        return ResponseEntity.badRequest().body(probleme);
    }

    private static ProblemDetail probleme(HttpStatus statut, String titre, String detail) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(titre);
        return probleme;
    }
}
