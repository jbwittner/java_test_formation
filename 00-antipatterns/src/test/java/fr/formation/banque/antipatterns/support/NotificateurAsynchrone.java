package fr.formation.banque.antipatterns.support;

import fr.formation.banque.domaine.NotificateurVirement;
import fr.formation.banque.domaine.Virement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Double de test qui imite un envoi asynchrone (le publieur Pub/Sub du chapitre 04
 * se comporte ainsi : la méthode rend la main avant que le message soit parti).
 *
 * <p>Le délai est volontairement variable pour reproduire ce qui se passe en
 * intégration continue : une machine chargée, et le {@code Thread.sleep} calibré
 * sur un poste de développement ne suffit plus.
 */
public class NotificateurAsynchrone implements NotificateurVirement, AutoCloseable {

    private final List<Virement> recus = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService ordonnanceur = Executors.newSingleThreadScheduledExecutor();
    private final Duration delai;

    public NotificateurAsynchrone(Duration delai) {
        this.delai = delai;
    }

    @Override
    public void virementExecute(Virement virement) {
        ordonnanceur.schedule(() -> recus.add(virement), delai.toMillis(), TimeUnit.MILLISECONDS);
    }

    public List<Virement> recus() {
        return List.copyOf(recus);
    }

    @Override
    public void close() {
        ordonnanceur.shutdownNow();
    }
}
