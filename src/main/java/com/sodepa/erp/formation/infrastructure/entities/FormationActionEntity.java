package com.sodepa.erp.formation.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rh_formation_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationActionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private FormationPlanEntity plan;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String theme;

    @Column(nullable = false, length = 100)
    private String domaine;

    @Column(name = "public_cible", length = 200)
    private String publicCible;

    @Column(name = "organisme_formateur", length = 150)
    private String organismeFormateur;

    @Column(name = "type_formateur", length = 30)
    @Builder.Default
    private String typeFormateur = "EXTERNE";

    @Column(length = 150)
    private String lieu;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "duree_heures")
    private Integer dureeHeures;

    @Column(name = "cout_estimatif_fcfa", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal coutEstimatifFcfa = BigDecimal.ZERO;

    @Column(name = "donne_droit_reclassement", nullable = false)
    @Builder.Default
    private boolean donneDroitReclassement = false;

    @OneToMany(mappedBy = "action", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormationParticipationEntity> participations = new ArrayList<>();
}
