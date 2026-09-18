package fr.formation.banque.domaine;

import java.util.UUID;

/**
 * Port de génération d'identifiants.
 *
 * <p>Même raison d'être que le {@link java.time.Clock} de {@link HorodatageVirement} :
 * isoler une source de non-déterminisme derrière une interface pour que le test
 * puisse la figer.
 */
@FunctionalInterface
public interface GenerateurReference {

    String suivante();

    /** Implémentation de production. */
    static GenerateurReference aleatoire() {
        return () -> "VIR-" + UUID.randomUUID();
    }
}
