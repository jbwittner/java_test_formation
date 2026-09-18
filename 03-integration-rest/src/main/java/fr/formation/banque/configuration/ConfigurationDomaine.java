package fr.formation.banque.configuration;

import fr.formation.banque.domaine.CompteRepository;
import fr.formation.banque.domaine.GenerateurReference;
import fr.formation.banque.domaine.HorodatageVirement;
import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.ServiceVirement;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du domaine dans Spring.
 *
 * <p>Le domaine reste dépourvu d'annotations Spring : c'est cette configuration
 * qui l'assemble. Conséquence pratique pour les tests : {@link ServiceVirement}
 * s'instancie toujours à la main en test unitaire (chapitre 01), et Spring ne
 * sert que lorsqu'on veut réellement l'intégration.
 *
 * <p>Le {@link Clock} est un bean : un test d'intégration peut donc le remplacer
 * par une horloge figée avec {@code @MockitoBean} ou une configuration de test,
 * exactement comme en test unitaire.
 */
@Configuration
public class ConfigurationDomaine {

    @Bean
    Clock horlogeSysteme() {
        return Clock.systemDefaultZone();
    }

    @Bean
    HorodatageVirement horodatageVirement(Clock horloge) {
        return new HorodatageVirement(horloge);
    }

    @Bean
    GenerateurReference generateurReference() {
        return GenerateurReference.aleatoire();
    }

    /**
     * Notificateur neutre pour ce chapitre : le vrai publieur Pub/Sub arrive au
     * chapitre 04. Un bean par défaut évite d'avoir à mocker une dépendance qui
     * n'est pas le sujet ici.
     *
     * <p>{@code @ConditionalOnMissingBean} laisse le chapitre 04 fournir sa
     * propre implémentation sans conflit : dès qu'un {@link NotificateurVirement}
     * est déclaré ailleurs, celui-ci s'efface.
     */
    @Bean
    @ConditionalOnMissingBean(NotificateurVirement.class)
    NotificateurVirement notificateurNeutre() {
        return virement -> { };
    }

    @Bean
    ServiceVirement serviceVirement(CompteRepository comptes,
                                    HorodatageVirement horodatage,
                                    GenerateurReference references,
                                    NotificateurVirement notificateur) {
        return new ServiceVirement(comptes, horodatage, references, notificateur);
    }
}
