-- Journal des evenements consommes.
--
-- La cle primaire est la reference fonctionnelle du virement : c'est le
-- mecanisme d'idempotence. Pub/Sub livre AU MOINS UNE FOIS -- un consommateur
-- qui ne gere pas la redelivrance finit par traiter deux fois le meme message.
CREATE TABLE virement_recu (
    reference        VARCHAR(64)    PRIMARY KEY,
    iban_source      VARCHAR(34)    NOT NULL,
    iban_destination VARCHAR(34)    NOT NULL,
    montant          NUMERIC(19, 2) NOT NULL,
    recu_le          TIMESTAMP      NOT NULL
);
