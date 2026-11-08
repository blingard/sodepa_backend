package com.sodepa.erp.organigramme.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.organigramme.application.exceptions.BudgetedHeadcountExceededException;
import com.sodepa.erp.organigramme.application.exceptions.ConflictAffectationException;
import com.sodepa.erp.organigramme.application.exceptions.CyclicDependencyException;
import com.sodepa.erp.organigramme.application.exceptions.OrganigrammeNotFoundException;
import com.sodepa.erp.organigramme.application.exceptions.TypeStructureIncompatibleException;
import com.sodepa.erp.organigramme.application.inputs.*;
import com.sodepa.erp.organigramme.application.outputs.*;
import com.sodepa.erp.organigramme.application.ports.HierarchieResolutionPort;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.organigramme.infrastructure.entities.*;
import com.sodepa.erp.organigramme.infrastructure.repo.*;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Composant applicatif `OrganigrammeAdapter` du système ERP SODEPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganigrammeAdapter implements OrganigrammePort, HierarchieResolutionPort {

    private final UniteOrganisationnelleRepository uniteRepository;
    private final PosteRepository posteRepository;
    private final AffectationPosteRepository affectationRepository;
    private final OrganigrammeVersionRepository versionRepository;
    private final DelegationPouvoirRepository delegationRepository;
    private final UserRepository userRepository;
    private final TypeStructureRepository typeStructureRepository;
    private final ObjectifUniteRepository objectifRepository;
    private final EmployeRepository employeRepository;
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final UtilsService utilsService;
    private final ObjectMapper objectMapper;
    private final ClickHouseManager clickHouseManager;

    public boolean canValidate(Permissions permission) {
        try {
            utilsService.hasPermission(permission);
            return true;
        } catch (Exception e) {
            return false;
        }
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

    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public UniteOrganisationnelleOutput creerUnite(CreerUniteInput input) {
        utilsService.hasPermission(Permissions.CREATE_UNITE);
        if (uniteRepository.existsByCode(input.code())) {
            throw new IllegalArgumentException("Une unité organisationnelle avec le code " + input.code() + " existe déjà.");
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.UNITE_ORGANISATIONNELLE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Création unité soumise au Maker-Checker: {} (Ticket: {})", input.code(), uuid);

        trackClickHouse(MakerCheckerEntityName.UNITE_ORGANISATIONNELLE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_UNITE",
                "code", input.code(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_UNITE)) {
            try {
                validateOrRejectUnite(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto unité {}: {}", input.code(), e.getMessage());
            }
        }

        return uniteRepository.findById(id)
                .map(this::mapToUniteOutput)
                .orElseGet(() -> UniteOrganisationnelleOutput.builder()
                        .id(id)
                        .code(input.code())
                        .intitule(input.intitule())
                        .typeUnite(input.typeUnite())
                        .localisation(input.localisation())
                        .statut(StatutUnite.ACTIF)
                        .dateCreation(input.dateCreation() != null ? input.dateCreation() : LocalDate.now())
                        .build());
    }

    private UniteOrganisationnelleEntity creationUnite(Map<String, Object> payload, UUID uniteId) {
        CreerUniteInput input = objectMapper.convertValue(payload, CreerUniteInput.class);
        UniteOrganisationnelleEntity parent = null;
        String materializedPath;

        if (input.parentId() != null) {
            parent = uniteRepository.findById(input.parentId())
                    .orElseThrow(() -> new OrganigrammeNotFoundException("Unité parente introuvable : " + input.parentId()));
            materializedPath = parent.getMaterializedPath() + input.code() + "/";
        } else {
            materializedPath = "/" + input.code() + "/";
        }

        TypeStructureEntity typeStructure = null;
        if (input.typeStructureId() != null) {
            typeStructure = typeStructureRepository.findById(input.typeStructureId())
                    .orElseThrow(() -> new OrganigrammeNotFoundException("Type de structure introuvable : " + input.typeStructureId()));
        }

        UniteOrganisationnelleEntity unite = UniteOrganisationnelleEntity.builder()
                .id(uniteId)
                .code(input.code())
                .intitule(input.intitule())
                .typeUnite(input.typeUnite())
                .typeStructure(typeStructure)
                .parent(parent)
                .materializedPath(materializedPath)
                .localisation(input.localisation())
                .statut(StatutUnite.ACTIF)
                .dateCreation(input.dateCreation() != null ? input.dateCreation() : LocalDate.now())
                .build();

        return uniteRepository.save(unite);
    }

    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public UniteOrganisationnelleOutput deplacerUnite(DeplacerUniteInput input) {
        utilsService.hasPermission(Permissions.UPDATE_UNITE);
        UniteOrganisationnelleEntity unite = uniteRepository.findById(input.uniteId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Unité introuvable : " + input.uniteId()));

        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.UNITE_ORGANISATIONNELLE,
                input.uniteId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Déplacement unité soumis au Maker-Checker: {} (Ticket: {})", unite.getCode(), uuid);

        trackClickHouse(MakerCheckerEntityName.UNITE_ORGANISATIONNELLE.name(), input.uniteId().toString(), Map.of(
                "action", "SOUMISSION_DEPLACER_UNITE",
                "code", unite.getCode(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_UNITE)) {
            try {
                validateOrRejectUnite(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.UPDATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto déplacement unité {}: {}", input.uniteId(), e.getMessage());
            }
        }

        return mapToUniteOutput(uniteRepository.findById(input.uniteId()).orElse(unite));
    }

    private UniteOrganisationnelleEntity applyDeplacerUnite(Map<String, Object> payload, UUID uniteId) {
        DeplacerUniteInput input = objectMapper.convertValue(payload, DeplacerUniteInput.class);
        UniteOrganisationnelleEntity unite = uniteRepository.findById(uniteId)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Unité introuvable : " + uniteId));

        UniteOrganisationnelleEntity nouveauParent = null;
        String newPrefix;

        if (input.nouveauParentId() != null) {
            nouveauParent = uniteRepository.findById(input.nouveauParentId())
                    .orElseThrow(() -> new OrganigrammeNotFoundException("Nouveau parent introuvable : " + input.nouveauParentId()));

            if (nouveauParent.getMaterializedPath().startsWith(unite.getMaterializedPath())) {
                throw new CyclicDependencyException("Dépendance cyclique détectée : le nouveau parent " +
                        nouveauParent.getCode() + " est un descendant de " + unite.getCode());
            }
            newPrefix = nouveauParent.getMaterializedPath() + unite.getCode() + "/";
        } else {
            newPrefix = "/" + unite.getCode() + "/";
        }

        String oldPrefix = unite.getMaterializedPath();
        unite.setParent(nouveauParent);
        unite.setMaterializedPath(newPrefix);
        uniteRepository.save(unite);

        List<UniteOrganisationnelleEntity> descendants = uniteRepository.findDescendantsByPathPrefix(oldPrefix);
        for (UniteOrganisationnelleEntity desc : descendants) {
            if (!desc.getId().equals(unite.getId())) {
                String updatedPath = newPrefix + desc.getMaterializedPath().substring(oldPrefix.length());
                desc.setMaterializedPath(updatedPath);
                uniteRepository.save(desc);
            }
        }

        log.info("Unité {} déplacée avec succès. Nouveau path : {}", unite.getCode(), newPrefix);
        return unite;
    }

    @Override
    @Transactional
    public void validateOrRejectUnite(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_UNITE);
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
            log.info("Opération Unité rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID uniteId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationUnite(checkerOutput.payload(), uniteId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                applyDeplacerUnite(checkerOutput.payload(), uniteId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Unité approuvée par Checker {}. Ticket: {}, Unité PK: {}", checkerId, checkerOutput.id(), uniteId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UniteOrganisationnelleOutput> listerUnites() {
        return uniteRepository.findAll().stream()
                .map(this::mapToUniteOutput)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "organigramme-tree")
    public List<ArborescenceNoeudOutput> getArborescenceComplete() {
        List<UniteOrganisationnelleEntity> racines = uniteRepository.findByParentIsNull();
        return racines.stream()
                .map(this::construireSousArbre)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArborescenceNoeudOutput getSousArbreUnite(UUID uniteId) {
        UniteOrganisationnelleEntity unite = uniteRepository.findById(uniteId)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Unité introuvable : " + uniteId));
        return construireSousArbre(unite);
    }

    @Override
    @Transactional
    public PosteOutput creerPoste(CreerPosteInput input) {
        utilsService.hasPermission(Permissions.CREATE_POSTE);
        if (posteRepository.existsByCodePoste(input.codePoste())) {
            throw new IllegalArgumentException("Le poste avec le code " + input.codePoste() + " existe déjà.");
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.POSTE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Création poste soumise au Maker-Checker: {} (Ticket: {})", input.codePoste(), uuid);

        trackClickHouse(MakerCheckerEntityName.POSTE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_POSTE",
                "codePoste", input.codePoste(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_POSTE)) {
            try {
                validateOrRejectPoste(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto poste {}: {}", input.codePoste(), e.getMessage());
            }
        }

        return posteRepository.findById(id)
                .map(this::mapToPosteOutput)
                .orElseGet(() -> PosteOutput.builder()
                        .id(id)
                        .codePoste(input.codePoste())
                        .intitule(input.intitule())
                        .uniteId(input.uniteOrganisationnelleId())
                        .effectifAutorise(input.effectifAutorise() != null ? input.effectifAutorise() : 1)
                        .effectifActuel(0)
                        .categorieConvention(input.categorieConvention())
                        .statut(StatutPoste.OUVERT)
                        .descriptionMission(input.descriptionMission())
                        .permissions(input.permissions() != null ? input.permissions() : Collections.emptySet())
                        .build());
    }

    private PosteEntity creationPoste(Map<String, Object> payload, UUID posteId) {
        CreerPosteInput input = objectMapper.convertValue(payload, CreerPosteInput.class);
        UniteOrganisationnelleEntity unite = uniteRepository.findById(input.uniteOrganisationnelleId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Unité organisationnelle introuvable : " + input.uniteOrganisationnelleId()));

        PosteEntity posteSuperieur = null;
        if (input.posteSuperieurId() != null) {
            posteSuperieur = posteRepository.findById(input.posteSuperieurId())
                    .orElseThrow(() -> new OrganigrammeNotFoundException("Poste supérieur N+1 introuvable : " + input.posteSuperieurId()));
        }

        PosteEntity poste = PosteEntity.builder()
                .id(posteId)
                .codePoste(input.codePoste())
                .intitule(input.intitule())
                .uniteOrganisationnelle(unite)
                .posteSuperieur(posteSuperieur)
                .effectifAutorise(input.effectifAutorise() != null && input.effectifAutorise() > 0 ? input.effectifAutorise() : 1)
                .effectifActuel(0)
                .categorieConvention(input.categorieConvention())
                .statut(StatutPoste.OUVERT)
                .descriptionMission(input.descriptionMission())
                .permissions(input.permissions() != null ? input.permissions() : new java.util.HashSet<>())
                .build();

        return posteRepository.save(poste);
    }

    @Override
    @Transactional
    public void validateOrRejectPoste(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_POSTE);
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
            log.info("Opération Poste rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID posteId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationPoste(checkerOutput.payload(), posteId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Poste approuvée par Checker {}. Ticket: {}, Poste PK: {}", checkerId, checkerOutput.id(), posteId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosteOutput> listerPostes(UUID uniteId) {
        List<PosteEntity> postes = (uniteId != null)
                ? posteRepository.findByUniteOrganisationnelleId(uniteId)
                : posteRepository.findAll();

        return postes.stream().map(this::mapToPosteOutput).toList();
    }

    @Override
    @Transactional
    public AffectationOutput affecterCollaborateur(AffecterCollaborateurInput input) {
        utilsService.hasPermission(Permissions.CREATE_AFFECTATION);
        UtilisateurEntity utilisateur = userRepository.findById(input.utilisateurId())
                .or(() -> employeRepository.findById(input.utilisateurId()).map(EmployeEntity::getUtilisateur))
                .orElseThrow(() -> new OrganigrammeNotFoundException("Utilisateur ou Collaborateur introuvable : " + input.utilisateurId()));

        PosteEntity poste = posteRepository.findById(input.posteId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Poste introuvable : " + input.posteId()));

        if (input.typeAffectation() == TypeAffectation.TITULAIRE) {
            // Vérifier s'il a déjà une affectation titulaire active
            Optional<AffectationPosteEntity> affectationExistante = affectationRepository
                    .findFirstByUtilisateurIdAndTypeAffectationAndStatut(
                            utilisateur.getId(), TypeAffectation.TITULAIRE, StatutAffectation.ACTIF);
            if (affectationExistante.isPresent()) {
                throw new ConflictAffectationException("L'utilisateur " + utilisateur.getUsername() +
                        " possède déjà une affectation TITULAIRE active sur le poste " +
                        affectationExistante.get().getPoste().getCodePoste());
            }

            // Vérifier le plafond d'effectif budgété
            long actifs = affectationRepository.countByPosteIdAndStatut(poste.getId(), StatutAffectation.ACTIF);
            if (actifs >= poste.getEffectifAutorise()) {
                throw new BudgetedHeadcountExceededException("L'effectif budgété autorisé (" +
                        poste.getEffectifAutorise() + ") pour le poste " + poste.getCodePoste() + " est déjà atteint.");
            }
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.AFFECTATION_POSTE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Affectation collaborateur soumise au Maker-Checker pour {} (Ticket: {})", utilisateur.getUsername(), uuid);

        trackClickHouse(MakerCheckerEntityName.AFFECTATION_POSTE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_AFFECTER_COLLABORATEUR",
                "username", utilisateur.getUsername(),
                "poste", poste.getCodePoste(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_AFFECTATION)) {
            try {
                validateOrRejectAffectation(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto affectation: {}", e.getMessage());
            }
        }

        return affectationRepository.findById(id)
                .map(this::mapToAffectationOutput)
                .orElseGet(() -> AffectationOutput.builder()
                        .id(id)
                        .utilisateurId(utilisateur.getId())
                        .utilisateurNom(utilisateur.getNom() + " " + utilisateur.getPrenom())
                        .utilisateurEmail(utilisateur.getEmail())
                        .posteId(poste.getId())
                        .posteCode(poste.getCodePoste())
                        .posteIntitule(poste.getIntitule())
                        .uniteId(poste.getUniteOrganisationnelle().getId())
                        .uniteIntitule(poste.getUniteOrganisationnelle().getIntitule())
                        .typeAffectation(input.typeAffectation())
                        .dateDebut(input.dateDebut() != null ? input.dateDebut() : LocalDate.now())
                        .dateFin(input.dateFin())
                        .referenceDecision(input.referenceDecision())
                        .statut(StatutAffectation.ACTIF)
                        .build());
    }

    private AffectationPosteEntity creationAffectation(Map<String, Object> payload, UUID affectationId) {
        AffecterCollaborateurInput input = objectMapper.convertValue(payload, AffecterCollaborateurInput.class);
        UtilisateurEntity utilisateur = userRepository.findById(input.utilisateurId())
                .or(() -> employeRepository.findById(input.utilisateurId()).map(EmployeEntity::getUtilisateur))
                .orElseThrow(() -> new OrganigrammeNotFoundException("Utilisateur ou Collaborateur introuvable : " + input.utilisateurId()));
        PosteEntity poste = posteRepository.findById(input.posteId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Poste introuvable : " + input.posteId()));

        AffectationPosteEntity affectation = AffectationPosteEntity.builder()
                .id(affectationId)
                .utilisateur(utilisateur)
                .poste(poste)
                .typeAffectation(input.typeAffectation())
                .dateDebut(input.dateDebut() != null ? input.dateDebut() : LocalDate.now())
                .dateFin(input.dateFin())
                .referenceDecision(input.referenceDecision())
                .statut(StatutAffectation.ACTIF)
                .build();

        affectation = affectationRepository.save(affectation);
        poste.setEffectifActuel((int) affectationRepository.countByPosteIdAndStatut(poste.getId(), StatutAffectation.ACTIF));
        posteRepository.save(poste);
        employeRepository.findByUtilisateurId(utilisateur.getId()).ifPresent(emp -> {
            emp.setPoste(poste);
            emp.setUniteOrganisationnelle(poste.getUniteOrganisationnelle());
            employeRepository.save(emp);
        });
        return affectation;
    }

    @Override
    @Transactional
    public AffectationOutput cloturerAffectation(CloturerAffectationInput input) {
        utilsService.hasPermission(Permissions.UPDATE_AFFECTATION);
        AffectationPosteEntity affectation = affectationRepository.findById(input.affectationId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Affectation introuvable : " + input.affectationId()));

        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.AFFECTATION_POSTE,
                input.affectationId().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Clôture affectation soumise au Maker-Checker: {} (Ticket: {})", input.affectationId(), uuid);

        trackClickHouse(MakerCheckerEntityName.AFFECTATION_POSTE.name(), input.affectationId().toString(), Map.of(
                "action", "SOUMISSION_CLOTURER_AFFECTATION",
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_AFFECTATION)) {
            try {
                validateOrRejectAffectation(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique de clôture",
                        MakerCheckerOperationType.UPDATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto clôture affectation: {}", e.getMessage());
            }
        }

        return mapToAffectationOutput(affectationRepository.findById(input.affectationId()).orElse(affectation));
    }

    private AffectationPosteEntity applyCloturerAffectation(Map<String, Object> payload, UUID affectationId) {
        CloturerAffectationInput input = objectMapper.convertValue(payload, CloturerAffectationInput.class);
        AffectationPosteEntity affectation = affectationRepository.findById(affectationId)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Affectation introuvable : " + affectationId));

        affectation.setStatut(StatutAffectation.CLOTURE);
        affectation.setDateFin(input.dateFin() != null ? input.dateFin() : LocalDate.now());
        affectation.setMotifFin(input.motifFin());
        affectationRepository.save(affectation);

        PosteEntity poste = affectation.getPoste();
        poste.setEffectifActuel((int) affectationRepository.countByPosteIdAndStatut(poste.getId(), StatutAffectation.ACTIF));
        posteRepository.save(poste);
        return affectation;
    }

    @Override
    @Transactional
    public void validateOrRejectAffectation(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_AFFECTATION);
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
            log.info("Opération Affectation rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID affectationId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationAffectation(checkerOutput.payload(), affectationId);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                applyCloturerAffectation(checkerOutput.payload(), affectationId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Affectation approuvée par Checker {}. Ticket: {}, Affectation PK: {}", checkerId, checkerOutput.id(), affectationId);
        }
    }

    @Override
    @Transactional
    public DelegationOutput creerDelegation(CreerDelegationInput input) {
        utilsService.hasPermission(Permissions.CREATE_DELEGATION);
        if (input.delegantId().equals(input.delegataireId())) {
            throw new IllegalArgumentException("Le délégant et le délégataire ne peuvent pas être la même personne.");
        }

        UtilisateurEntity delegant = userRepository.findById(input.delegantId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Délégant introuvable : " + input.delegantId()));

        UtilisateurEntity delegataire = userRepository.findById(input.delegataireId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Délégataire introuvable : " + input.delegataireId()));

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(input, Map.class));
        payload.put("id", id.toString());

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.DELEGATION_SIGNATURE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Délégation de signature soumise au Maker-Checker: {} vers {} (Ticket: {})",
                delegant.getUsername(), delegataire.getUsername(), uuid);

        trackClickHouse(MakerCheckerEntityName.DELEGATION_SIGNATURE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_DELEGATION",
                "delegant", delegant.getUsername(),
                "delegataire", delegataire.getUsername(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_DELEGATION)) {
            try {
                validateOrRejectDelegation(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto délégation: {}", e.getMessage());
            }
        }

        return delegationRepository.findById(id)
                .map(this::mapToDelegationOutput)
                .orElseGet(() -> DelegationOutput.builder()
                        .id(id)
                        .delegantId(delegant.getId())
                        .delegantNom(delegant.getNom() + " " + delegant.getPrenom())
                        .delegataireId(delegataire.getId())
                        .delegataireNom(delegataire.getNom() + " " + delegataire.getPrenom())
                        .typeDelegation(input.typeDelegation())
                        .plafondMontant(input.plafondMontant())
                        .dateDebut(input.dateDebut())
                        .dateFin(input.dateFin())
                        .statut(StatutDelegation.ACTIVE)
                        .build());
    }

    private DelegationPouvoirEntity creationDelegation(Map<String, Object> payload, UUID delegationId) {
        CreerDelegationInput input = objectMapper.convertValue(payload, CreerDelegationInput.class);
        UtilisateurEntity delegant = userRepository.findById(input.delegantId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Délégant introuvable : " + input.delegantId()));
        UtilisateurEntity delegataire = userRepository.findById(input.delegataireId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Délégataire introuvable : " + input.delegataireId()));

        DelegationPouvoirEntity delegation = DelegationPouvoirEntity.builder()
                .id(delegationId)
                .delegant(delegant)
                .delegataire(delegataire)
                .typeDelegation(input.typeDelegation())
                .plafondMontant(input.plafondMontant())
                .dateDebut(input.dateDebut())
                .dateFin(input.dateFin())
                .statut(StatutDelegation.ACTIVE)
                .build();

        return delegationRepository.save(delegation);
    }

    @Override
    @Transactional
    public void validateOrRejectDelegation(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_DELEGATION);
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
            log.info("Opération Délégation rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID delegationId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationDelegation(checkerOutput.payload(), delegationId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Délégation approuvée par Checker {}. Ticket: {}, Délégation PK: {}", checkerId, checkerOutput.id(), delegationId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelegationOutput> listerDelegationsActives() {
        return delegationRepository.findAll().stream()
                .filter(d -> d.getStatut() == StatutDelegation.ACTIVE)
                .map(this::mapToDelegationOutput)
                .toList();
    }

    @Override
    @Transactional
    public OrganigrammeVersionOutput creerVersion(CreerVersionOrganigrammeInput input) {
        if (versionRepository.existsByNumeroVersion(input.numeroVersion())) {
            throw new IllegalArgumentException("La version " + input.numeroVersion() + " existe déjà.");
        }

        OrganigrammeVersionEntity version = OrganigrammeVersionEntity.builder()
                .id(UUID.randomUUID())
                .numeroVersion(input.numeroVersion())
                .libelle(input.libelle())
                .description(input.description())
                .dateEffet(input.dateEffet())
                .statut(StatutVersionOrganigramme.BROUILLON)
                .build();

        version = versionRepository.save(version);
        return mapToVersionOutput(version);
    }

    @Override
    @Transactional
    public OrganigrammeVersionOutput activerVersion(UUID versionId, UUID approuvePar) {
        OrganigrammeVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Version d'organigramme introuvable : " + versionId));

        // Archiver les anciennes versions en vigueur
        List<OrganigrammeVersionEntity> actives = versionRepository.findByStatut(StatutVersionOrganigramme.EN_VIGUEUR);
        for (OrganigrammeVersionEntity ancienne : actives) {
            ancienne.setStatut(StatutVersionOrganigramme.ARCHIVE);
            versionRepository.save(ancienne);
        }

        version.setStatut(StatutVersionOrganigramme.EN_VIGUEUR);
        version.setApprouvePar(approuvePar);
        version.setDateApprobation(Instant.now());
        version = versionRepository.save(version);

        log.info("Nouvelle version d'organigramme activée : {} par {}", version.getNumeroVersion(), approuvePar);
        return mapToVersionOutput(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganigrammeVersionOutput> listerVersions() {
        return versionRepository.findAll().stream()
                .map(this::mapToVersionOutput)
                .toList();
    }

    // ==========================================
    // MÉTHODES DE RÉSOLUTION HIÉRARCHIQUE (PORT)
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurEntity> getManagerDirect(UUID utilisateurId) {
        List<AffectationPosteEntity> affectations = affectationRepository
                .findByUtilisateurIdAndStatut(utilisateurId, StatutAffectation.ACTIF);

        if (affectations.isEmpty()) return Optional.empty();

        // Préférer l'affectation titulaire
        AffectationPosteEntity aff = affectations.stream()
                .filter(a -> a.getTypeAffectation() == TypeAffectation.TITULAIRE)
                .findFirst()
                .orElse(affectations.get(0));

        PosteEntity poste = aff.getPoste();
        if (poste.getPosteSuperieur() == null) {
            return Optional.empty(); // Sommet de la hiérarchie
        }

        PosteEntity superieur = poste.getPosteSuperieur();
        List<AffectationPosteEntity> affsSuperieur = affectationRepository
                .findByPosteIdAndStatut(superieur.getId(), StatutAffectation.ACTIF);

        return affsSuperieur.stream()
                .filter(a -> a.getTypeAffectation() == TypeAffectation.TITULAIRE)
                .map(AffectationPosteEntity::getUtilisateur)
                .findFirst()
                .or(() -> affsSuperieur.stream().map(AffectationPosteEntity::getUtilisateur).findFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UtilisateurEntity> getChaineHierarchique(UUID utilisateurId, int maxNiveaux) {
        List<UtilisateurEntity> chaine = new ArrayList<>();
        UUID currentUserId = utilisateurId;
        int level = 0;

        while (level < maxNiveaux) {
            Optional<UtilisateurEntity> managerOpt = getManagerDirect(currentUserId);
            if (managerOpt.isEmpty()) break;

            UtilisateurEntity manager = managerOpt.get();
            chaine.add(manager);
            currentUserId = manager.getId();
            level++;
        }
        return chaine;
    }

    @Override
    @Transactional(readOnly = true)
    public ChaineHierarchiqueOutput getChaineHierarchiqueDetaillee(UUID utilisateurId, int maxNiveaux) {
        UtilisateurEntity user = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Utilisateur introuvable : " + utilisateurId));

        List<AffectationPosteEntity> affs = affectationRepository
                .findByUtilisateurIdAndStatut(utilisateurId, StatutAffectation.ACTIF);

        String posteIntitule = affs.isEmpty() ? "Sans poste" : affs.get(0).getPoste().getIntitule();
        String uniteIntitule = affs.isEmpty() ? "Sans unité" : affs.get(0).getPoste().getUniteOrganisationnelle().getIntitule();

        Optional<UtilisateurEntity> directManagerOpt = getManagerDirect(utilisateurId);
        UUID managerId = directManagerOpt.map(UtilisateurEntity::getId).orElse(null);
        String managerNom = directManagerOpt.map(u -> u.getNom() + " " + u.getPrenom()).orElse("Aucun (Sommet)");
        String managerPoste = directManagerOpt.flatMap(m -> affectationRepository.findByUtilisateurIdAndStatut(m.getId(), StatutAffectation.ACTIF).stream().findFirst())
                .map(a -> a.getPoste().getIntitule()).orElse("-");

        // Vérification de délégation sur le manager direct
        boolean estEnDelegation = false;
        UUID delegataireId = null;
        String delegataireNom = null;

        if (managerId != null) {
            List<DelegationPouvoirEntity> delegations = delegationRepository.findActiveDelegations(
                    managerId, Instant.now(), TypeDelegation.TOTALE);
            if (!delegations.isEmpty()) {
                estEnDelegation = true;
                UtilisateurEntity del = delegations.get(0).getDelegataire();
                delegataireId = del.getId();
                delegataireNom = del.getNom() + " " + del.getPrenom();
            }
        }

        List<ChaineHierarchiqueOutput.EchelonHierarchiqueOutput> ligne = new ArrayList<>();
        List<UtilisateurEntity> chaineManagers = getChaineHierarchique(utilisateurId, maxNiveaux);

        int niveauIndex = 1;
        for (UtilisateurEntity m : chaineManagers) {
            List<AffectationPosteEntity> mAffs = affectationRepository.findByUtilisateurIdAndStatut(m.getId(), StatutAffectation.ACTIF);
            PosteEntity p = mAffs.isEmpty() ? null : mAffs.get(0).getPoste();

            ligne.add(ChaineHierarchiqueOutput.EchelonHierarchiqueOutput.builder()
                    .niveau(niveauIndex++)
                    .utilisateurId(m.getId())
                    .nom(m.getNom() + " " + m.getPrenom())
                    .email(m.getEmail())
                    .posteCode(p != null ? p.getCodePoste() : "-")
                    .posteIntitule(p != null ? p.getIntitule() : "-")
                    .uniteCode(p != null ? p.getUniteOrganisationnelle().getCode() : "-")
                    .uniteIntitule(p != null ? p.getUniteOrganisationnelle().getIntitule() : "-")
                    .build());
        }

        return ChaineHierarchiqueOutput.builder()
                .collaborateurId(user.getId())
                .collaborateurNom(user.getNom() + " " + user.getPrenom())
                .posteIntitule(posteIntitule)
                .uniteIntitule(uniteIntitule)
                .managerDirectId(managerId)
                .managerDirectNom(managerNom)
                .managerDirectPoste(managerPoste)
                .estEnDelegation(estEnDelegation)
                .delegataireActuelId(delegataireId)
                .delegataireActuelNom(delegataireNom)
                .ligneHierarchique(ligne)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurEntity> getResponsableUnite(UUID uniteId) {
        List<PosteEntity> postes = posteRepository.findByUniteOrganisationnelleId(uniteId);
        // Le responsable est le poste de l'unité qui n'a pas de N+1 au sein de la même unité
        Optional<UtilisateurEntity> fromAff = postes.stream()
                .filter(p -> p.getPosteSuperieur() == null || !p.getPosteSuperieur().getUniteOrganisationnelle().getId().equals(uniteId))
                .flatMap(p -> affectationRepository.findByPosteIdAndStatut(p.getId(), StatutAffectation.ACTIF).stream())
                .filter(a -> a.getTypeAffectation() == TypeAffectation.TITULAIRE)
                .map(AffectationPosteEntity::getUtilisateur)
                .findFirst();

        if (fromAff.isPresent()) {
            return fromAff;
        }

        return postes.stream()
                .filter(p -> p.getPosteSuperieur() == null || !p.getPosteSuperieur().getUniteOrganisationnelle().getId().equals(uniteId))
                .flatMap(p -> employeRepository.findByPosteId(p.getId()).stream())
                .map(EmployeEntity::getUtilisateur)
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSuperieurHierarchique(UUID managerId, UUID collaborateurId) {
        List<UtilisateurEntity> chaine = getChaineHierarchique(collaborateurId, 10);
        return chaine.stream().anyMatch(m -> m.getId().equals(managerId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccessToUnite(UUID utilisateurId, UUID uniteId) {
        List<AffectationPosteEntity> affs = affectationRepository.findByUtilisateurIdAndStatut(utilisateurId, StatutAffectation.ACTIF);
        if (affs.isEmpty()) return false;

        UniteOrganisationnelleEntity targetUnite = uniteRepository.findById(uniteId).orElse(null);
        if (targetUnite == null) return false;

        for (AffectationPosteEntity aff : affs) {
            String userPath = aff.getPoste().getUniteOrganisationnelle().getMaterializedPath();
            // Si le chemin de l'unité de l'utilisateur est un préfixe du chemin de l'unité cible, il a accès
            if (targetUnite.getMaterializedPath().startsWith(userPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurEntity> resoudreApprobateurActuel(UUID demandeurId, BigDecimal montant, TypeDelegation type) {
        Optional<UtilisateurEntity> managerOpt = getManagerDirect(demandeurId);
        if (managerOpt.isEmpty()) return Optional.empty();

        UtilisateurEntity manager = managerOpt.get();
        List<DelegationPouvoirEntity> delegations = delegationRepository.findActiveDelegations(
                manager.getId(), Instant.now(), type);

        if (!delegations.isEmpty()) {
            for (DelegationPouvoirEntity del : delegations) {
                if (del.getPlafondMontant() == null || montant == null || del.getPlafondMontant().compareTo(montant) >= 0) {
                    log.info("Approbation routée vers le délégataire {} à la place du manager {}",
                            del.getDelegataire().getUsername(), manager.getUsername());
                    return Optional.of(del.getDelegataire());
                }
            }
        }
        return Optional.of(manager);
    }

    // ==========================================
    // TYPES DE STRUCTURES
    // ==========================================
    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public TypeStructureOutput creerTypeStructure(CreerTypeStructureInput input) {
        String code = input.code();
        if (code == null || code.trim().isEmpty()) {
            code = genererCodeTypeStructure(input.libelle());
        } else {
            code = code.trim().toUpperCase();
            if (typeStructureRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Un type de structure avec le code " + code + " existe déjà.");
            }
        }

        TypeStructureEntity typeParent = null;
        if (input.typeParentId() != null) {
            typeParent = typeStructureRepository.findById(input.typeParentId())
                    .orElseThrow(() -> new OrganigrammeNotFoundException("Type parent introuvable : " + input.typeParentId()));
        }
        TypeStructureEntity entity = TypeStructureEntity.builder()
                .id(UUID.randomUUID())
                .code(code)
                .libelle(input.libelle())
                .description(input.description())
                .typeParent(typeParent)
                .niveauHierarchique(input.niveauHierarchique() != null ? input.niveauHierarchique() : (typeParent != null ? typeParent.getNiveauHierarchique() + 1 : 1))
                .actif(input.actif() != null ? input.actif() : true)
                .couleur(input.couleur() != null ? input.couleur() : "#4f46e5")
                .icone(input.icone() != null ? input.icone() : "solar:buildings-2-line-duotone")
                .missionsTypes(input.missionsTypes())
                .build();
        entity = typeStructureRepository.save(entity);
        log.info("Type de structure créé avec code auto-généré : {} ({})", entity.getLibelle(), entity.getCode());
        return mapToTypeStructureOutput(entity);
    }

    private String genererCodeTypeStructure(String libelle) {
        if (libelle == null || libelle.isBlank()) {
            libelle = "TYPE";
        }
        String normalized = java.text.Normalizer.normalize(libelle, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String baseCode = normalized.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (baseCode.isBlank()) {
            baseCode = "TYPE";
        }
        if (baseCode.length() > 40) {
            baseCode = baseCode.substring(0, 40).replaceAll("_+$", "");
        }

        String candidate = baseCode;
        int counter = 1;
        while (typeStructureRepository.existsByCode(candidate)) {
            candidate = baseCode + "_" + counter;
            counter++;
        }
        return candidate;
    }

    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public TypeStructureOutput modifierTypeStructure(ModifierTypeStructureInput input) {
        TypeStructureEntity entity = typeStructureRepository.findById(input.id())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Type de structure introuvable : " + input.id()));

        TypeStructureEntity typeParent = null;
        if (input.typeParentId() != null) {
            if (input.typeParentId().equals(input.id())) {
                throw new CyclicDependencyException("Un type de structure ne peut pas être son propre parent.");
            }
            typeParent = typeStructureRepository.findById(input.typeParentId())
                    .orElseThrow(() -> new OrganigrammeNotFoundException("Type parent introuvable : " + input.typeParentId()));
        }
        entity.setLibelle(input.libelle());
        entity.setDescription(input.description());
        entity.setTypeParent(typeParent);
        if (input.niveauHierarchique() != null) {
            entity.setNiveauHierarchique(input.niveauHierarchique());
        }
        if (input.actif() != null) {
            entity.setActif(input.actif());
        }
        if (input.couleur() != null) {
            entity.setCouleur(input.couleur());
        }
        if (input.icone() != null) {
            entity.setIcone(input.icone());
        }
        if (input.missionsTypes() != null) {
            entity.setMissionsTypes(input.missionsTypes());
        }
        entity = typeStructureRepository.save(entity);
        log.info("Type de structure mis à jour : {} ({})", entity.getLibelle(), entity.getCode());
        return mapToTypeStructureOutput(entity);
    }

    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public void supprimerTypeStructure(UUID id) {
        TypeStructureEntity entity = typeStructureRepository.findById(id)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Type de structure introuvable : " + id));
        entity.setActif(false);
        typeStructureRepository.save(entity);
        log.info("Type de structure désactivé (soft-delete) : {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeStructureOutput> listerTypesStructures() {
        return typeStructureRepository.findAllByOrderByNiveauHierarchiqueAscLibelleAsc().stream()
                .map(this::mapToTypeStructureOutput)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TypeStructureOutput getTypeStructure(UUID id) {
        TypeStructureEntity entity = typeStructureRepository.findById(id)
                .orElseThrow(() -> new OrganigrammeNotFoundException("Type de structure introuvable : " + id));
        return mapToTypeStructureOutput(entity);
    }

    // ==========================================
    // OBJECTIFS D'UNITÉS
    // ==========================================
    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public ObjectifUniteOutput ajouterObjectif(CreerObjectifInput input) {
        UniteOrganisationnelleEntity unite = uniteRepository.findById(input.uniteId())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Unité introuvable : " + input.uniteId()));

        ObjectifUniteEntity objectif = ObjectifUniteEntity.builder()
                .id(UUID.randomUUID())
                .unite(unite)
                .intitule(input.intitule())
                .description(input.description())
                .indicateur(input.indicateur())
                .uniteMesure(input.uniteMesure() != null ? input.uniteMesure() : "UNITES")
                .valeurCible(input.valeurCible())
                .valeurRealisee(input.valeurRealisee() != null ? input.valeurRealisee() : BigDecimal.ZERO)
                .anneeExercice(input.anneeExercice() != null ? input.anneeExercice() : LocalDate.now().getYear())
                .dateEcheance(input.dateEcheance())
                .statut(input.statut() != null ? input.statut() : StatutObjectif.NON_DEMARRE)
                .responsableNom(input.responsableNom())
                .build();

        objectif.recalculerTaux();
        objectif = objectifRepository.save(objectif);
        log.info("Objectif ajouté à l'unité {} : {} (cible : {})", unite.getCode(), objectif.getIntitule(), objectif.getValeurCible());
        return mapToObjectifOutput(objectif);
    }

    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public ObjectifUniteOutput mettreAJourRealiseObjectif(MettreAJourRealiseObjectifInput input) {
        ObjectifUniteEntity objectif = objectifRepository.findById(input.id())
                .orElseThrow(() -> new OrganigrammeNotFoundException("Objectif introuvable : " + input.id()));
        objectif.setValeurRealisee(input.valeurRealisee());
        objectif.recalculerTaux();
        objectif = objectifRepository.save(objectif);
        log.info("Objectif {} mis à jour : réalisé={}, taux={}%", objectif.getId(), objectif.getValeurRealisee(), objectif.getTauxAtteinte());
        return mapToObjectifOutput(objectif);
    }

    @Override
    @Transactional
    @CacheEvict(value = "organigramme-tree", allEntries = true)
    public void supprimerObjectif(UUID id) {
        objectifRepository.deleteById(id);
        log.info("Objectif {} supprimé", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectifUniteOutput> listerObjectifsUnite(UUID uniteId, Integer anneeExercice) {
        List<ObjectifUniteEntity> list;
        if (anneeExercice != null) {
            list = objectifRepository.findByUnite_IdAndAnneeExerciceOrderByCreatedAtDesc(uniteId, anneeExercice);
        } else {
            list = objectifRepository.findByUnite_IdOrderByCreatedAtDesc(uniteId);
        }
        return list.stream().map(this::mapToObjectifOutput).toList();
    }

    // ==========================================
    // HELPERS & MAPPERS
    // ==========================================

    private ArborescenceNoeudOutput construireSousArbre(UniteOrganisationnelleEntity unite) {
        List<UniteOrganisationnelleEntity> enfants = uniteRepository.findByParentId(unite.getId());
        List<ArborescenceNoeudOutput> sousNoeuds = enfants.stream()
                .map(this::construireSousArbre)
                .toList();

        List<PosteEntity> postes = posteRepository.findByUniteOrganisationnelleId(unite.getId());
        int totalPostesAutorises = postes.stream().mapToInt(PosteEntity::getEffectifAutorise).sum();
        int totalPostesPourvus = postes.stream().mapToInt(PosteEntity::getEffectifActuel).sum();

        Optional<UtilisateurEntity> resp = getResponsableUnite(unite.getId());
        String respNom = resp.map(u -> u.getNom() + " " + u.getPrenom()).orElse("Non pourvu");
        String respPoste = resp.flatMap(u -> affectationRepository.findByUtilisateurIdAndStatut(u.getId(), StatutAffectation.ACTIF).stream().findFirst())
                .map(a -> a.getPoste().getIntitule()).orElse("-");

        BigDecimal tauxMoyen = objectifRepository.calculateAverageTauxByUniteId(unite.getId());
        if (tauxMoyen != null) {
            tauxMoyen = tauxMoyen.setScale(1, RoundingMode.HALF_UP);
        } else {
            tauxMoyen = BigDecimal.ZERO;
        }
        int nbObjectifs = (int) objectifRepository.countByUniteId(unite.getId());

        return ArborescenceNoeudOutput.builder()
                .id(unite.getId())
                .code(unite.getCode())
                .intitule(unite.getIntitule())
                .typeUnite(unite.getTypeUnite())
                .typeStructureId(unite.getTypeStructure() != null ? unite.getTypeStructure().getId() : null)
                .typeStructureLibelle(unite.getTypeStructure() != null ? unite.getTypeStructure().getLibelle() : null)
                .typeStructureCode(unite.getTypeStructure() != null ? unite.getTypeStructure().getCode() : null)
                .typeStructureCouleur(unite.getTypeStructure() != null ? unite.getTypeStructure().getCouleur() : null)
                .typeStructureIcone(unite.getTypeStructure() != null ? unite.getTypeStructure().getIcone() : null)
                .materializedPath(unite.getMaterializedPath())
                .localisation(unite.getLocalisation())
                .statut(unite.getStatut())
                .totalPostesAutorises(totalPostesAutorises)
                .totalPostesPourvus(totalPostesPourvus)
                .responsableNom(respNom)
                .responsablePoste(respPoste)
                .tauxMoyenObjectifs(tauxMoyen)
                .nbObjectifs(nbObjectifs)
                .sousUnites(sousNoeuds)
                .build();
    }

    private UniteOrganisationnelleOutput mapToUniteOutput(UniteOrganisationnelleEntity u) {
        int nbEnfants = uniteRepository.findByParentId(u.getId()).size();
        int nbPostes = posteRepository.findByUniteOrganisationnelleId(u.getId()).size();

        BigDecimal tauxMoyen = objectifRepository.calculateAverageTauxByUniteId(u.getId());
        if (tauxMoyen != null) {
            tauxMoyen = tauxMoyen.setScale(1, RoundingMode.HALF_UP);
        } else {
            tauxMoyen = BigDecimal.ZERO;
        }
        int nbObjectifs = (int) objectifRepository.countByUniteId(u.getId());

        return UniteOrganisationnelleOutput.builder()
                .id(u.getId())
                .code(u.getCode())
                .intitule(u.getIntitule())
                .typeUnite(u.getTypeUnite())
                .typeStructureId(u.getTypeStructure() != null ? u.getTypeStructure().getId() : null)
                .typeStructureLibelle(u.getTypeStructure() != null ? u.getTypeStructure().getLibelle() : null)
                .typeStructureCode(u.getTypeStructure() != null ? u.getTypeStructure().getCode() : null)
                .typeStructureCouleur(u.getTypeStructure() != null ? u.getTypeStructure().getCouleur() : null)
                .typeStructureIcone(u.getTypeStructure() != null ? u.getTypeStructure().getIcone() : null)
                .parentId(u.getParent() != null ? u.getParent().getId() : null)
                .parentIntitule(u.getParent() != null ? u.getParent().getIntitule() : null)
                .materializedPath(u.getMaterializedPath())
                .localisation(u.getLocalisation())
                .statut(u.getStatut())
                .dateCreation(u.getDateCreation())
                .dateFermeture(u.getDateFermeture())
                .nbSousUnites(nbEnfants)
                .nbPostes(nbPostes)
                .tauxMoyenObjectifs(tauxMoyen)
                .nbObjectifs(nbObjectifs)
                .build();
    }

    private TypeStructureOutput mapToTypeStructureOutput(TypeStructureEntity t) {
        return TypeStructureOutput.builder()
                .id(t.getId())
                .code(t.getCode())
                .libelle(t.getLibelle())
                .description(t.getDescription())
                .typeParentId(t.getTypeParent() != null ? t.getTypeParent().getId() : null)
                .typeParentLibelle(t.getTypeParent() != null ? t.getTypeParent().getLibelle() : null)
                .niveauHierarchique(t.getNiveauHierarchique())
                .actif(t.getActif())
                .couleur(t.getCouleur())
                .icone(t.getIcone())
                .missionsTypes(t.getMissionsTypes())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private ObjectifUniteOutput mapToObjectifOutput(ObjectifUniteEntity o) {
        return ObjectifUniteOutput.builder()
                .id(o.getId())
                .uniteId(o.getUnite().getId())
                .uniteIntitule(o.getUnite().getIntitule())
                .intitule(o.getIntitule())
                .description(o.getDescription())
                .indicateur(o.getIndicateur())
                .uniteMesure(o.getUniteMesure())
                .valeurCible(o.getValeurCible())
                .valeurRealisee(o.getValeurRealisee())
                .tauxAtteinte(o.getTauxAtteinte())
                .anneeExercice(o.getAnneeExercice())
                .dateEcheance(o.getDateEcheance())
                .statut(o.getStatut())
                .responsableNom(o.getResponsableNom())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private PosteOutput mapToPosteOutput(PosteEntity p) {
        List<AffectationPosteEntity> affs = affectationRepository
                .findByPosteIdAndStatut(p.getId(), StatutAffectation.ACTIF);

        Optional<AffectationPosteEntity> aff = affs.stream()
                .filter(a -> a.getTypeAffectation() == TypeAffectation.TITULAIRE)
                .findFirst()
                .or(() -> affs.stream().findFirst());

        String titulaireNom = aff.map(a -> a.getUtilisateur().getNom() + " " + a.getUtilisateur().getPrenom()).orElse(null);
        UUID titulaireId = aff.map(a -> a.getUtilisateur().getId()).orElse(null);

        // Si aucune affectation explicite n'a été trouvée, fallback sur le dossier Employé direct
        if (titulaireNom == null) {
            List<EmployeEntity> emps = employeRepository.findByPosteId(p.getId());
            if (!emps.isEmpty()) {
                EmployeEntity e = emps.get(0);
                titulaireNom = e.getNom() + " " + e.getPrenom();
                titulaireId = e.getUtilisateur() != null ? e.getUtilisateur().getId() : e.getId();
            }
        }

        return PosteOutput.builder()
                .id(p.getId())
                .codePoste(p.getCodePoste())
                .intitule(p.getIntitule())
                .uniteId(p.getUniteOrganisationnelle().getId())
                .uniteIntitule(p.getUniteOrganisationnelle().getIntitule())
                .posteSuperieurId(p.getPosteSuperieur() != null ? p.getPosteSuperieur().getId() : null)
                .posteSuperieurIntitule(p.getPosteSuperieur() != null ? p.getPosteSuperieur().getIntitule() : null)
                .effectifAutorise(p.getEffectifAutorise())
                .effectifActuel(p.getEffectifActuel())
                .categorieConvention(p.getCategorieConvention())
                .statut(p.getStatut())
                .descriptionMission(p.getDescriptionMission())
                .titulaireActuelNom(titulaireNom != null ? titulaireNom : "Poste vacant")
                .titulaireActuelId(titulaireId)
                .permissions(p.getPermissions() != null ? p.getPermissions() : java.util.Collections.emptySet())
                .build();
    }

    private AffectationOutput mapToAffectationOutput(AffectationPosteEntity a) {
        return AffectationOutput.builder()
                .id(a.getId())
                .utilisateurId(a.getUtilisateur().getId())
                .utilisateurNom(a.getUtilisateur().getNom() + " " + a.getUtilisateur().getPrenom())
                .utilisateurEmail(a.getUtilisateur().getEmail())
                .posteId(a.getPoste().getId())
                .posteCode(a.getPoste().getCodePoste())
                .posteIntitule(a.getPoste().getIntitule())
                .uniteId(a.getPoste().getUniteOrganisationnelle().getId())
                .uniteIntitule(a.getPoste().getUniteOrganisationnelle().getIntitule())
                .typeAffectation(a.getTypeAffectation())
                .dateDebut(a.getDateDebut())
                .dateFin(a.getDateFin())
                .referenceDecision(a.getReferenceDecision())
                .motifFin(a.getMotifFin())
                .statut(a.getStatut())
                .build();
    }

    private DelegationOutput mapToDelegationOutput(DelegationPouvoirEntity d) {
        return DelegationOutput.builder()
                .id(d.getId())
                .delegantId(d.getDelegant().getId())
                .delegantNom(d.getDelegant().getNom() + " " + d.getDelegant().getPrenom())
                .delegataireId(d.getDelegataire().getId())
                .delegataireNom(d.getDelegataire().getNom() + " " + d.getDelegataire().getPrenom())
                .typeDelegation(d.getTypeDelegation())
                .plafondMontant(d.getPlafondMontant())
                .dateDebut(d.getDateDebut())
                .dateFin(d.getDateFin())
                .statut(d.getStatut())
                .build();
    }

    private OrganigrammeVersionOutput mapToVersionOutput(OrganigrammeVersionEntity v) {
        return OrganigrammeVersionOutput.builder()
                .id(v.getId())
                .numeroVersion(v.getNumeroVersion())
                .libelle(v.getLibelle())
                .description(v.getDescription())
                .dateEffet(v.getDateEffet())
                .statut(v.getStatut())
                .approuvePar(v.getApprouvePar())
                .dateApprobation(v.getDateApprobation())
                .build();
    }
}
