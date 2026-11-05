package com.sodepa.erp.rh.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.organigramme.infrastructure.entities.DerogationPermissionEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDerogation;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.DerogationPermissionRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.organigramme.presentation.requests.AccorderDerogationRequest;
import com.sodepa.erp.organigramme.presentation.responses.DerogationPermissionResponse;
import com.sodepa.erp.rh.infrastructure.entities.*;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository;
import com.sodepa.erp.rh.presentation.requests.CreerEmployeRequest;
import com.sodepa.erp.rh.presentation.requests.ModifierEmployeRequest;
import com.sodepa.erp.rh.presentation.responses.EmployeResponse;
import com.sodepa.erp.rh.presentation.responses.GrilleSalarialeResponse;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RhService {

    private final EmployeRepository employeRepository;
    private final GrilleSalarialeRepository grilleSalarialeRepository;
    private final UniteOrganisationnelleRepository uniteRepository;
    private final PosteRepository posteRepository;
    private final UserRepository userRepository;
    private final DerogationPermissionRepository derogationPermissionRepository;
    private final ClickHouseManager clickHouseManager;
    private final UtilsService utilsService;
    private final ObjectMapper objectMapper;
    private final MakerCheckerEnginePort makerCheckerEngine;

    public boolean canValidate(Permissions permission) {
        try {
            utilsService.hasPermission(permission);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<GrilleSalarialeResponse> getGrilleSalariale() {
        return grilleSalarialeRepository.findByOrderByCategorieAscEchelonAsc().stream()
                .map(this::mapToGrilleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeResponse> listerEmployes(UUID uniteId, StatutActivite statut, String recherche) {
        return employeRepository.findAll().stream()
                .filter(e -> uniteId == null || (e.getUniteOrganisationnelle() != null && e.getUniteOrganisationnelle().getId().equals(uniteId)))
                .filter(e -> statut == null || e.getStatutActivite() == statut)
                .filter(e -> {
                    if (recherche == null || recherche.isBlank()) return true;
                    String q = recherche.toLowerCase().trim();
                    return e.getNom().toLowerCase().contains(q) ||
                           e.getPrenom().toLowerCase().contains(q) ||
                           e.getMatricule().toLowerCase().contains(q);
                })
                .map(this::mapToEmployeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeResponse getEmployeById(UUID id) {
        EmployeEntity e = employeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + id));
        return mapToEmployeResponse(e);
    }

    @Transactional(readOnly = true)
    public EmployeResponse getEmployeByMatricule(String matricule) {
        EmployeEntity e = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec le matricule: " + matricule));
        return mapToEmployeResponse(e);
    }

    @Transactional
    public EmployeResponse creerEmploye(CreerEmployeRequest req) {
        utilsService.hasPermission(Permissions.CREATE_EMPLOYE);

        // Règle fondamentale : Tout employé est obligatoirement lié à un compte utilisateur existant.
        UtilisateurEntity utilisateur = null;
        if (req.getUtilisateurId() != null) {
            utilisateur = userRepository.findById(req.getUtilisateurId())
                    .orElseThrow(() -> new IllegalArgumentException("Compte utilisateur introuvable avec l'ID: " + req.getUtilisateurId()));
        } else if (req.getEmail() != null && !req.getEmail().isBlank()) {
            utilisateur = userRepository.findByEmail(req.getEmail().trim()).orElse(null);
        }

        if (utilisateur == null) {
            throw new IllegalArgumentException("Un compte utilisateur existant est obligatoire pour créer un dossier employé (tout employé est obligatoirement un utilisateur SODEPA).");
        }

        // Contrainte absolue : admin_maker et admin_checker ne sont JAMAIS des employés
        if ("admin_maker".equalsIgnoreCase(utilisateur.getUsername()) || "admin_checker".equalsIgnoreCase(utilisateur.getUsername())) {
            throw new IllegalArgumentException("Les comptes racines d'initialisation système ('admin_maker', 'admin_checker') ne peuvent pas être des employés SODEPA.");
        }

        // Vérification de l'unicité de la relation 1-to-1
        if (employeRepository.existsByUtilisateurId(utilisateur.getId())) {
            throw new IllegalArgumentException("L'utilisateur " + utilisateur.getUsername() + " possède déjà un dossier administratif employé actif.");
        }

        // Résolution de la grille indiciaire
        GrilleSalarialeEntity grille = grilleSalarialeRepository
                .findByCategorieAndEchelon(req.getCategorie(), req.getEchelon().toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Échelon " + req.getEchelon() + " invalide pour la catégorie " + req.getCategorie()));

        // Résolution unité
        UniteOrganisationnelleEntity unite = null;
        if (req.getUniteOrganisationnelleId() != null) {
            unite = uniteRepository.findById(req.getUniteOrganisationnelleId())
                    .orElseThrow(() -> new IllegalArgumentException("Unité organisationnelle introuvable"));
        }

        // Résolution poste
        PosteEntity poste = null;
        if (req.getPosteId() != null) {
            poste = posteRepository.findById(req.getPosteId())
                    .orElseThrow(() -> new IllegalArgumentException("Poste introuvable"));
        }

        // Génération automatique du matricule SODEPA (ex: SDP-2026-0001)
        long count = employeRepository.count() + 1;
        String matricule = String.format("SDP-%d-%04d", LocalDate.now().getYear(), count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new java.util.HashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());
        payload.put("matricule", matricule);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.EMPLOYE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Création employé soumise au Maker-Checker: {} (Ticket: {})", matricule, uuid);

        // Traçabilité ClickHouse
        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_EMPLOYE",
                "matricule", matricule,
                "nom", req.getNom(),
                "prenom", req.getPrenom(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_EMPLOYE)) {
            log.info("Utilisateur habilité, validation automatique de l'employé {}", matricule);
            try {
                validateOrRejectEmploye(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique de l'employé {}: {}", matricule, e.getMessage());
            }
        }

        final UniteOrganisationnelleEntity finalUnite = unite;
        final PosteEntity finalPoste = poste;
        final GrilleSalarialeEntity finalGrille = grille;
        final UtilisateurEntity finalUtilisateur = utilisateur;

        return employeRepository.findById(id)
                .map(this::mapToEmployeResponse)
                .orElseGet(() -> EmployeResponse.builder()
                        .id(id)
                        .matricule(matricule)
                        .nom(req.getNom())
                        .prenom(req.getPrenom())
                        .nomComplet(req.getPrenom() + " " + req.getNom())
                        .sexe(req.getSexe())
                        .dateNaissance(req.getDateNaissance())
                        .lieuNaissance(req.getLieuNaissance())
                        .situationMatrimoniale(req.getSituationMatrimoniale())
                        .nombreEnfantsACharge(req.getNombreEnfantsACharge() != null ? req.getNombreEnfantsACharge() : 0)
                        .email(req.getEmail() != null ? req.getEmail() : finalUtilisateur.getEmail())
                        .telephone(req.getTelephone())
                        .adresse(req.getAdresse())
                        .numeroCnps(req.getNumeroCnps())
                        .compteBancaireRib(req.getCompteBancaireRib())
                        .banqueNom(req.getBanqueNom())
                        .typeContrat(req.getTypeContrat())
                        .dateEmbauche(req.getDateEmbauche())
                        .dateFinEssai(req.getDateFinEssai())
                        .dateFinContrat(req.getDateFinContrat())
                        .statutActivite(StatutActivite.EN_FONCTION)
                        .uniteId(finalUnite != null ? finalUnite.getId() : null)
                        .uniteCode(finalUnite != null ? finalUnite.getCode() : null)
                        .uniteIntitule(finalUnite != null ? finalUnite.getIntitule() : null)
                        .posteId(finalPoste != null ? finalPoste.getId() : null)
                        .posteCode(finalPoste != null ? finalPoste.getCodePoste() : null)
                        .posteIntitule(finalPoste != null ? finalPoste.getIntitule() : null)
                        .grilleId(finalGrille != null ? finalGrille.getId() : null)
                        .categorie(finalGrille != null ? finalGrille.getCategorie() : null)
                        .echelon(finalGrille != null ? finalGrille.getEchelon() : null)
                        .groupe(finalGrille != null ? finalGrille.getGroupe() : null)
                        .salaireBase(finalGrille != null ? finalGrille.getSalaireBase() : null)
                        .tauxHoraire(finalGrille != null ? finalGrille.getTauxHoraire() : null)
                        .utilisateurId(finalUtilisateur.getId())
                        .username(finalUtilisateur.getUsername())
                        .userId(finalUtilisateur.getId().toString())
                        .build());
    }

    @Transactional
    public EmployeResponse modifierEmploye(UUID id, ModifierEmployeRequest req) {
        utilsService.hasPermission(Permissions.UPDATE_EMPLOYE);
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + id));

        Map<String, Object> payload = new java.util.HashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.EMPLOYE,
                id.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Modification employé soumise au Maker-Checker: {} (Ticket: {})", entity.getMatricule(), uuid);

        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_MODIFIER_EMPLOYE",
                "matricule", entity.getMatricule(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_EMPLOYE)) {
            try {
                validateOrRejectEmploye(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique de la modification par l'utilisateur",
                        MakerCheckerOperationType.UPDATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique de la modification employé {}: {}", id, e.getMessage());
            }
        }

        return mapToEmployeResponse(employeRepository.findById(id).orElse(entity));
    }

    @Transactional
    public void supprimerEmploye(UUID id) {
        utilsService.hasPermission(Permissions.DELETE_EMPLOYE);
        EmployeEntity entity = employeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + id));

        Map<String, Object> payload = Map.of(
                "id", id.toString(),
                "matricule", entity.getMatricule()
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.EMPLOYE,
                id.toString(),
                payload,
                MakerCheckerOperationType.DELETE
        );
        log.info("Suppression employé soumise au Maker-Checker: {} (Ticket: {})", entity.getMatricule(), uuid);

        trackClickHouse(MakerCheckerEntityName.EMPLOYE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_SUPPRIMER_EMPLOYE",
                "matricule", entity.getMatricule(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_EMPLOYE)) {
            try {
                validateOrRejectEmploye(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique de la suppression par l'utilisateur",
                        MakerCheckerOperationType.DELETE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique de la suppression employé {}: {}", id, e.getMessage());
            }
        }
    }

    @Transactional
    public void validateOrRejectEmploye(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_EMPLOYE);
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        makerCheckerEngine.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            makerCheckerEngine.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Opération Employé rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID employeId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationEmploye(checkerOutput.payload(), employeId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                updateEmploye(checkerOutput.payload(), employeId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.DELETE) {
                deleteEmploye(employeId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Employé approuvée par le Checker {}. Ticket: {}, Employé PK: {}", checkerId, checkerOutput.id(), employeId);
        }
    }

    private EmployeEntity creationEmploye(Map<String, Object> payload, UUID employeId) {
        CreerEmployeRequest req = objectMapper.convertValue(payload, CreerEmployeRequest.class);
        String matricule = (String) payload.get("matricule");
        if (matricule == null) {
            matricule = String.format("SDP-%d-%04d", LocalDate.now().getYear(), employeRepository.count() + 1);
        }

        UtilisateurEntity utilisateur = null;
        if (req.getUtilisateurId() != null) {
            utilisateur = userRepository.findById(req.getUtilisateurId()).orElse(null);
        } else if (req.getEmail() != null && !req.getEmail().isBlank()) {
            utilisateur = userRepository.findByEmail(req.getEmail().trim()).orElse(null);
        }

        GrilleSalarialeEntity grille = grilleSalarialeRepository
                .findByCategorieAndEchelon(req.getCategorie(), req.getEchelon().toUpperCase().trim())
                .orElse(null);

        UniteOrganisationnelleEntity unite = null;
        if (req.getUniteOrganisationnelleId() != null) {
            unite = uniteRepository.findById(req.getUniteOrganisationnelleId()).orElse(null);
        }

        PosteEntity poste = null;
        if (req.getPosteId() != null) {
            poste = posteRepository.findById(req.getPosteId()).orElse(null);
        }

        EmployeEntity entity = EmployeEntity.builder()
                .id(employeId)
                .matricule(matricule)
                .nom(req.getNom() != null ? req.getNom().trim() : "")
                .prenom(req.getPrenom() != null ? req.getPrenom().trim() : "")
                .sexe(req.getSexe())
                .dateNaissance(req.getDateNaissance())
                .lieuNaissance(req.getLieuNaissance())
                .situationMatrimoniale(req.getSituationMatrimoniale())
                .nombreEnfantsACharge(req.getNombreEnfantsACharge() != null ? req.getNombreEnfantsACharge() : 0)
                .email(req.getEmail() != null ? req.getEmail() : (utilisateur != null ? utilisateur.getEmail() : null))
                .telephone(req.getTelephone())
                .adresse(req.getAdresse())
                .numeroCnps(req.getNumeroCnps())
                .compteBancaireRib(req.getCompteBancaireRib())
                .banqueNom(req.getBanqueNom())
                .typeContrat(req.getTypeContrat())
                .dateEmbauche(req.getDateEmbauche())
                .dateFinEssai(req.getDateFinEssai())
                .dateFinContrat(req.getDateFinContrat())
                .statutActivite(StatutActivite.EN_FONCTION)
                .uniteOrganisationnelle(unite)
                .poste(poste)
                .grilleSalariale(grille)
                .utilisateur(utilisateur)
                .build();

        return employeRepository.save(entity);
    }

    private EmployeEntity updateEmploye(Map<String, Object> payload, UUID employeId) {
        ModifierEmployeRequest req = objectMapper.convertValue(payload, ModifierEmployeRequest.class);
        EmployeEntity entity = employeRepository.findById(employeId)
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable avec l'ID: " + employeId));

        if (req.getNom() != null && !req.getNom().isBlank()) entity.setNom(req.getNom().trim());
        if (req.getPrenom() != null && !req.getPrenom().isBlank()) entity.setPrenom(req.getPrenom().trim());
        if (req.getSituationMatrimoniale() != null) entity.setSituationMatrimoniale(req.getSituationMatrimoniale());
        if (req.getNombreEnfantsACharge() != null) entity.setNombreEnfantsACharge(req.getNombreEnfantsACharge());
        if (req.getEmail() != null) entity.setEmail(req.getEmail());
        if (req.getTelephone() != null) entity.setTelephone(req.getTelephone());
        if (req.getAdresse() != null) entity.setAdresse(req.getAdresse());
        if (req.getNumeroCnps() != null) entity.setNumeroCnps(req.getNumeroCnps());
        if (req.getCompteBancaireRib() != null) entity.setCompteBancaireRib(req.getCompteBancaireRib());
        if (req.getBanqueNom() != null) entity.setBanqueNom(req.getBanqueNom());
        if (req.getTypeContrat() != null) entity.setTypeContrat(req.getTypeContrat());
        if (req.getStatutActivite() != null) entity.setStatutActivite(req.getStatutActivite());
        if (req.getDateFinEssai() != null) entity.setDateFinEssai(req.getDateFinEssai());
        if (req.getDateFinContrat() != null) entity.setDateFinContrat(req.getDateFinContrat());

        if (req.getUniteOrganisationnelleId() != null) {
            UniteOrganisationnelleEntity u = uniteRepository.findById(req.getUniteOrganisationnelleId())
                    .orElseThrow(() -> new IllegalArgumentException("Unité organisationnelle introuvable"));
            entity.setUniteOrganisationnelle(u);
        }

        if (req.getPosteId() != null) {
            PosteEntity p = posteRepository.findById(req.getPosteId())
                    .orElseThrow(() -> new IllegalArgumentException("Poste introuvable"));
            entity.setPoste(p);
        }

        if (req.getCategorie() != null && req.getEchelon() != null) {
            GrilleSalarialeEntity g = grilleSalarialeRepository
                    .findByCategorieAndEchelon(req.getCategorie(), req.getEchelon().toUpperCase().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Échelon invalide"));
            entity.setGrilleSalariale(g);
        }

        return employeRepository.save(entity);
    }

    private void deleteEmploye(UUID employeId) {
        employeRepository.findById(employeId).ifPresent(employeRepository::delete);
    }

    // ==========================================
    // PERMISSIONS DÉROGÉES (DG / DIRECTION)
    // ==========================================

    @Transactional
    public DerogationPermissionResponse accorderDerogation(AccorderDerogationRequest req) {
        utilsService.hasPermission(Permissions.CREATE_DEROGATION);

        if (req.getDateFin().isBefore(req.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }

        UtilisateurEntity beneficiaire = userRepository.findById(req.getUtilisateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur bénéficiaire introuvable: " + req.getUtilisateurId()));

        if ("admin_maker".equalsIgnoreCase(beneficiaire.getUsername()) || "admin_checker".equalsIgnoreCase(beneficiaire.getUsername())) {
            throw new IllegalArgumentException("Les comptes racines d'initialisation système détiennent déjà tous les privilèges sans dérogation.");
        }

        String accordeParId = null;
        try {
            accordeParId = utilsService.getCurrentUserData().userId();
        } catch (Exception ignored) {
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new java.util.HashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());
        payload.put("accordeParId", accordeParId);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.DEROGATION_PERMISSION,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Octroi de dérogation soumis au Maker-Checker pour {} (Ticket: {})", beneficiaire.getUsername(), uuid);

        trackClickHouse(MakerCheckerEntityName.DEROGATION_PERMISSION.name(), id.toString(), Map.of(
                "action", "SOUMISSION_ACCORDER_DEROGATION",
                "beneficiaire", beneficiaire.getUsername(),
                "permission", req.getPermission().name(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_DEROGATION)) {
            try {
                validateOrRejectDerogation(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur accordeur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto dérogation: {}", e.getMessage());
            }
        }

        return derogationPermissionRepository.findById(id)
                .map(this::mapToDerogationResponse)
                .orElseGet(() -> DerogationPermissionResponse.builder()
                        .id(id)
                        .utilisateurId(beneficiaire.getId())
                        .username(beneficiaire.getUsername())
                        .nomComplet(beneficiaire.getNom() + " " + beneficiaire.getPrenom())
                        .permission(req.getPermission())
                        .dateDebut(req.getDateDebut())
                        .dateFin(req.getDateFin())
                        .statut(StatutDerogation.ACTIVE)
                        .motif(req.getMotif())
                        .referenceDecision(req.getReferenceDecision())
                        .accordeParNom("Direction Générale")
                        .dateCreation(Instant.now())
                        .build());
    }

    @Transactional
    public DerogationPermissionResponse revoquerDerogation(UUID derogationId) {
        utilsService.hasPermission(Permissions.UPDATE_DEROGATION);
        DerogationPermissionEntity der = derogationPermissionRepository.findById(derogationId)
                .orElseThrow(() -> new IllegalArgumentException("Dérogation introuvable avec l'ID: " + derogationId));

        Map<String, Object> payload = Map.of(
                "id", derogationId.toString(),
                "action", "REVOQUER"
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.DEROGATION_PERMISSION,
                derogationId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Révocation dérogation soumise au Maker-Checker pour {} (Ticket: {})", der.getUtilisateur().getUsername(), uuid);

        trackClickHouse(MakerCheckerEntityName.DEROGATION_PERMISSION.name(), derogationId.toString(), Map.of(
                "action", "SOUMISSION_REVOQUER_DEROGATION",
                "beneficiaire", der.getUtilisateur().getUsername(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_DEROGATION)) {
            try {
                validateOrRejectDerogation(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique de la révocation",
                        MakerCheckerOperationType.UPDATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto révocation dérogation: {}", e.getMessage());
            }
        }

        return mapToDerogationResponse(derogationPermissionRepository.findById(derogationId).orElse(der));
    }

    @Transactional
    public void validateOrRejectDerogation(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_DEROGATION);
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(utilsService.getCurrentUserData().userId());
        if (checkerOutput.maker() != null && checkerOutput.maker().id() != null && checkerOutput.maker().id().equals(checkerId)) {
            throw new RuntimeException("A checker cannot vote on their own request.");
        }

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }

        makerCheckerEngine.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (MakerCheckerStatus.REJECTED.equals(newStatus)) {
            makerCheckerEngine.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
            log.info("Opération Dérogation rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID derogationId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationDerogation(checkerOutput.payload(), derogationId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                updateDerogation(checkerOutput.payload(), derogationId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Dérogation approuvée par le Checker {}. Ticket: {}, Dérogation PK: {}", checkerId, checkerOutput.id(), derogationId);
        }
    }

    private DerogationPermissionEntity creationDerogation(Map<String, Object> payload, UUID derogationId) {
        AccorderDerogationRequest req = objectMapper.convertValue(payload, AccorderDerogationRequest.class);
        UtilisateurEntity beneficiaire = userRepository.findById(req.getUtilisateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur bénéficiaire introuvable: " + req.getUtilisateurId()));

        String accordeParId = (String) payload.get("accordeParId");
        UtilisateurEntity accordeur = null;
        if (accordeParId != null) {
            accordeur = userRepository.findById(UUID.fromString(accordeParId)).orElse(null);
        }

        DerogationPermissionEntity derogation = DerogationPermissionEntity.builder()
                .id(derogationId)
                .utilisateur(beneficiaire)
                .permission(req.getPermission())
                .dateDebut(req.getDateDebut())
                .dateFin(req.getDateFin())
                .statut(StatutDerogation.ACTIVE)
                .motif(req.getMotif())
                .referenceDecision(req.getReferenceDecision())
                .accordePar(accordeur)
                .dateCreation(Instant.now())
                .build();

        return derogationPermissionRepository.save(derogation);
    }

    private DerogationPermissionEntity updateDerogation(Map<String, Object> payload, UUID derogationId) {
        DerogationPermissionEntity der = derogationPermissionRepository.findById(derogationId)
                .orElseThrow(() -> new IllegalArgumentException("Dérogation introuvable avec l'ID: " + derogationId));
        der.setStatut(StatutDerogation.REVOQUEE);
        return derogationPermissionRepository.save(der);
    }

    @Transactional(readOnly = true)
    public List<DerogationPermissionResponse> listerDerogations(UUID utilisateurId) {
        List<DerogationPermissionEntity> list = (utilisateurId != null)
                ? derogationPermissionRepository.findByUtilisateurId(utilisateurId)
                : derogationPermissionRepository.findAll();
        return list.stream().map(this::mapToDerogationResponse).collect(Collectors.toList());
    }

    private void trackClickHouse(String entityName, String entityPk, Object payloadObj) {
        try {
            String actorId = "SYSTEM";
            try {
                actorId = utilsService.getCurrentUserData().userId();
            } catch (Exception ignored) {
            }

            String payloadStr = objectMapper.writeValueAsString(payloadObj);
            clickHouseManager.saveMakerChecker(
                    MakerCheckerMessageInput.builder()
                            .id(UUID.randomUUID())
                            .entityName(entityName)
                            .entityPk(entityPk)
                            .payload(payloadStr)
                            .timestamp(LocalDateTime.now())
                            .maker_id(actorId)
                            .checker_id(actorId)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Journalisation ClickHouse non bloquante ignorée pour {} ({}): {}", entityName, entityPk, e.getMessage());
        }
    }

    private EmployeResponse mapToEmployeResponse(EmployeEntity e) {
        EmployeResponse.EmployeResponseBuilder builder = EmployeResponse.builder()
                .id(e.getId())
                .matricule(e.getMatricule())
                .nom(e.getNom())
                .prenom(e.getPrenom())
                .nomComplet(e.getPrenom() + " " + e.getNom())
                .sexe(e.getSexe())
                .dateNaissance(e.getDateNaissance())
                .lieuNaissance(e.getLieuNaissance())
                .situationMatrimoniale(e.getSituationMatrimoniale())
                .nombreEnfantsACharge(e.getNombreEnfantsACharge())
                .email(e.getEmail())
                .telephone(e.getTelephone())
                .adresse(e.getAdresse())
                .numeroCnps(e.getNumeroCnps())
                .compteBancaireRib(e.getCompteBancaireRib())
                .banqueNom(e.getBanqueNom())
                .typeContrat(e.getTypeContrat())
                .dateEmbauche(e.getDateEmbauche())
                .dateFinEssai(e.getDateFinEssai())
                .dateFinContrat(e.getDateFinContrat())
                .statutActivite(e.getStatutActivite())
                .utilisateurId(e.getUtilisateur() != null ? e.getUtilisateur().getId() : null)
                .username(e.getUtilisateur() != null ? e.getUtilisateur().getUsername() : null)
                .userId(e.getUtilisateur() != null ? e.getUtilisateur().getId().toString() : null)
                .dateCreation(e.getDateCreation());

        if (e.getUniteOrganisationnelle() != null) {
            builder.uniteId(e.getUniteOrganisationnelle().getId())
                   .uniteCode(e.getUniteOrganisationnelle().getCode())
                   .uniteIntitule(e.getUniteOrganisationnelle().getIntitule());
        }

        if (e.getPoste() != null) {
            builder.posteId(e.getPoste().getId())
                   .posteCode(e.getPoste().getCodePoste())
                   .posteIntitule(e.getPoste().getIntitule());
        }

        if (e.getGrilleSalariale() != null) {
            builder.grilleId(e.getGrilleSalariale().getId())
                   .categorie(e.getGrilleSalariale().getCategorie())
                   .echelon(e.getGrilleSalariale().getEchelon())
                   .groupe(e.getGrilleSalariale().getGroupe())
                   .salaireBase(e.getGrilleSalariale().getSalaireBase())
                   .tauxHoraire(e.getGrilleSalariale().getTauxHoraire());
        }

        return builder.build();
    }

    private DerogationPermissionResponse mapToDerogationResponse(DerogationPermissionEntity d) {
        return DerogationPermissionResponse.builder()
                .id(d.getId())
                .utilisateurId(d.getUtilisateur().getId())
                .username(d.getUtilisateur().getUsername())
                .nomComplet(d.getUtilisateur().getNom() + " " + d.getUtilisateur().getPrenom())
                .permission(d.getPermission())
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .statut(d.getStatut())
                .motif(d.getMotif())
                .referenceDecision(d.getReferenceDecision())
                .accordeParNom(d.getAccordePar() != null ? d.getAccordePar().getNom() + " " + d.getAccordePar().getPrenom() : "Direction Générale")
                .dateCreation(d.getDateCreation())
                .build();
    }

    private GrilleSalarialeResponse mapToGrilleResponse(GrilleSalarialeEntity g) {
        return GrilleSalarialeResponse.builder()
                .id(g.getId())
                .categorie(g.getCategorie())
                .echelon(g.getEchelon())
                .groupe(g.getGroupe())
                .salaireBase(g.getSalaireBase())
                .tauxHoraire(g.getTauxHoraire())
                .actif(g.getActif())
                .build();
    }
}
