package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import fr.formation.banque.antipatterns.support.DepotComptes;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.HorodatageVirement;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.Virement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * ANTI-PATTERN 8 — mocks sur-spécifiés.
 *
 * <p><b>Symptôme</b> : {@code verify(..., times(2))} sur chaque appel,
 * {@code verifyNoMoreInteractions}, {@code inOrder(...)} systématique.
 *
 * <p><b>Pourquoi c'est grave</b> : le test fige l'<b>implémentation</b> au lieu
 * de décrire le <b>comportement</b>. Le moindre remaniement sans changement de
 * comportement — mettre en cache un chargement, enregistrer les deux comptes
 * en une seule fois, inverser deux lignes indépendantes — fait passer au rouge
 * des tests qui n'ont détecté aucun bug. À force, l'équipe modifie le test pour
 * qu'il passe sans le lire : le filet de sécurité est devenu un frein.
 *
 * <p><b>Correction</b> : vérifier les interactions qui <b>font partie du
 * contrat</b> (« une notification part », « rien n'est enregistré en cas
 * d'échec ») et ignorer le reste. Mieux encore : quand c'est possible, remplacer
 * le mock par un <i>fake</i> et asserter sur l'<b>état</b> obtenu plutôt que sur
 * les appels effectués.
 */
@DisplayName("Anti-pattern 8 — mocks sur-spécifiés")
class AntiPattern08MocksSurSpecifiesTest {

    private static final Montant MILLE = Montant.euros("1000.00");
    private static final Clock HORLOGE =
            Clock.fixed(Instant.parse("2025-06-03T08:00:00Z"), ZoneId.of("Europe/Paris"));

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @DisplayName("fige le nombre et l'ordre exacts des appels au dépôt")
        void testVirementInteractions() {
            CompteRepository comptes = mock(CompteRepository.class);
            NotificateurVirement notificateur = mock(NotificateurVirement.class);
            Compte source = new Compte("FR76-SOURCE", Montant.euros("5000.00"));
            Compte destination = new Compte("FR76-DEST", Montant.euros("0.00"));
            when(comptes.parIban("FR76-SOURCE")).thenReturn(Optional.of(source));
            when(comptes.parIban("FR76-DEST")).thenReturn(Optional.of(destination));

            new ServiceVirement(comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", notificateur)
                    .executer("FR76-SOURCE", "FR76-DEST", MILLE);

            // Chacune de ces lignes casse au premier remaniement interne :
            // - ajouter un cache de comptes -> times(1) devient faux ;
            // - regrouper les deux enregistrements -> times(2) devient faux ;
            // - déplacer la notification avant l'enregistrement -> inOrder casse.
            // Aucun de ces changements ne modifie pourtant le comportement observable.
            InOrder ordre = inOrder(comptes, notificateur);
            ordre.verify(comptes).parIban("FR76-SOURCE");
            ordre.verify(comptes).parIban("FR76-DEST");
            ordre.verify(comptes).enregistrer(source);
            ordre.verify(comptes).enregistrer(destination);
            ordre.verify(notificateur).virementExecute(any());
            verify(comptes, times(2)).enregistrer(any());
            verifyNoMoreInteractions(comptes, notificateur);
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("vérifie l'état obtenu, pas la séquence d'appels")
        void devrait_enregistrer_les_deux_comptes_a_jour_quand_le_virement_reussit() {
            // Fake plutôt que mock : on assère sur ce qui a été RANGÉ,
            // pas sur la façon dont on l'a rangé.
            DepotComptes comptes = new DepotComptes(
                    new Compte("FR76-SOURCE", Montant.euros("5000.00")),
                    new Compte("FR76-DEST", Montant.euros("0.00")));

            new ServiceVirement(comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", v -> { })
                    .executer("FR76-SOURCE", "FR76-DEST", MILLE);

            assertThat(comptes.parIban("FR76-SOURCE").orElseThrow().solde())
                    .isEqualTo(Montant.euros("3999.00"));
            assertThat(comptes.parIban("FR76-DEST").orElseThrow().solde())
                    .isEqualTo(MILLE);
        }

        @Test
        @DisplayName("vérifie la seule interaction qui fait partie du contrat")
        void devrait_publier_une_notification_quand_le_virement_reussit() {
            DepotComptes comptes = new DepotComptes(
                    new Compte("FR76-SOURCE", Montant.euros("5000.00")),
                    new Compte("FR76-DEST", Montant.euros("0.00")));
            NotificateurVirement notificateur = mock(NotificateurVirement.class);

            new ServiceVirement(comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", notificateur)
                    .executer("FR76-SOURCE", "FR76-DEST", MILLE);

            // La notification EST le comportement attendu (un autre système en
            // dépend) : la vérifier est légitime. Son ordre par rapport à
            // l'enregistrement ne l'est pas.
            verify(notificateur).virementExecute(any(Virement.class));
        }
    }
}
