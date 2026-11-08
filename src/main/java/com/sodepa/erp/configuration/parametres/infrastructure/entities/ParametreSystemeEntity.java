package com.sodepa.erp.configuration.parametres.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité de persistance représentant `ParametreSystemeEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "app_parametres_systeme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreSystemeEntity {

    /**
     * Champ cle : cle utilisé pour le pilotage et le suivi dans ParametreSystemeEntity.
     */
    @Id
    @Column(name = "cle", length = 100, nullable = false)
    private String cle;

    /**
     * Champ valeur : valeur utilisé pour le pilotage et le suivi dans ParametreSystemeEntity.
     */
    @Column(name = "valeur", length = 255, nullable = false)
    private String valeur;

    @Column(name = "type_valeur", length = 30, nullable = false)
    private String typeValeur; // DECIMAL, INTEGER, STRING, BOOLEAN

    @Column(name = "categorie", length = 50, nullable = false)
    private String categorie; // PAIE, ACHATS, CHEPTEL, TRESORERIE, FORMATION, SECURITE

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Champ modifiablePar : modifiable par utilisé pour le pilotage et le suivi dans ParametreSystemeEntity.
     */
    @Column(name = "modifiable_par", length = 100)
    private String modifiablePar;

    /**
     * Champ derniereModification : derniere modification utilisé pour le pilotage et le suivi dans ParametreSystemeEntity.
     */
    @Column(name = "derniere_modification")
    private LocalDateTime derniereModification;

    /**
     * Champ modifiePar : modifie par utilisé pour le pilotage et le suivi dans ParametreSystemeEntity.
     */
    @Column(name = "modifie_par", length = 100)
    private String modifiePar;
}
