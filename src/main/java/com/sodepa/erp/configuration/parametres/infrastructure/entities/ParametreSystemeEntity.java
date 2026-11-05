package com.sodepa.erp.configuration.parametres.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_parametres_systeme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametreSystemeEntity {

    @Id
    @Column(name = "cle", length = 100, nullable = false)
    private String cle;

    @Column(name = "valeur", length = 255, nullable = false)
    private String valeur;

    @Column(name = "type_valeur", length = 30, nullable = false)
    private String typeValeur; // DECIMAL, INTEGER, STRING, BOOLEAN

    @Column(name = "categorie", length = 50, nullable = false)
    private String categorie; // PAIE, ACHATS, CHEPTEL, TRESORERIE, FORMATION, SECURITE

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "modifiable_par", length = 100)
    private String modifiablePar;

    @Column(name = "derniere_modification")
    private LocalDateTime derniereModification;

    @Column(name = "modifie_par", length = 100)
    private String modifiePar;
}
