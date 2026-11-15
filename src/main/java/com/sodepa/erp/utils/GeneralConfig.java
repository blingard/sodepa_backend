package com.sodepa.erp.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composant applicatif `GeneralConfig` du système ERP SODEPA.
 */
@Configuration
public class GeneralConfig {
    @Bean
    public ObjectMapper objectMapper() {
        // FAIL_ON_UNKNOWN_PROPERTIES désactivé : les adaptateurs relisent les charges
        // utiles du maker-checker avec convertValue, et ces charges portent des clés
        // techniques ajoutées à la soumission (id, circuitWorkflow…) que les records
        // d'entrée ne déclarent pas. Avec le réglage par défaut, toute validation
        // d'engagement ou de réallocation échouait en 400 « Unrecognized field ».
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
