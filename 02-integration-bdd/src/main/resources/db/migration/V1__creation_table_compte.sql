-- Premiere migration : structure de la table compte.
--
-- Les migrations sont du CODE DE PRODUCTION : elles se testent, exactement
-- comme le reste. Un test qui demarre sur une base vide et laisse Flyway
-- appliquer les migrations prouve que le schema livre correspond bien aux
-- entites JPA (cf. spring.jpa.hibernate.ddl-auto=validate).
-- allocationSize=50 cote JPA doit correspondre a INCREMENT BY 50 ici,
-- sinon Hibernate distribue des identifiants deja utilises.
CREATE SEQUENCE compte_seq INCREMENT BY 50 START WITH 1;

CREATE TABLE compte (
    id      BIGINT         PRIMARY KEY,
    iban    VARCHAR(34)    NOT NULL,
    solde   NUMERIC(19, 2) NOT NULL,
    version BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT compte_iban_unique UNIQUE (iban),
    CONSTRAINT compte_solde_positif CHECK (solde >= 0)
);

COMMENT ON COLUMN compte.version IS 'Verrouillage optimiste (@Version cote JPA)';
