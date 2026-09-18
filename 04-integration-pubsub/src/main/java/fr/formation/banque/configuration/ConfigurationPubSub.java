package fr.formation.banque.configuration;

import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration Pub/Sub de l'application.
 *
 * <p>{@code @EnableConfigurationProperties} est nécessaire car la classe
 * {@code @SpringBootApplication} de ce projet (héritée du chapitre 02) n'utilise
 * pas {@code @ConfigurationPropertiesScan}.
 *
 * <p><b>Le convertisseur JSON n'est pas optionnel.</b> Par défaut,
 * spring-cloud-gcp n'accepte que des {@code String} et des {@code byte[]} :
 * publier un objet lève
 * {@code PubSubMessageConversionException: Unable to convert payload of type ...}.
 * C'est typiquement une erreur qu'aucun test unitaire avec {@code PubSubTemplate}
 * mocké ne peut détecter — le mock accepte n'importe quel objet. Il faut un vrai
 * broker, donc un test d'intégration.
 *
 * <p>Noter le package {@code tools.jackson.databind} : Spring Boot 4 utilise
 * <b>Jackson 3</b>, où {@code ObjectMapper} a changé de package
 * ({@code com.fasterxml.jackson.databind} en Jackson 2).
 */
@Configuration
@EnableConfigurationProperties(ProprietesPubSub.class)
public class ConfigurationPubSub {

    @Bean
    PubSubMessageConverter convertisseurJson(ObjectMapper objectMapper) {
        return new JacksonPubSubMessageConverter(objectMapper);
    }
}
