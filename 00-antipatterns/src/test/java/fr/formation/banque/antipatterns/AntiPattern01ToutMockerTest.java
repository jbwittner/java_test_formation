package fr.formation.banque.antipatterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.formation.banque.antipatterns.support.DepotComptes;
import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.HorodatageVirement;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.ServiceVirement;
import fr.formation.banque.domaine.TypeCompte;
import fr.formation.banque.domaine.Virement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ANTI-PATTERN 1 — tout mocker, y compris le domaine.
 *
 * <p><b>Symptôme</b> : la classe de test contient plus de {@code when(...)} que
 * d'assertions.
 *
 * <p><b>Pourquoi c'est grave</b> : mocker {@link Compte}, c'est remplacer la
 * règle métier par la réponse que le test a décidée. Le test devient une
 * tautologie — il vérifie que le mock renvoie ce qu'on lui a dit de renvoyer.
 * Il reste vert même si {@code Compte.debiter} est entièrement cassé.
 *
 * <p><b>Correction</b> : mocker les <b>ports</b> (base, broker, HTTP), utiliser
 * les <b>vrais objets</b> du domaine. Et souvent, remplacer le mock du dépôt par
 * un <i>fake</i> en mémoire, plus lisible et plus robuste.
 */
@DisplayName("Anti-pattern 1 — tout mocker")
class AntiPattern01ToutMockerTest {

    private static final Montant MILLE = Montant.euros("1000.00");
    private static final Clock HORLOGE =
            Clock.fixed(Instant.parse("2025-06-03T08:00:00Z"), ZoneId.of("Europe/Paris"));

    @Nested
    @DisplayName("✘ MAUVAIS")
    class Mauvais {

        @Test
        @DisplayName("mocke les comptes eux-mêmes : le test ne prouve rien")
        void virementOk() {
            CompteRepository comptes = mock(CompteRepository.class);
            NotificateurVirement notificateur = mock(NotificateurVirement.class);

            // Le domaine est mocké. Le test décide du solde, du type de compte,
            // et ne vérifie donc RIEN du comportement réel de Compte.
            Compte source = mock(Compte.class);
            Compte destination = mock(Compte.class);
            when(source.type()).thenReturn(TypeCompte.STANDARD);
            when(comptes.parIban("FR76-SOURCE")).thenReturn(Optional.of(source));
            when(comptes.parIban("FR76-DEST")).thenReturn(Optional.of(destination));

            ServiceVirement service = new ServiceVirement(
                    comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", notificateur);

            Virement virement = service.executer("FR76-SOURCE", "FR76-DEST", MILLE);

            // Seule chose réellement vérifiée : que le service appelle debiter().
            // Que le débit soit correct, que le solde soit suffisant, que les
            // frais soient justes — rien de tout cela n'est testé.
            verify(source).debiter(Montant.euros("1001.00"));
            verify(destination).crediter(MILLE);
            assertThat(virement).isNotNull();

            // Pire : ce test passe même si le compte source est à découvert
            // interdit, puisque le mock ne lève jamais SoldeInsuffisantException.
        }
    }

    @Nested
    @DisplayName("✔ BON")
    class Bon {

        @Test
        @DisplayName("utilise de vrais comptes : les règles sont réellement exercées")
        void devrait_debiter_le_montant_et_les_frais_quand_le_solde_suffit() {
            // Un fake en mémoire pour le port, des objets réels pour le domaine.
            Compte source = new Compte("FR76-SOURCE", TypeCompte.STANDARD,
                    Montant.euros("5000.00"), Montant.euros("0.00"));
            Compte destination = Compte.standard("FR76-DEST", Montant.euros("0.00"));
            DepotComptes comptes = new DepotComptes(source, destination);

            ServiceVirement service = new ServiceVirement(
                    comptes, new HorodatageVirement(HORLOGE), () -> "VIR-1", virement -> { });

            Virement virement = service.executer("FR76-SOURCE", "FR76-DEST", MILLE);

            // On observe l'ÉTAT réel produit par les règles réelles.
            assertThat(source.solde()).isEqualTo(Montant.euros("3999.00"));
            assertThat(destination.solde()).isEqualTo(MILLE);
            assertThat(virement.frais()).isEqualTo(Montant.euros("1.00"));
        }
    }
}
