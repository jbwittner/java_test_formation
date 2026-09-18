package fr.formation.banque.unitaire.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteIntrouvableException;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.GenerateurReference;
import fr.formation.banque.domaine.HorodatageVirement;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.SoldeInsuffisantException;
import fr.formation.banque.domaine.TypeCompte;
import fr.formation.banque.domaine.Virement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DÉMO 3 — mocker, mais mocker quoi ?
 *
 * <p>Règle simple et suffisante : <b>on mocke les ports, jamais le domaine.</b>
 * <ul>
 *   <li>{@link CompteRepository} et {@link NotificateurVirement} sont des ports :
 *       leurs implémentations réelles parlent à PostgreSQL et à Pub/Sub. Les
 *       mocker est la seule façon de garder ce test à 5 ms. ✔</li>
 *   <li>{@link Compte}, {@link Montant} et {@code GrilleFrais} sont du domaine pur.
 *       Les mocker reviendrait à réécrire la règle dans le test : le test
 *       passerait même si le code de production était faux. ✘</li>
 *   <li>{@link HorodatageVirement} n'est pas mocké non plus : on lui injecte un
 *       {@link Clock#fixed} — un vrai objet, rendu déterministe. Toujours
 *       préférer un objet réel figé à un mock quand c'est possible.</li>
 * </ul>
 *
 * <p>Sur les vérifications : on {@code verify} ce qui EST le comportement attendu
 * (« la notification part », « rien n'est enregistré en cas d'échec »). On ne
 * vérifie pas chaque appel technique, sinon le test casse à la première
 * refactorisation sans qu'aucun bug n'ait été introduit (anti-pattern n°8).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceVirement — orchestration d'un virement")
class ServiceVirementTest {

    private static final Montant MILLE_EUROS = Montant.euros("1000.00");

    @Mock
    private CompteRepository comptes;

    @Mock
    private NotificateurVirement notificateur;

    @Captor
    private ArgumentCaptor<Virement> virementPublie;

    private ServiceVirement service;

    @BeforeEach
    void preparerService() {
        // Horloge figée : mardi 3 juin 2025 à 10 h 00, donc avant la coupure.
        // La date de valeur est ainsi connue à l'avance, dans tous les environnements.
        Clock horloge = Clock.fixed(Instant.parse("2025-06-03T08:00:00Z"), ZoneId.of("Europe/Paris"));
        service = new ServiceVirement(
                comptes,
                new HorodatageVirement(horloge),
                () -> "VIR-TEST-1",
                notificateur);
    }

    private Compte donnerCompte(String iban, TypeCompte type, String solde) {
        Compte compte = new Compte(iban, type, Montant.euros(solde), Montant.euros("0.00"));
        when(comptes.parIban(iban)).thenReturn(Optional.of(compte));
        return compte;
    }

    @Test
    @DisplayName("débite le montant augmenté des frais et crédite le destinataire")
    void devrait_debiter_montant_et_frais_quand_le_virement_reussit() {
        Compte source = donnerCompte("FR76-SOURCE", TypeCompte.STANDARD, "5000.00");
        Compte destination = donnerCompte("FR76-DEST", TypeCompte.STANDARD, "0.00");

        Virement virement = service.executer("FR76-SOURCE", "FR76-DEST", MILLE_EUROS);

        // Frais palier bas : 1000 EUR -> forfait 1,00 EUR. La valeur attendue est
        // écrite en dur, PAS recalculée avec GrilleFrais : un test qui rejoue la
        // formule de production ne teste que lui-même.
        assertThat(virement.frais()).isEqualTo(Montant.euros("1.00"));
        assertThat(source.solde()).isEqualTo(Montant.euros("3999.00"));
        assertThat(destination.solde()).isEqualTo(MILLE_EUROS);
    }

    @Test
    @DisplayName("exonère de frais un compte premium")
    void devrait_appliquer_des_frais_nuls_quand_le_compte_source_est_premium() {
        donnerCompte("FR76-SOURCE", TypeCompte.PREMIUM, "5000.00");
        donnerCompte("FR76-DEST", TypeCompte.STANDARD, "0.00");

        Virement virement = service.executer("FR76-SOURCE", "FR76-DEST", MILLE_EUROS);

        assertThat(virement.frais()).isEqualTo(Montant.euros("0.00"));
        assertThat(virement.totalDebite()).isEqualTo(MILLE_EUROS);
    }

    @Test
    @DisplayName("date le virement au jour même avant l'heure de coupure")
    void devrait_dater_au_jour_meme_quand_l_heure_est_avant_la_coupure() {
        donnerCompte("FR76-SOURCE", TypeCompte.STANDARD, "5000.00");
        donnerCompte("FR76-DEST", TypeCompte.STANDARD, "0.00");

        Virement virement = service.executer("FR76-SOURCE", "FR76-DEST", MILLE_EUROS);

        // Déterministe grâce à l'horloge figée : ce test donnera le même résultat
        // dans dix ans, à 3 h du matin, sur la machine d'intégration continue.
        assertThat(virement.dateDeValeur()).isEqualTo(LocalDate.of(2025, 6, 3));
    }

    @Test
    @DisplayName("publie un événement décrivant le virement exécuté")
    void devrait_notifier_le_virement_quand_il_reussit() {
        donnerCompte("FR76-SOURCE", TypeCompte.STANDARD, "5000.00");
        donnerCompte("FR76-DEST", TypeCompte.STANDARD, "0.00");

        service.executer("FR76-SOURCE", "FR76-DEST", MILLE_EUROS);

        // ArgumentCaptor : on ne se contente pas de « la méthode a été appelée »,
        // on inspecte CE QUI a été publié. C'est la différence entre vérifier un
        // appel et vérifier un comportement.
        verify(notificateur).virementExecute(virementPublie.capture());
        assertThat(virementPublie.getValue())
                .returns("VIR-TEST-1", Virement::reference)
                .returns("FR76-SOURCE", Virement::ibanSource)
                .returns("FR76-DEST", Virement::ibanDestination)
                .returns(MILLE_EUROS, Virement::montant);
    }

    @Test
    @DisplayName("n'enregistre ni ne notifie quand le solde est insuffisant")
    void devrait_ne_rien_enregistrer_quand_le_solde_est_insuffisant() {
        donnerCompte("FR76-SOURCE", TypeCompte.STANDARD, "100.00");
        donnerCompte("FR76-DEST", TypeCompte.STANDARD, "0.00");

        assertThatExceptionOfType(SoldeInsuffisantException.class)
                .isThrownBy(() -> service.executer("FR76-SOURCE", "FR76-DEST", MILLE_EUROS));

        // Le « never » est ici un vrai test de régression : publier un événement
        // pour un virement qui n'a pas eu lieu serait un bug grave et silencieux.
        verify(comptes, never()).enregistrer(any());
        verify(notificateur, never()).virementExecute(any());
    }

    @Test
    @DisplayName("échoue quand le compte source est inconnu")
    void devrait_lever_CompteIntrouvableException_quand_l_iban_source_est_inconnu() {
        when(comptes.parIban("FR76-INCONNU")).thenReturn(Optional.empty());

        assertThatExceptionOfType(CompteIntrouvableException.class)
                .isThrownBy(() -> service.executer("FR76-INCONNU", "FR76-DEST", MILLE_EUROS))
                .withMessageContaining("FR76-INCONNU");
    }

    @Test
    @DisplayName("refuse un virement d'un compte vers lui-même")
    void devrait_refuser_quand_la_source_et_la_destination_sont_identiques() {
        // Aucun stub n'est nécessaire : la règle est vérifiée avant tout chargement.
        // MockitoExtension échouerait d'ailleurs sur un stub inutilisé (strict stubs) —
        // c'est une aide, pas une gêne : elle signale les tests qui préparent trop.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.executer("FR76-SOURCE", "FR76-SOURCE", MILLE_EUROS));
    }
}
