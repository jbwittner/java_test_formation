package fr.formation.banque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application, et surtout <b>classe de configuration
 * racine</b> pour les tests.
 *
 * <p>{@code @SpringBootTest} et les slices comme {@code @DataJpaTest} remontent
 * les packages depuis la classe de test jusqu'à trouver une classe annotée
 * {@code @SpringBootApplication}. Les tests de ce module vivent dans
 * {@code fr.formation.banque.integrationbdd.*} : la remontée aboutit donc ici,
 * dans {@code fr.formation.banque}. C'est la raison pour laquelle cette classe
 * est placée à la racine du package de l'application — la placer plus bas
 * casserait la détection dans tous les tests.
 */
@SpringBootApplication
public class BanqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(BanqueApplication.class, args);
    }
}
