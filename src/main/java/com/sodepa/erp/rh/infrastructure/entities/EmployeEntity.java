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
/**
 * Entité de persistance représentant `EmployeEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeEntity implements Persistable<UUID> {

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
     * Matricule officiel de l'employé SODEPA dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    /**
     * Nom officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 100)
    private String nom;

    /**
     * Prénom de la personne dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 100)
    private String prenom;

    /**
     * Sexe de l'animal (Mâle, Femelle) dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private Sexe sexe;

    /**
     * Champ dateNaissance : date naissance utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    /**
     * Champ lieuNaissance : lieu naissance utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "lieu_naissance", length = 100)
    private String lieuNaissance;

    /**
     * Champ situationMatrimoniale : situation matrimoniale utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "situation_matrimoniale", nullable = false, length = 20)
    @Builder.Default
    private SituationMatrimoniale situationMatrimoniale = SituationMatrimoniale.CELIBATAIRE;

    /**
     * Champ nombreEnfantsACharge : nombre enfants a charge utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "nombre_enfants_a_charge", nullable = false)
    @Builder.Default
    private Integer nombreEnfantsACharge = 0;

    /**
     * Adresse électronique professionnelle de contact dans le cadre des opérations SODEPA.
     */
    @Column(length = 150)
    private String email;

    /**
     * Numéro de téléphone de contact dans le cadre des opérations SODEPA.
     */
    @Column(length = 50)
    private String telephone;

    /**
     * Adresse géographique ou postale dans le cadre des opérations SODEPA.
     */
    @Column(length = 255)
    private String adresse;

    /**
     * Champ numeroCnps : numero cnps utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "numero_cnps", length = 50)
    private String numeroCnps;

    /**
     * Champ compteBancaireRib : compte bancaire rib utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "compte_bancaire_rib", length = 50)
    private String compteBancaireRib;

    /**
     * Nom ou désignation textuelle de banque pour l'affichage et la traçabilité.
     */
    @Column(name = "banque_nom", length = 100)
    private String banqueNom;

    /**
     * Champ typeContrat : type contrat utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 30)
    @Builder.Default
    private TypeContrat typeContrat = TypeContrat.CDI;

    /**
     * Champ dateEmbauche : date embauche utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    /**
     * Champ dateFinEssai : date fin essai utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "date_fin_essai")
    private LocalDate dateFinEssai;

    /**
     * Champ dateFinContrat : date fin contrat utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    /**
     * Champ statutActivite : statut activite utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_activite", nullable = false, length = 30)
    @Builder.Default
    private StatutActivite statutActivite = StatutActivite.EN_FONCTION;

    /**
     * Champ uniteOrganisationnelle : unite organisationnelle utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_organisationnelle_id")
    private UniteOrganisationnelleEntity uniteOrganisationnelle;

    /**
     * Champ poste : poste utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private PosteEntity poste;

    /**
     * Champ grilleSalariale : grille salariale utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grille_salariale_id")
    private GrilleSalarialeEntity grilleSalariale;

    /**
     * Champ utilisateur : utilisateur utilisé pour le pilotage et le suivi dans EmployeEntity.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", unique = true)
    private UtilisateurEntity utilisateur;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
