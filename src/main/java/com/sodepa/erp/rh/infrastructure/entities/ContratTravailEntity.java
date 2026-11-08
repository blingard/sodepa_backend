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
/**
 * Entité de persistance représentant `ContratTravailEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratTravailEntity implements Persistable<UUID> {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Indicateur booléen spécifiant si isNew est effectif ou applicable.
     */
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

    /**
     * Champ numeroContrat : numero contrat utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "numero_contrat", nullable = false, unique = true, length = 50)
    private String numeroContrat;

    /**
     * Employé ou collaborateur SODEPA concerné dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private EmployeEntity employe;

    /**
     * Champ typeContrat : type contrat utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 30)
    @Builder.Default
    private TypeContrat typeContrat = TypeContrat.CDI;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutContrat statut = StatutContrat.EN_ATTENTE_VALIDATION;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Champ dateFinEssai : date fin essai utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "date_fin_essai")
    private LocalDate dateFinEssai;

    /**
     * Champ dateFinContrat : date fin contrat utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    /**
     * Catégorie de rattachement administratif ou technique dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    private Integer categorie;

    /**
     * Champ echelon : echelon utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(nullable = false, length = 10)
    private String echelon;

    /**
     * Champ salaireBase : salaire base utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "salaire_base", precision = 15, scale = 2)
    private BigDecimal salaireBase;

    /**
     * Champ referenceNoteServiceDg : reference note service dg utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "reference_note_service_dg", length = 150)
    private String referenceNoteServiceDg;

    /**
     * Champ dateSignatureDg : date signature dg utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "date_signature_dg")
    private LocalDate dateSignatureDg;

    /**
     * Champ signeParDg : signe par dg utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @Column(name = "signe_par_dg", nullable = false)
    @Builder.Default
    private Boolean signeParDg = false;

    /**
     * Identifiant unique technique (UUID) de l'élément workflowinstance associé.
     */
    @Column(name = "workflow_instance_id")
    private UUID workflowInstanceId;

    /**
     * Identifiant unique technique (UUID) de l'élément makercheckerticket associé.
     */
    @Column(name = "maker_checker_ticket_id")
    private UUID makerCheckerTicketId;

    /**
     * Champ uniteOrganisationnelle : unite organisationnelle utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Champ poste : poste utilisé pour le pilotage et le suivi dans ContratTravailEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private PosteEntity poste;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
