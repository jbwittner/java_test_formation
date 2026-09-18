package fr.formation.banque.evenement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trace d'un événement consommé.
 *
 * <p>La clé primaire est la <b>référence du virement</b>, pas un identifiant
 * technique : c'est ce qui rend le consommateur idempotent. Pub/Sub garantit une
 * livraison <i>au moins une fois</i> — un même message peut donc arriver deux
 * fois, et la base refusera le doublon.
 */
@Entity
@Table(name = "virement_recu")
public class VirementRecuEntity {

    @Id
    @Column(length = 64)
    private String reference;

    @Column(name = "iban_source", nullable = false, length = 34)
    private String ibanSource;

    @Column(name = "iban_destination", nullable = false, length = 34)
    private String ibanDestination;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    @Column(name = "recu_le", nullable = false)
    private Instant recuLe;

    protected VirementRecuEntity() {
        // requis par JPA
    }

    public VirementRecuEntity(String reference, String ibanSource, String ibanDestination,
                              BigDecimal montant, Instant recuLe) {
        this.reference = reference;
        this.ibanSource = ibanSource;
        this.ibanDestination = ibanDestination;
        this.montant = montant;
        this.recuLe = recuLe;
    }

    public String reference() {
        return reference;
    }

    public String ibanSource() {
        return ibanSource;
    }

    public String ibanDestination() {
        return ibanDestination;
    }

    public BigDecimal montant() {
        return montant;
    }

    public Instant recuLe() {
        return recuLe;
    }
}
