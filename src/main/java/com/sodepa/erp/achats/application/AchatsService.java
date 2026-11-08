package com.sodepa.erp.achats.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.achats.infrastructure.PvEventInput;
import com.sodepa.erp.achats.infrastructure.entities.*;
import com.sodepa.erp.achats.infrastructure.repo.*;
import com.sodepa.erp.achats.presentation.requests.*;
import com.sodepa.erp.achats.presentation.responses.*;
import com.sodepa.erp.budget.infrastructure.entities.AchatSeuilConfigEntity;
import com.sodepa.erp.budget.infrastructure.repo.AchatSeuilConfigRepository;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity;
import com.sodepa.erp.ged.infrastructure.repo.DocumentGedRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.share.GlobalConstants;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.MinioService;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.presentation.requests.DemarrerWorkflowRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `AchatsService` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchatsService {


    private final AchatsSecurityService securityService;
    private final FournisseurRepository fournisseurRepository;
    private final ExpressionBesoinRepository expressionBesoinRepository;
    private final ExpressionBesoinLigneRepository expressionBesoinLigneRepository;
    private final MarcheDossierRepository marcheDossierRepository;
    private final MarcheDossierLigneRepository marcheDossierLigneRepository;
    private final MarcheReceptionPvRepository marcheReceptionPvRepository;
    private final AchatSeuilConfigRepository seuilConfigRepository;
    private final EmployeRepository employeRepository;
    private final UniteOrganisationnelleRepository uniteOrganisationnelleRepository;
    private final ParametreConfigurationService configService;
    private final MinioService minioService;
    private final DocumentGedRepository documentGedRepository;
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final Optional<WorkflowService> workflowService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // FOURNISSEURS
    // ==========================================

    @Transactional(readOnly = true)
    public List<FournisseurResponse> listerFournisseurs(boolean agreeSeulement) {
        securityService.checkPermission(Permissions.LIST_PROVIDERS);
        List<FournisseurEntity> list = agreeSeulement
                ? fournisseurRepository.findByAgreeSodepaTrueAndActifTrue()
                : fournisseurRepository.findAll(Sort.by(Sort.Direction.ASC, "raisonSociale"));
        return list.stream().map(FournisseurResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public UUID creerFournisseur(CreerFournisseurRequest request) {
        securityService.checkPermission(Permissions.CREATE_PROVIDER);
        long count = fournisseurRepository.count() + 1;
        String code = String.format("FRN-%04d", count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new java.util.HashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("code", code);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.FOURNISSEUR,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Création de fournisseur soumise au Maker-Checker. Code: {}, Raison Sociale: {}", code, request.getRaisonSociale());

        if (securityService.canValidate(Permissions.VALIDATE_PROVIDER)) {
            log.info("Utilisateur habilité, validation automatique du fournisseur {}", code);
            try {
                validateOrRejectFournisseur(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique du fournisseur {}: {}", code, e.getMessage());
            }
        }
        return uuid;
    }

    @Transactional
    public void validateOrRejectFournisseur(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_PROVIDER);
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
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
            log.info("Fournisseur rejeté par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID frnId = UUID.fromString(checkerOutput.entityPk());
            Map<String, Object> payload = checkerOutput.payload();
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationFournisseur(payload, frnId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Fournisseur approuvé par le Checker {}. Ticket: {}, PK: {}", checkerId, checkerOutput.id(), frnId);
        }
    }

    private FournisseurEntity creationFournisseur(Map<String, Object> payload, UUID frnId) {
        FournisseurEntity entity = FournisseurEntity.builder()
                .id(frnId)
                .code((String) payload.get("code"))
                .raisonSociale((String) payload.get("raisonSociale"))
                .numeroContribuable((String) payload.get("numeroContribuable"))
                .rccm((String) payload.get("rccm"))
                .regimeFiscal(payload.get("regimeFiscal") != null ? (String) payload.get("regimeFiscal") : "REEL")
                .specialite((String) payload.get("specialite"))
                .adresse((String) payload.get("adresse"))
                .telephone((String) payload.get("telephone"))
                .email((String) payload.get("email"))
                .banqueNom((String) payload.get("banqueNom"))
                .compteBancaireRib((String) payload.get("compteBancaireRib"))
                .agreeSodepa(true)
                .dateAgrement(LocalDate.now())
                .actif(true)
                .build();
        FournisseurEntity saved = fournisseurRepository.save(entity);
        log.info("Fournisseur matérialisé en base: {} ({})", saved.getRaisonSociale(), saved.getCode());
        return saved;
    }

    // ==========================================
    // EXPRESSIONS DE BESOINS (EB)
    // ==========================================

    @Transactional(readOnly = true)
    public List<ExpressionBesoinResponse> listerExpressionsBesoin() {
        securityService.checkPermission(Permissions.LIST_EXPRESSIONS);
        String scope = securityService.getScopeMaterializedPath();
        List<ExpressionBesoinEntity> list;
        if (scope == null) {
            list = expressionBesoinRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));
        } else {
            list = expressionBesoinRepository.findByMaterializedPathStartingWith(scope);
        }
        return list.stream()
                .map(ExpressionBesoinResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpressionBesoinResponse obtenirExpressionBesoin(UUID id) {
        securityService.checkPermission(Permissions.VIEW_EXPRESSION);
        ExpressionBesoinEntity eb = getEbOrThrow(id);
        securityService.validateAccessToUnite(eb.getUniteOrganisationnelle(),
                "Accès refusé à cette expression de besoin : hors de votre périmètre hiérarchique.");
        return ExpressionBesoinResponse.fromEntity(eb);
    }

    @Transactional
    public ExpressionBesoinResponse creerExpressionBesoin(CreerExpressionBesoinRequest request) {
        securityService.checkPermission(Permissions.CREATE_EXPRESSION);
        long count = expressionBesoinRepository.count() + 1;
        int annee = LocalDate.now().getYear();
        String numero = String.format("EB-%d-%04d", annee, count);
        UUID id = UUID.randomUUID();

        // Résolution de l'unité de rattachement depuis l'affectation active si omise
        UniteOrganisationnelleEntity unite = null;
        if (request.getUniteOrganisationnelleId() != null) {
            unite = uniteOrganisationnelleRepository.findById(request.getUniteOrganisationnelleId()).orElse(null);
        }
        if (unite == null) {
            unite = securityService.getCurrentUserUnite().orElse(null);
        }
        if (unite != null) {
            securityService.validateAccessToUnite(unite, Permissions.CREATE_EXPRESSION.name());
        }

        String structureNom = request.getStructureNom();
        if (structureNom == null && unite != null) {
            structureNom = unite.getIntitule();
        }

        String demandeurNom = request.getDemandeurNom();
        if (demandeurNom == null) {
            UserData ud = securityService.getCurrentUserData();
            demandeurNom = ud.name();
        }

        Map<String, Object> payload = new java.util.HashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("numero", numero);
        payload.put("structureNom", structureNom);
        payload.put("demandeurNom", demandeurNom);
        if (unite != null) {
            payload.put("uniteOrganisationnelleId", unite.getId().toString());
        }

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.EXPRESSION_BESOIN,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Expression de besoin soumise au Maker-Checker: {} (Ticket: {})", numero, uuid);

        if (securityService.canValidate(Permissions.VALIDATE_EXPRESSION)) {
            log.info("Utilisateur habilité, validation automatique de l'EB {}", numero);
            try {
                validateOrRejectExpressionBesoin(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique de l'EB {}: {}", numero, e.getMessage());
            }
        }

        final String finalStructureNom = structureNom;
        final String finalDemandeurNom = demandeurNom;

        return expressionBesoinRepository.findById(id)
                .map(ExpressionBesoinResponse::fromEntity)
                .orElseGet(() -> ExpressionBesoinResponse.builder()
                        .id(id)
                        .numero(numero)
                        .demandeurNom(finalDemandeurNom)
                        .structureNom(finalStructureNom)
                        .motif(request.getMotif())
                        .dateExpression(LocalDate.now())
                        .statut(StatutExpressionBesoin.BROUILLON)
                        .build());
    }

    @Transactional
    public void validateOrRejectExpressionBesoin(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_EXPRESSION);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.EXPRESSION_BESOIN,
                "Accès refusé pour validation/rejet de l'EB : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
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
            log.info("Expression de besoin rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID ebId = UUID.fromString(checkerOutput.entityPk());
            Map<String, Object> payload = checkerOutput.payload();
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationExpressionBesoin(payload, ebId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Expression de besoin approuvée par le Checker {}. Ticket: {}, PK: {}", checkerId, checkerOutput.id(), ebId);
        }
    }

    private ExpressionBesoinEntity creationExpressionBesoin(Map<String, Object> payload, UUID ebId) {
        UniteOrganisationnelleEntity unite = null;
        Object uIdObj = payload.get("uniteOrganisationnelleId");
        if (uIdObj != null) {
            unite = uniteOrganisationnelleRepository.findById(UUID.fromString(uIdObj.toString())).orElse(null);
        }

        ExpressionBesoinEntity eb = ExpressionBesoinEntity.builder()
                .id(ebId)
                .numero((String) payload.get("numero"))
                .demandeurNom((String) payload.get("demandeurNom"))
                .structureNom((String) payload.get("structureNom"))
                .motif((String) payload.get("motif"))
                .dateExpression(LocalDate.now())
                .stockDisponible(false)
                .attestationCarenceEtablie(true)
                .budgetDisponible(true)
                .ligneBudgetaireImputation((String) payload.get("ligneBudgetaireImputation"))
                .statut(StatutExpressionBesoin.BROUILLON)
                .dateCreation(Instant.now())
                .lignes(new ArrayList<>())
                .uniteOrganisationnelle(unite)
                .build();

        Object demandeurIdObj = payload.get("demandeurId");
        if (demandeurIdObj != null) {
            employeRepository.findById(UUID.fromString(demandeurIdObj.toString())).ifPresent(eb::setDemandeur);
        }

        BigDecimal totalEstime = BigDecimal.ZERO;
        Object lignesObj = payload.get("lignes");
        if (lignesObj instanceof List<?> rawList) {
            int ordre = 1;
            for (Object item : rawList) {
                CreerExpressionBesoinRequest.LigneExpressionBesoinRequest l = objectMapper.convertValue(item, CreerExpressionBesoinRequest.LigneExpressionBesoinRequest.class);
                BigDecimal pu = l.getPrixUnitaireEstime() != null ? l.getPrixUnitaireEstime() : BigDecimal.ZERO;
                BigDecimal totalLigne = pu.multiply(l.getQuantite()).setScale(2, RoundingMode.HALF_UP);
                totalEstime = totalEstime.add(totalLigne);

                ExpressionBesoinLigneEntity ligneEntity = ExpressionBesoinLigneEntity.builder()
                        .id(UUID.randomUUID())
                        .expressionBesoin(eb)
                        .designation(l.getDesignation())
                        .specificationsTechniques(l.getSpecificationsTechniques())
                        .quantite(l.getQuantite())
                        .uniteMesure(l.getUniteMesure() != null ? l.getUniteMesure() : "U")
                        .prixUnitaireEstime(pu)
                        .montantTotalEstime(totalLigne)
                        .ordre(ordre++)
                        .build();
                eb.getLignes().add(ligneEntity);
            }
        }
        eb.setMontantEstimeTotal(totalEstime);

        ExpressionBesoinEntity saved = expressionBesoinRepository.save(eb);
        log.info("Expression de besoin matérialisée en base: {} pour un montant estimé de {}", saved.getNumero(), saved.getMontantEstimeTotal());

        // Démarrage du circuit d'approbation N+1
        try {
            workflowService.ifPresent(wf -> {
                try {
                    DemarrerWorkflowRequest wfReq = DemarrerWorkflowRequest.builder()
                            .workflowDefinitionCode("CIRCUIT_VALIDATION_EB")
                            .entityName("EXPRESSION_BESOIN")
                            .entityId(saved.getId().toString())
                            .initiateurId(securityService.getCurrentUserId())
                            .contexte(Map.of("numero", saved.getNumero(), "montantEstime", saved.getMontantEstimeTotal()))
                            .build();
                    wf.demarrerWorkflow(wfReq);
                    log.info("Workflow d'approbation hiérarchique initié pour EB: {}", saved.getNumero());
                } catch (Exception ex) {
                    log.warn("Circuit de workflow EB non initialisé: {}", ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Impossible de déclencher le workflow pour l'EB {}: {}", saved.getNumero(), e.getMessage());
        }

        return saved;
    }

    private ExpressionBesoinEntity getEbOrThrow(UUID id) {
        return expressionBesoinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.NEED_EXPRESSION_NOT_FOUND));
    }

    // ==========================================
    // DOSSIERS D'ACHAT / MARCHÉS / BONS DE COMMANDE
    // ==========================================

    @Transactional(readOnly = true)
    public List<MarcheDossierResponse> listerDossiers() {
        securityService.checkPermission(Permissions.LIST_FOLDERS);
        String scope = securityService.getScopeMaterializedPath();
        List<MarcheDossierEntity> list;
        if (scope == null) {
            list = marcheDossierRepository.findAll(Sort.by(Sort.Direction.DESC, "dateCreation"));
        } else {
            list = marcheDossierRepository.findByExpressionBesoinMaterializedPathStartingWith(scope);
        }
        return list.stream()
                .map(MarcheDossierResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarcheDossierResponse obtenirDossier(UUID id) {
        securityService.checkPermission(Permissions.VIEW_FOLDER);
        MarcheDossierEntity dossier = getDossierOrThrow(id);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé à ce dossier de commande : hors de votre périmètre hiérarchique.");
        }
        return MarcheDossierResponse.fromEntity(dossier);
    }

    @Transactional
    public MarcheDossierResponse creerDossier(CreerMarcheDossierRequest request) {
        securityService.checkPermission(Permissions.CREATE_FOLDER);
        FournisseurEntity frn = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.SUPPLIER_NOT_FOUND));

        ExpressionBesoinEntity eb = null;
        if (request.getExpressionBesoinId() != null) {
            eb = getEbOrThrow(request.getExpressionBesoinId());
            securityService.validateAccessToUnite(eb.getUniteOrganisationnelle(),
                    "Accès refusé pour la création du dossier : l'expression de besoin est hors périmètre.");
        }

        BigDecimal totalHt = BigDecimal.ZERO;
        BigDecimal totalTva = BigDecimal.ZERO;

        List<Map<String, Object>> lignesPayload = new ArrayList<>();
        int ordre = 1;
        for (CreerMarcheDossierRequest.LigneMarcheDossierRequest l : request.getLignes()) {
            BigDecimal pu = l.getPrixUnitaireHt();
            BigDecimal htLigne = pu.multiply(l.getQuantite()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tauxDefaut = configService.getDecimal(GlobalConstants.ACHATS_TAUX_TVA, BigDecimal.valueOf(19.25));
            BigDecimal taux = l.getTauxTva() != null ? l.getTauxTva() : tauxDefaut;
            BigDecimal tvaLigne = htLigne.multiply(taux).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal ttcLigne = htLigne.add(tvaLigne);

            totalHt = totalHt.add(htLigne);
            totalTva = totalTva.add(tvaLigne);

            Map<String, Object> ligneMap = Map.of(
                    "designation", l.getDesignation(),
                    "quantite", l.getQuantite(),
                    "uniteMesure", l.getUniteMesure() != null ? l.getUniteMesure() : "U",
                    "prixUnitaireHt", pu,
                    "montantHt", htLigne,
                    "tauxTva", taux,
                    "montantTtc", ttcLigne,
                    "ordre", ordre++
            );
            lignesPayload.add(ligneMap);
        }

        BigDecimal totalTtc = totalHt.add(totalTva);

        // Détermination automatique de la procédure selon les seuils SODEPA
        TypeProcedureAchat procedure = determinerProcedure(totalTtc);
        boolean cipmRequis = (procedure == TypeProcedureAchat.MARCHE_PUBLIC_CIPM || procedure == TypeProcedureAchat.LETTRE_COMMANDE);

        int annee = LocalDate.now().getYear();
        long count = marcheDossierRepository.count() + 1;
        String numeroDossier = String.format("%s-%d-%04d", procedure.getPrefix(), annee, count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", id.toString());
        payload.put("numeroDossier", numeroDossier);
        payload.put("typeProcedure", procedure.name());
        payload.put("objet", request.getObjet());
        payload.put("fournisseurId", frn.getId().toString());
        payload.put("fournisseurNom", frn.getRaisonSociale());
        payload.put("montantHt", totalHt);
        payload.put("montantTva", totalTva);
        payload.put("montantTtc", totalTtc);
        payload.put("delaiLivraisonJours", request.getDelaiLivraisonJours() != null ? request.getDelaiLivraisonJours() : 90);
        payload.put("examenCipmRequis", cipmRequis);
        payload.put("lignes", lignesPayload);
        if (eb != null) {
            payload.put("expressionBesoinId", eb.getId().toString());
            payload.put("ligneBudgetaireCode", request.getLigneBudgetaireCode() != null ? request.getLigneBudgetaireCode() : eb.getLigneBudgetaireImputation());
        } else if (request.getLigneBudgetaireCode() != null) {
            payload.put("ligneBudgetaireCode", request.getLigneBudgetaireCode());
        }

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Dossier de commande soumis au Maker-Checker: {} (Ticket: {})", numeroDossier, uuid);

        if (securityService.canValidate(Permissions.VALIDATE_FOLDER)) {
            log.info("Utilisateur habilité, validation automatique du dossier {}", numeroDossier);
            try {
                validateOrRejectMarcheDossier(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique du dossier {}: {}", numeroDossier, e.getMessage());
            }
        }

        final BigDecimal finalTotalHt = totalHt;
        final BigDecimal finalTotalTva = totalTva;

        return marcheDossierRepository.findById(id)
                .map(MarcheDossierResponse::fromEntity)
                .orElseGet(() -> MarcheDossierResponse.builder()
                        .id(id)
                        .numeroDossier(numeroDossier)
                        .typeProcedure(procedure)
                        .objet(request.getObjet())
                        .fournisseurNom(frn.getRaisonSociale())
                        .montantHt(finalTotalHt)
                        .montantTva(finalTotalTva)
                        .montantTtc(totalTtc)
                        .statut(StatutMarcheDossier.BROUILLON)
                        .build());
    }

    @Transactional
    public void validateOrRejectMarcheDossier(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_FOLDER);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.MARCHE_DOSSIER,
                "Accès refusé pour validation/rejet du dossier : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
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
            log.info("Dossier de marché rejeté par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID dossierId = UUID.fromString(checkerOutput.entityPk());
            Map<String, Object> payload = checkerOutput.payload();
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationMarcheDossier(payload, dossierId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Dossier de marché approuvé par le Checker {}. Ticket: {}, PK: {}", checkerId, checkerOutput.id(), dossierId);
        }
    }

    private MarcheDossierEntity creationMarcheDossier(Map<String, Object> payload, UUID dossierId) {
        FournisseurEntity frn = null;
        Object frnIdObj = payload.get("fournisseurId");
        if (frnIdObj != null) {
            frn = fournisseurRepository.findById(UUID.fromString(frnIdObj.toString())).orElse(null);
        }

        ExpressionBesoinEntity eb = null;
        Object ebIdObj = payload.get("expressionBesoinId");
        if (ebIdObj != null) {
            eb = expressionBesoinRepository.findById(UUID.fromString(ebIdObj.toString())).orElse(null);
        }

        TypeProcedureAchat procedure = TypeProcedureAchat.valueOf((String) payload.get("typeProcedure"));
        BigDecimal montantHt = new BigDecimal(payload.get("montantHt").toString());
        BigDecimal montantTva = new BigDecimal(payload.get("montantTva").toString());
        BigDecimal montantTtc = new BigDecimal(payload.get("montantTtc").toString());
        Integer delai = payload.get("delaiLivraisonJours") != null ? Integer.valueOf(payload.get("delaiLivraisonJours").toString()) : 90;
        Boolean cipmRequis = Boolean.valueOf(payload.get("examenCipmRequis").toString());

        MarcheDossierEntity dossier = MarcheDossierEntity.builder()
                .id(dossierId)
                .numeroDossier((String) payload.get("numeroDossier"))
                .typeProcedure(procedure)
                .objet((String) payload.get("objet"))
                .expressionBesoin(eb)
                .fournisseur(frn)
                .fournisseurNom((String) payload.get("fournisseurNom"))
                .montantHt(montantHt)
                .montantTva(montantTva)
                .montantTtc(montantTtc)
                .ligneBudgetaireCode((String) payload.get("ligneBudgetaireCode"))
                .delaiLivraisonJours(delai)
                .statut(StatutMarcheDossier.BROUILLON)
                .proformasObtenuesCount(0)
                .examenCipmRequis(cipmRequis)
                .dateCreation(Instant.now())
                .lignes(new ArrayList<>())
                .procesVerbaux(new ArrayList<>())
                .build();

        Object lignesObj = payload.get("lignes");
        if (lignesObj instanceof List<?> rawList) {
            for (Object item : rawList) {
                Map<String, Object> l = (Map<String, Object>) item;
                MarcheDossierLigneEntity ligneEntity = MarcheDossierLigneEntity.builder()
                        .id(UUID.randomUUID())
                        .dossier(dossier)
                        .designation((String) l.get("designation"))
                        .quantite(new BigDecimal(l.get("quantite").toString()))
                        .uniteMesure((String) l.get("uniteMesure"))
                        .prixUnitaireHt(new BigDecimal(l.get("prixUnitaireHt").toString()))
                        .montantHt(new BigDecimal(l.get("montantHt").toString()))
                        .tauxTva(new BigDecimal(l.get("tauxTva").toString()))
                        .montantTtc(new BigDecimal(l.get("montantTtc").toString()))
                        .ordre(Integer.parseInt(l.get("ordre").toString()))
                        .build();
                dossier.getLignes().add(ligneEntity);
            }
        }

        MarcheDossierEntity saved = marcheDossierRepository.save(dossier);
        log.info("Dossier de marché matérialisé en base: {} ({})", saved.getNumeroDossier(), saved.getTypeProcedure());
        return saved;
    }

    @Transactional
    public MarcheDossierResponse enregistrerProformas(UUID dossierId, int count, MultipartFile file) {
        securityService.checkPermission(Permissions.UPDATE_FOLDER);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé au dossier : hors périmètre hiérarchique.");
        }

        String cle = "";
        if (file != null && !file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String hashSha256 = calculateSha256(bytes);
                cle = "achats/dossiers/" + dossierId + "/proformas_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String bucket = "sodepa-ged";

                try {
                    minioService.uploadFile(file, cle);
                } catch (Exception e) {
                    log.warn("MinIO indisponible pour l'upload du proforma : {}", e.getMessage());
                }

                DocumentGedEntity docGed = DocumentGedEntity.builder()
                        .id(UUID.randomUUID())
                        .nomFichier(file.getOriginalFilename())
                        .tailleOctets(file.getSize())
                        .contentType(file.getContentType() != null ? file.getContentType() : "application/pdf")
                        .hashSha256(hashSha256)
                        .s3Cle(cle)
                        .s3Bucket(bucket)
                        .typeEntite("MARCHE_DOSSIER")
                        .entiteId(dossierId)
                        .description("Factures proforma (au moins " + count + " reçues) pour dossier " + dossier.getNumeroDossier())
                        .uploadPar(securityService.getCurrentUserData().username())
                        .dateUpload(LocalDateTime.now())
                        .build();
                documentGedRepository.save(docGed);
                log.info("Pièce jointe proforma archivée en GED MinIO avec hash SHA-256: {}", hashSha256);
            } catch (Exception e) {
                log.error("Erreur lors de l'archivage GED de la proforma", e);
            }
        }

        Map<String, Object> payload = Map.of(
                "action", "ENREGISTRER_PROFORMAS",
                "dossierId", dossierId.toString(),
                "proformasCount", count,
                "s3Cle", cle
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                dossierId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Réception des proformas soumise au Maker-Checker pour dossier {} (Ticket: {})", dossier.getNumeroDossier(), uuid);

        if (securityService.canValidate(Permissions.UPDATE_FOLDER)) {
            dossier.setProformasObtenuesCount(count);
            dossier.setStatut(StatutMarcheDossier.SELECTION_PRESTATAIRE);
            dossier = marcheDossierRepository.save(dossier);
            makerCheckerEngine.update(uuid, MakerCheckerStatus.ACCEPTED, "Auto-validation proformas", securityService.getUtilsService().getCurrentUserData().userId());
        }

        return MarcheDossierResponse.fromEntity(dossier);
    }

    @Transactional
    public MarcheDossierResponse engagerBudget(UUID dossierId, String ligneBudgetaire) {
        securityService.checkPermission(Permissions.ENGAGE_BUDGET);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé pour engagement budgétaire : hors de votre périmètre hiérarchique.");
        }

        Map<String, Object> payload = Map.of(
                "action", "ENGAGER_BUDGET",
                "dossierId", dossierId.toString(),
                "ligneBudgetaireCode", ligneBudgetaire != null ? ligneBudgetaire : ""
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                dossierId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Engagement budgétaire soumis au Maker-Checker pour dossier {} (Ticket: {})", dossier.getNumeroDossier(), uuid);

        if (securityService.canValidate(Permissions.ENGAGE_BUDGET)) {
            if (ligneBudgetaire != null && !ligneBudgetaire.isBlank()) {
                dossier.setLigneBudgetaireCode(ligneBudgetaire);
            }
            dossier.setStatut(StatutMarcheDossier.ENGAGE_BUDGET);
            dossier = marcheDossierRepository.save(dossier);
            makerCheckerEngine.update(uuid, MakerCheckerStatus.ACCEPTED, "Auto-validation engagement budget", securityService.getUtilsService().getCurrentUserData().userId());
        }
        return MarcheDossierResponse.fromEntity(dossier);
    }

    @Transactional
    public MarcheDossierResponse signerDG(UUID dossierId) {
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        dossier.setStatut(StatutMarcheDossier.SIGNE);
        return MarcheDossierResponse.fromEntity(marcheDossierRepository.save(dossier));
    }

    @Transactional
    public MarcheDossierResponse notifierFournisseur(UUID dossierId, LocalDate dateNotification, Integer delaiJours) {
        securityService.checkPermission(Permissions.NOTIFY_PROVIDER);
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé pour notification prestataire : hors de votre structure.");
        }

        LocalDate dateNotif = dateNotification != null ? dateNotification : LocalDate.now();
        int delai = delaiJours != null ? delaiJours : (dossier.getDelaiLivraisonJours() != null ? dossier.getDelaiLivraisonJours() : GlobalConstants.DELAIS_LIVRAISON_PAR_DEFAULT.intValue());

        Map<String, Object> payload = Map.of(
                "action", "NOTIFIER_FOURNISSEUR",
                "dossierId", dossierId.toString(),
                "dateNotification", dateNotif.toString(),
                "delaiJours", delai
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.MARCHE_DOSSIER,
                dossierId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Notification fournisseur soumise au Maker-Checker pour dossier {} (Ticket: {})", dossier.getNumeroDossier(), uuid);

        if (securityService.canValidate(Permissions.NOTIFY_PROVIDER)) {
            dossier.setDateNotification(dateNotif);
            dossier.setDelaiLivraisonJours(delai);
            dossier.setDateLimiteLivraison(dateNotif.plusDays(delai));
            dossier.setStatut(StatutMarcheDossier.NOTIFIE);
            dossier = marcheDossierRepository.save(dossier);
            makerCheckerEngine.update(uuid, MakerCheckerStatus.ACCEPTED, "Auto-validation notification fournisseur", securityService.getUtilsService().getCurrentUserData().userId());
        }
        return MarcheDossierResponse.fromEntity(dossier);
    }

    private void updateMarcheDossier(Map<String, Object> payload, UUID dossierId) {
        MarcheDossierEntity dossier = getDossierOrThrow(dossierId);
        String action = (String) payload.get("action");
        if ("ENREGISTRER_PROFORMAS".equals(action)) {
            Object countObj = payload.get("proformasCount");
            if (countObj != null) {
                dossier.setProformasObtenuesCount(Integer.parseInt(countObj.toString()));
            }
            dossier.setStatut(StatutMarcheDossier.SELECTION_PRESTATAIRE);
        } else if ("ENGAGER_BUDGET".equals(action)) {
            Object lb = payload.get("ligneBudgetaireCode");
            if (lb != null && !lb.toString().isBlank()) {
                dossier.setLigneBudgetaireCode(lb.toString());
            }
            dossier.setStatut(StatutMarcheDossier.ENGAGE_BUDGET);
        } else if ("NOTIFIER_FOURNISSEUR".equals(action)) {
            Object dNotif = payload.get("dateNotification");
            if (dNotif != null) {
                dossier.setDateNotification(LocalDate.parse(dNotif.toString()));
            }
            Object delai = payload.get("delaiJours");
            if (delai != null) {
                dossier.setDelaiLivraisonJours(Integer.parseInt(delai.toString()));
            }
            if (dossier.getDateNotification() != null && dossier.getDelaiLivraisonJours() != null) {
                dossier.setDateLimiteLivraison(dossier.getDateNotification().plusDays(dossier.getDelaiLivraisonJours()));
            }
            dossier.setStatut(StatutMarcheDossier.NOTIFIE);
        }
        marcheDossierRepository.save(dossier);
        log.info("Dossier de marché mis à jour en base (Action: {}). PK: {}", action, dossierId);
    }

    private MarcheDossierEntity getDossierOrThrow(UUID id) {
        return marcheDossierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.ORDER_OR_CONTRACT_FILE_NOT_FOUND));
    }

    // ==========================================
    // RÉCEPTIONS TECHNIQUES & LIQUIDATION
    // ==========================================

    @Transactional(readOnly = true)
    public List<MarcheReceptionPvResponse> listerReceptions() {
        securityService.checkPermission(Permissions.LIST_RECEPTIONS);
        String scope = securityService.getScopeMaterializedPath();
        List<MarcheReceptionPvEntity> list;
        if (scope == null) {
            list = marcheReceptionPvRepository.findAll(Sort.by(Sort.Direction.DESC, "dateReception"));
        } else {
            list = marcheReceptionPvRepository.findByMaterializedPathStartingWith(scope);
        }
        return list.stream()
                .map(MarcheReceptionPvResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UUID enregistrerPvReception(EnregistrerPvReceptionRequest request) {
        securityService.checkPermission(Permissions.CREATE_RECEPTION);
        MarcheDossierEntity dossier = getDossierOrThrow(request.getDossierId());
        if (dossier.getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(dossier.getExpressionBesoin().getUniteOrganisationnelle(),
                    Permissions.CREATE_RECEPTION.name());
        }

        long count = marcheReceptionPvRepository.count() + 1;
        int annee = LocalDate.now().getYear();
        String numeroPv = String.format(GlobalConstants.PV_REC, annee, count);
        UUID id = UUID.randomUUID();
        String fichierUrl = minioService.uploadFile(request.getFichierPv(), "achats/receptions/" + id + "/pv_reception_" + System.currentTimeMillis() + "_" +numeroPv );
        PvEventInput pvEventInput = PvEventInput.builder()
                .id(id)
                .numeroPv(numeroPv)
                .dossierId(request.getDossierId())
                .dateReception(request.getDateReception())
                .lieuReception(request.getLieuReception())
                .resultatConformite(request.getResultatConformite())
                .observations(request.getObservations())
                .numeroFactureDefinitive(request.getNumeroFactureDefinitive())
                .montantFactureLiquide(request.getMontantFactureLiquide())
                .fichierUrl(fichierUrl)
                .build();
        Map<String, Object> payload = objectMapper.convertValue(pvEventInput, Map.class);
        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.MARCHE_RECEPTION_PV,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Procès-verbal de réception soumis au pattern Maker-Checker. PV {} pour le dossier {}", numeroPv, dossier.getNumeroDossier());
        if(securityService.canValidate(Permissions.VALIDATE_RECEPTION)) {
            log.info("Utilisateur admin, validation automatique du PV {} pour le dossier {}", numeroPv, dossier.getNumeroDossier());
            try {
                validateOrRejectPvReception(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur qui a creer",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique du PV {}: {}", numeroPv, e.getMessage());
            }
        }
        return uuid;
    }

    @Transactional
    public void validateOrRejectPvReception(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_RECEPTION);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.MARCHE_RECEPTION_PV,
                "Accès refusé pour validation/rejet du PV : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());

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
            log.info("Procès-verbal de réception rejeté par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID pvId = UUID.fromString(checkerOutput.entityPk());
            PvEventInput pvEventInput = objectMapper.convertValue(checkerOutput.payload(), PvEventInput.class);
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationPvReception(pvEventInput, pvId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Procès-verbal de réception approuvé par le Checker {}. Ticket: {}, PV PK: {}", checkerId, checkerOutput.id(), pvId);
        }
    }

    private MarcheReceptionPvEntity creationPvReception(PvEventInput input, UUID pvId) {
        MarcheDossierEntity dossier = getDossierOrThrow(input.dossierId());
        MarcheReceptionPvEntity entity = MarcheReceptionPvEntity.builder()
                .id(pvId)
                .numeroPv(input.numeroPv())
                .dossier(dossier)
                .dateReception(input.dateReception())
                .lieuReception(input.lieuReception())
                .resultatConformite(input.resultatConformite() != null ? input.resultatConformite() : ResultatConformitePv.CONFORME_SANS_RESERVE)
                .observations(input.observations())
                .numeroFactureDefinitive(input.numeroFactureDefinitive())
                .montantFactureLiquide(input.montantFactureLiquide())
                .fichierUrl(input.fichierUrl())
                .liquidationValidee(false)
                .build();
        MarcheReceptionPvEntity saved = marcheReceptionPvRepository.save(entity);

        dossier.setStatut(StatutMarcheDossier.RECEPTIONNE);
        marcheDossierRepository.save(dossier);

        log.info("Procès-verbal de réception matérialisé en base. PK: {}, N°: {}", pvId, input.numeroPv());
        return saved;
    }

    @Transactional
    public MarcheReceptionPvResponse liquiderFacture(UUID receptionPvId, String numeroFacture, BigDecimal montantLiquide) {
        securityService.checkPermission(Permissions.LIQUIDATE_INVOICE);
        MarcheReceptionPvEntity pv = marcheReceptionPvRepository.findById(receptionPvId)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.PV_REC_NOT_FOUND));

        if (pv.getDossier() != null && pv.getDossier().getExpressionBesoin() != null) {
            securityService.validateAccessToUnite(pv.getDossier().getExpressionBesoin().getUniteOrganisationnelle(),
                    "Accès refusé pour liquidation : hors périmètre hiérarchique.");
        }

        Map<String, Object> payload = Map.of(
                "receptionPvId", receptionPvId.toString(),
                "numeroFacture", numeroFacture != null ? numeroFacture : (pv.getNumeroFactureDefinitive() != null ? pv.getNumeroFactureDefinitive() : ""),
                "montantLiquide", montantLiquide != null ? montantLiquide.toString() : (pv.getMontantFactureLiquide() != null ? pv.getMontantFactureLiquide().toString() : "0")
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.LIQUIDATION_FACTURE,
                receptionPvId.toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );
        log.info("Liquidation de facture soumise au Maker-Checker: PV {} (Ticket: {})", pv.getNumeroPv(), uuid);

        if (securityService.canValidate(Permissions.VALIDATE_LIQUIDATION)) {
            log.info("Utilisateur habilité, validation automatique de la liquidation du PV {}", pv.getNumeroPv());
            try {
                validateOrRejectLiquidationFacture(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur liquidateur",
                        MakerCheckerOperationType.UPDATE
                ));
            } catch (Exception e) {
                log.warn("Erreur lors de la validation automatique de la liquidation du PV {}: {}", pv.getNumeroPv(), e.getMessage());
            }
        }

        return marcheReceptionPvRepository.findById(receptionPvId)
                .map(MarcheReceptionPvResponse::fromEntity)
                .orElseGet(() -> MarcheReceptionPvResponse.fromEntity(pv));
    }

    @Transactional
    public void validateOrRejectLiquidationFacture(ValidateOrRejectSubmissionInput input) {
        securityService.checkPermission(Permissions.VALIDATE_LIQUIDATION);
        securityService.validateAccessToUnite(input.id(), MakerCheckerEntityName.LIQUIDATION_FACTURE,
                "Accès refusé pour validation/rejet de liquidation : hors de votre périmètre hiérarchique.");
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING) {
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));
        }

        UUID checkerId = UUID.fromString(securityService.getUtilsService().getCurrentUserData().userId());
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
            log.info("Liquidation de facture rejetée par le Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID pvId = UUID.fromString(checkerOutput.entityPk());
            applyLiquidationFacture(checkerOutput.payload(), pvId);
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Liquidation de facture approuvée par le Checker {}. Ticket: {}, PV PK: {}", checkerId, checkerOutput.id(), pvId);
        }
    }

    private MarcheReceptionPvEntity applyLiquidationFacture(Map<String, Object> payload, UUID pvId) {
        MarcheReceptionPvEntity pv = marcheReceptionPvRepository.findById(pvId)
                .orElseThrow(() -> new IllegalArgumentException(GlobalConstants.PV_REC_NOT_FOUND));

        Object numFactureObj = payload.get("numeroFacture");
        if (numFactureObj != null && !numFactureObj.toString().isBlank()) {
            pv.setNumeroFactureDefinitive(numFactureObj.toString());
        }
        Object montantObj = payload.get("montantLiquide");
        if (montantObj != null && !montantObj.toString().isBlank()) {
            pv.setMontantFactureLiquide(new BigDecimal(montantObj.toString()));
        }
        pv.setLiquidationValidee(true);
        pv.setDateLiquidation(Instant.now());

        MarcheReceptionPvEntity savedPv = marcheReceptionPvRepository.save(pv);

        MarcheDossierEntity dossier = pv.getDossier();
        if (dossier != null) {
            dossier.setStatut(StatutMarcheDossier.LIQUIDE);
            marcheDossierRepository.save(dossier);
        }
        log.info("Liquidation matérialisée en base pour le PV {}", savedPv.getNumeroPv());
        return savedPv;
    }

    // ==========================================
    // ROUTING DES PROCÉDURES SELON SEUILS
    // ==========================================

    public TypeProcedureAchat determinerProcedure(BigDecimal montantTtc) {
        BigDecimal seuilDirect = configService.getDecimal(GlobalConstants.ACHATS_SEUIL_ACHAT_DIRECT, BigDecimal.valueOf(500_000));
        BigDecimal seuilBc = configService.getDecimal(GlobalConstants.ACHATS_SEUIL_BON_COMMAND, BigDecimal.valueOf(15_000_000));
        BigDecimal seuilLc = configService.getDecimal(GlobalConstants.ACHATS_SEUIL_LETTRE_COMMAND, BigDecimal.valueOf(50_000_000));

        if (montantTtc == null || montantTtc.compareTo(seuilDirect) < 0) {
            return TypeProcedureAchat.ACHAT_DIRECT;
        } else if (montantTtc.compareTo(seuilBc) < 0) {
            return TypeProcedureAchat.BON_COMMANDE;
        } else if (montantTtc.compareTo(seuilLc) < 0) {
            return TypeProcedureAchat.LETTRE_COMMANDE;
        } else {
            return TypeProcedureAchat.MARCHE_PUBLIC_CIPM;
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de calcul du hash SHA-256", e);
        }
    }
}
