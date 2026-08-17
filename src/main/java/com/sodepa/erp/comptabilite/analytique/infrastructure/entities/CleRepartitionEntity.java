package com.sodepa.erp.comptabilite.analytique.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une clé de répartition analytique préconfigurée.
 * Permet d'automatiser la ventilation d'un montant sur plusieurs sections analytiques.
 */
@Entity
@Table(name = "cles_repartition")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CleRepartitionEntity {

    /**
     * Identifiant unique de la clé de répartition.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Code unique identifiant la clé (ex: 'ADMIN', 'VENTIL_RD').
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Libellé ou intitulé descriptif de la clé de répartition.
     */
    @Column(nullable = false)
    private String intitule;

    /**
     * Indique si la clé est active et utilisable.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    /**
     * Liste des détails (sections et pourcentages) rattachés à cette clé.
     */
    @OneToMany(mappedBy = "cle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetailCleRepartitionEntity> details = new ArrayList<>();

    /**
     * Ajoute un détail de répartition de manière bidirectionnelle.
     * 
     * @param detail le détail à ajouter
     */
    public void addDetail(DetailCleRepartitionEntity detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
        detail.setCle(this);
    }
}
