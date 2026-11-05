package com.sodepa.erp.rh.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant le contrat de travail d'un employé SODEPA,
 * soumis au circuit d'approbation hiérarchique et à la signature finale
 * du Directeur Général par Note de Service ou Circulaire officielle.
 */
@Entity
@Table(name = "rh_contrats_travail", indexes = {
    @Index(name = "idx_contrat_numero", columnList = "numero_contrat", unique = true),
    @Index(name = "idx_contrat_employe", columnList = "employe_id"),
    @Index(name = "idx_contrat_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratTravailEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }

    @Column(name = "numero_contrat", nullable = false, unique = true, length = 50)
    private String numeroContrat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 30)
    @Builder.Default
    private TypeContrat typeContrat = TypeContrat.CDI;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutContrat statut = StatutContrat.EN_ATTENTE_VALIDATION;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin_essai")
    private LocalDate dateFinEssai;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Column(nullable = false)
    private Integer categorie;

    @Column(nullable = false, length = 10)
    private String echelon;

    @Column(name = "salaire_base", precision = 15, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "reference_note_service_dg", length = 150)
    private String referenceNoteServiceDg;

    @Column(name = "date_signature_dg")
    private LocalDate dateSignatureDg;

    @Column(name = "signe_par_dg", nullable = false)
    @Builder.Default
    private Boolean signeParDg = false;

    @Column(name = "workflow_instance_id")
    private UUID workflowInstanceId;

    @Column(name = "maker_checker_ticket_id")
    private UUID makerCheckerTicketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private PosteEntity poste;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
