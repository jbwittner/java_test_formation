package fr.formation.banque.domaine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object : un montant en euros.
 *
 * <p>C'est le type le plus simple du domaine, et pourtant celui qui porte le plus
 * de règles : arrondi bancaire à 2 décimales, égalité par valeur. Un value object
 * ne se mocke JAMAIS dans un test : il est déjà rapide, déterministe et sans
 * dépendance.
 *
 * <p>{@code record} donne gratuitement {@code equals}, {@code hashCode} et
 * l'immuabilité — trois propriétés qui simplifient énormément les assertions.
 */
public record Montant(BigDecimal valeur) implements Comparable<Montant> {

    public static final int DECIMALES = 2;

    /** Normalise systématiquement l'échelle : 10 et 10.00 sont le même montant. */
    public Montant {
        Objects.requireNonNull(valeur, "valeur");
        valeur = valeur.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    public static Montant euros(String valeur) {
        return new Montant(new BigDecimal(valeur));
    }

    public static Montant zero() {
        return new Montant(BigDecimal.ZERO);
    }

    public Montant plus(Montant autre) {
        return new Montant(valeur.add(autre.valeur));
    }

    public Montant moins(Montant autre) {
        return new Montant(valeur.subtract(autre.valeur));
    }

    /** Utilisé pour les frais proportionnels ; l'arrondi est fait par le constructeur. */
    public Montant multiplie(BigDecimal facteur) {
        return new Montant(valeur.multiply(facteur));
    }

    public boolean estStrictementPositif() {
        return valeur.signum() > 0;
    }

    public boolean estNegatif() {
        return valeur.signum() < 0;
    }

    @Override
    public int compareTo(Montant autre) {
        return valeur.compareTo(autre.valeur);
    }

    @Override
    public String toString() {
        return valeur.toPlainString() + " EUR";
    }
}
