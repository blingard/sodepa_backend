package com.sodepa.erp.rh.infrastructure.entities;

import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité Employé représentant le dossier administratif d'un agent SODEPA.
 */
@Entity
@Table(name = "rh_employes", indexes = {
    @Index(name = "idx_employe_matricule", columnList = "matricule", unique = true),
    @Index(name = "idx_employe_poste", columnList = "poste_id"),
    @Index(name = "idx_employe_unite", columnList = "unite_organisationnelle_id"),
    @Index(name = "idx_employe_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeEntity implements Persistable<UUID> {

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

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Sexe sexe;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance", length = 100)
    private String lieuNaissance;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation_matrimoniale", nullable = false, length = 20)
    @Builder.Default
    private SituationMatrimoniale situationMatrimoniale = SituationMatrimoniale.CELIBATAIRE;

    @Column(name = "nombre_enfants_a_charge", nullable = false)
    @Builder.Default
    private Integer nombreEnfantsACharge = 0;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String telephone;

    @Column(length = 255)
    private String adresse;

    @Column(name = "numero_cnps", length = 50)
    private String numeroCnps;

    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 30)
    @Builder.Default
    private TypeContrat typeContrat = TypeContrat.CDI;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Column(name = "date_fin_essai")
    private LocalDate dateFinEssai;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_activite", nullable = false, length = 30)
    @Builder.Default
    private StatutActivite statutActivite = StatutActivite.EN_FONCTION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private PosteEntity poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grille_salariale_id")
    private GrilleSalarialeEntity grilleSalariale;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true)
    private UtilisateurEntity utilisateur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
