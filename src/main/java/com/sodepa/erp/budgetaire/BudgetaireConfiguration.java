package com.sodepa.erp.budgetaire;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Réglages communs du module de gestion budgétaire.
 *
 * <p>
 * L'horloge est injectée plutôt que lue par {@code LocalDate.now()} : les règles
 * budgétaires dépendent de la date (année en cours, date d'effet d'un seuil,
 * régime transitoire de janvier), et un test doit pouvoir fixer ce « aujourd'hui ».
 * Elle est réglée sur le fuseau du Cameroun : un serveur en UTC changerait
 * d'année une heure après la SODEPA.
 * </p>
 */
@Configuration
public class BudgetaireConfiguration {

    /** Fuseau de référence des dates budgétaires (UTC+1, sans heure d'été). */
    public static final ZoneId FUSEAU_SODEPA = ZoneId.of("Africa/Douala");

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock horlogeSodepa() {
        return Clock.system(FUSEAU_SODEPA);
    }
}
