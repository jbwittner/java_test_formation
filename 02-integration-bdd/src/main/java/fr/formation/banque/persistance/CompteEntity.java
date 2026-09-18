package fr.formation.banque.persistance;

import fr.formation.banque.domaine.Compte;
import fr.formation.banque.domaine.Devise;
import fr.formation.banque.domaine.Montant;
import fr.formation.banque.domaine.TypeCompte;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/**
 * Entité JPA : la représentation <b>de stockage</b> d'un compte.
 *
 * <p>Elle est volontairement distincte de {@link Compte}, l'objet du domaine.
 * Le domaine reste testable sans JPA (chapitre 01), et la base peut évoluer
 * sans contaminer les règles métier.
 *
 * <p>Le champ {@link Version} active le <b>verrouillage optimiste</b> : Hibernate
 * ajoute {@code WHERE version = ?} à chaque {@code UPDATE} et compte les lignes
 * modifiées. Si un autre transaction a déjà incrémenté la version, zéro ligne
 * est affectée et Spring lève {@code ObjectOptimisticLockingFailureException}.
 * C'est un comportement que H2 ne reproduit pas fidèlement — d'où Testcontainers.
 */
@Entity
@Table(name = "compte")
public class CompteEntity {

    // SEQUENCE plutot qu'IDENTITY : avec IDENTITY, Hibernate doit executer
    // l'INSERT immediatement pour obtenir la cle, ce qui interdit le
    // regroupement des ecritures ET masque les contraintes de base derriere
    // un comportement different de la production. Avec une sequence, les
    // INSERT partent au flush -- c'est le comportement demontre dans
    // RollbackTransactionnelIT.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "compte_seq")
    @SequenceGenerator(name = "compte_seq", sequenceName = "compte_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 34)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TypeCompte type;

    @Column(name = "solde", nullable = false, precision = 19, scale = 2)
    private BigDecimal solde;

    @Column(name = "decouvert_autorise", nullable = false, precision = 19, scale = 2)
    private BigDecimal decouvertAutorise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Devise devise;

    @Version
    @Column(nullable = false)
    private Long version;

    protected CompteEntity() {
        // requis par JPA
    }

    public CompteEntity(String iban, TypeCompte type, BigDecimal solde,
                        BigDecimal decouvertAutorise, Devise devise) {
        this.iban = iban;
        this.type = type;
        this.solde = solde;
        this.decouvertAutorise = decouvertAutorise;
        this.devise = devise;
    }

    public static CompteEntity depuisDomaine(Compte compte) {
        return new CompteEntity(
                compte.iban(),
                compte.type(),
                compte.solde().valeur(),
                compte.decouvertAutorise().valeur(),
                compte.solde().devise());
    }

    public Compte versDomaine() {
        return new Compte(iban, type,
                new Montant(solde, devise),
                new Montant(decouvertAutorise, devise));
    }

    /** Reporte l'état du domaine sur la ligne existante (sans toucher à l'identité). */
    public void mettreAJourDepuis(Compte compte) {
        this.type = compte.type();
        this.solde = compte.solde().valeur();
        this.decouvertAutorise = compte.decouvertAutorise().valeur();
        this.devise = compte.solde().devise();
    }

    public Long id() {
        return id;
    }

    public String iban() {
        return iban;
    }

    public TypeCompte type() {
        return type;
    }

    public BigDecimal solde() {
        return solde;
    }

    public void changerSolde(BigDecimal solde) {
        this.solde = solde;
    }

    public BigDecimal decouvertAutorise() {
        return decouvertAutorise;
    }

    public Devise devise() {
        return devise;
    }

    public Long version() {
        return version;
    }
}
