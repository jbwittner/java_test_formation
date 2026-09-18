package fr.formation.banque.domaine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object : un montant est indissociable de sa devise.
 *
 * <p>C'est le type le plus simple du domaine, et pourtant celui qui porte le plus
 * de règles : arrondi bancaire à 2 décimales, refus de mélanger les devises,
 * égalité par valeur. Un value object ne se mocke JAMAIS dans un test : il est
 * déjà rapide, déterministe et sans dépendance.
 *
 * <p>{@code record} donne gratuitement {@code equals}, {@code hashCode} et
 * l'immuabilité — trois propriétés qui simplifient énormément les assertions.
 */
public record Montant(BigDecimal valeur, Devise devise) implements Comparable<Montant> {

    public static final int DECIMALES = 2;

    /** Normalise systématiquement l'échelle : 10 EUR et 10.00 EUR sont le même montant. */
    public Montant {
        Objects.requireNonNull(valeur, "valeur");
        Objects.requireNonNull(devise, "devise");
        valeur = valeur.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    public static Montant de(String valeur, Devise devise) {
        return new Montant(new BigDecimal(valeur), devise);
    }

    public static Montant euros(String valeur) {
        return de(valeur, Devise.EUR);
    }

    public static Montant zero(Devise devise) {
        return new Montant(BigDecimal.ZERO, devise);
    }

    public Montant plus(Montant autre) {
        verifierMemeDevise(autre);
        return new Montant(valeur.add(autre.valeur), devise);
    }

    public Montant moins(Montant autre) {
        verifierMemeDevise(autre);
        return new Montant(valeur.subtract(autre.valeur), devise);
    }

    /** Utilisé pour les frais proportionnels ; l'arrondi est fait par le constructeur. */
    public Montant multiplie(BigDecimal facteur) {
        return new Montant(valeur.multiply(facteur), devise);
    }

    public Montant negatif() {
        return new Montant(valeur.negate(), devise);
    }

    public boolean estStrictementPositif() {
        return valeur.signum() > 0;
    }

    public boolean estNegatif() {
        return valeur.signum() < 0;
    }

    /**
     * Comparaison métier : deux montants ne sont comparables que dans la même devise.
     *
     * <p>Attention, {@code compareTo} est ici volontairement incohérent avec
     * {@code equals} pour les devises différentes (il lève au lieu de renvoyer un
     * ordre arbitraire) : mieux vaut une exception qu'une comparaison silencieusement fausse.
     */
    @Override
    public int compareTo(Montant autre) {
        verifierMemeDevise(autre);
        return valeur.compareTo(autre.valeur);
    }

    private void verifierMemeDevise(Montant autre) {
        if (devise != autre.devise) {
            throw new DeviseIncompatibleException(devise, autre.devise);
        }
    }

    @Override
    public String toString() {
        return valeur.toPlainString() + " " + devise;
    }
}
