package com.sodepa.erp.tresorerie.regies.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.tresorerie.regies.infrastructure.entities.*;
import com.sodepa.erp.tresorerie.regies.infrastructure.repo.*;
import com.sodepa.erp.tresorerie.regies.presentation.requests.*;
import com.sodepa.erp.tresorerie.regies.presentation.responses.*;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `RegiesTresorerieService` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegiesTresorerieService {

    private final TresorerieRegieRepository regieRepository;
    private final TresorerieRegieApprovisionnementRepository approvRepository;
    private final TresorerieRegieDepenseRepository depenseRepository;
    private final ParametreConfigurationService configService;
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

    @Transactional(readOnly = true)
    public List<TresorerieRegieResponse> listerRegies() {
        return regieRepository.findAll(Sort.by(Sort.Direction.ASC, "nom"))
                .stream()
                .map(TresorerieRegieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TresorerieRegieResponse obtenirRegie(UUID id) {
        return regieRepository.findById(id)
                .map(TresorerieRegieResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Régie introuvable: " + id));
    }

    @Transactional
    public TresorerieRegieApprovisionnementResponse creerDemandeApprovisionnement(CreerDemandeApprovisionnementRequest request) {
        utilsService.hasPermission(Permissions.CREATE_APPROVISIONNEMENT);
        TresorerieRegieEntity regie = regieRepository.findById(request.getRegieId())
                .orElseThrow(() -> new IllegalArgumentException("Régie introuvable: " + request.getRegieId()));

        long count = approvRepository.count() + 1;
        int annee = LocalDate.now().getYear();
        String numero = String.format("APPROV-%s-%d-%04d", regie.getCode().replace("REGIE_", ""), annee, count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("numeroDemande", numero);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.REGIE_APPROVISIONNEMENT,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Demande d'approvisionnement régie soumise au Maker-Checker: {} (Ticket: {})", numero, uuid);

        trackClickHouse(MakerCheckerEntityName.REGIE_APPROVISIONNEMENT.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_APPROVISIONNEMENT",
                "numeroDemande", numero,
                "regie", regie.getNom(),
                "montant", request.getMontantDemandeFcfa(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_APPROVISIONNEMENT)) {
            try {
                validateOrRejectApprovisionnement(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto approvisionnement: {}", e.getMessage());
            }
        }

        return approvRepository.findById(id)
                .map(TresorerieRegieApprovisionnementResponse::fromEntity)
                .orElseGet(() -> TresorerieRegieApprovisionnementResponse.builder()
                        .id(id)
                        .numeroDemande(numero)
                        .regieId(regie.getId())
                        .regieNom(regie.getNom())
                        .montantDemandeFcfa(request.getMontantDemandeFcfa())
                        .soldeAvantApprov(regie.getSoldeActuelFcfa())
                        .motifReapprovisionnement(request.getMotifReapprovisionnement())
                        .etatJustificatifsDepenses(request.getEtatJustificatifsDepenses())
                        .montantJustificatifsValides(request.getMontantJustificatifsValides() != null ? request.getMontantJustificatifsValides() : BigDecimal.ZERO)
                        .dateDemande(LocalDate.now())
                        .statut(StatutApprovisionnementRegie.EN_ATTENTE_DAF)
                        .signatureDaf(false)
                        .signatureDg(false)
                        .build());
    }

    private TresorerieRegieApprovisionnementEntity creationApprovisionnement(Map<String, Object> payload, UUID approvId) {
        UUID regieId = UUID.fromString(payload.get("regieId").toString());
        TresorerieRegieEntity regie = regieRepository.findById(regieId)
                .orElseThrow(() -> new IllegalArgumentException("Régie introuvable: " + regieId));

        String numeroDemande = (String) payload.get("numeroDemande");
        BigDecimal montantDemande = new BigDecimal(payload.get("montantDemandeFcfa").toString());
        String motif = (String) payload.get("motifReapprovisionnement");
        String justificatifs = (String) payload.get("etatJustificatifsDepenses");
        BigDecimal montantJustif = payload.get("montantJustificatifsValides") != null
                ? new BigDecimal(payload.get("montantJustificatifsValides").toString())
                : BigDecimal.ZERO;

        TresorerieRegieApprovisionnementEntity entity = TresorerieRegieApprovisionnementEntity.builder()
                .id(approvId)
                .numeroDemande(numeroDemande)
                .regie(regie)
                .montantDemandeFcfa(montantDemande)
                .soldeAvantApprov(regie.getSoldeActuelFcfa())
                .motifReapprovisionnement(motif)
                .etatJustificatifsDepenses(justificatifs)
                .montantJustificatifsValides(montantJustif)
                .dateDemande(LocalDate.now())
                .statut(StatutApprovisionnementRegie.EN_ATTENTE_DAF)
                .signatureDaf(false)
                .signatureDg(false)
                .build();

        return approvRepository.save(entity);
    }

    @Transactional
    public void validateOrRejectApprovisionnement(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_APPROVISIONNEMENT);
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
            log.info("Demande d'approvisionnement régie rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID approvId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationApprovisionnement(checkerOutput.payload(), approvId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Demande d'approvisionnement régie approuvée par Checker {}. Ticket: {}, Approv PK: {}", checkerId, checkerOutput.id(), approvId);
        }
    }

    @Transactional
    public TresorerieRegieApprovisionnementResponse signerDaf(UUID approvId) {
        TresorerieRegieApprovisionnementEntity entity = approvRepository.findById(approvId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'approvisionnement introuvable: " + approvId));

        entity.setSignatureDaf(true);
        entity.setDateSignatureDaf(Instant.now());
        entity.setStatut(StatutApprovisionnementRegie.EN_ATTENTE_DG);

        log.info("Approvisionnement {} visé par le DAF, en attente de la signature du Directeur Général", entity.getNumeroDemande());
        return TresorerieRegieApprovisionnementResponse.fromEntity(approvRepository.save(entity));
    }

    @Transactional
    public TresorerieRegieApprovisionnementResponse signerDgEtDebloquer(UUID approvId, String referenceVirement) {
        TresorerieRegieApprovisionnementEntity entity = approvRepository.findById(approvId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'approvisionnement introuvable: " + approvId));

        if (!entity.isSignatureDaf()) {
            throw new IllegalStateException("La double signature est obligatoire : le visa préalable du DAF est requis.");
        }

        entity.setSignatureDg(true);
        entity.setDateSignatureDg(Instant.now());
        entity.setReferenceVirementBanque(referenceVirement != null ? referenceVirement : "VIR-SODEPA-" + System.currentTimeMillis());
        entity.setDateEffetDeblocage(LocalDate.now());
        entity.setStatut(StatutApprovisionnementRegie.APPROUVE_APPROVISIONNE);

        // Créditer la caisse de régie
        TresorerieRegieEntity regie = entity.getRegie();
        regie.setSoldeActuelFcfa(regie.getSoldeActuelFcfa().add(entity.getMontantDemandeFcfa()));
        regieRepository.save(regie);

        log.info("Approvisionnement {} validé par le DG (Double signature confirmée). Caisse {} créditée de {}",
                entity.getNumeroDemande(), regie.getNom(), entity.getMontantDemandeFcfa());

        return TresorerieRegieApprovisionnementResponse.fromEntity(approvRepository.save(entity));
    }

    @Transactional
    public TresorerieRegieDepenseResponse enregistrerDepense(EnregistrerDepenseRegieRequest request) {
        utilsService.hasPermission(Permissions.CREATE_DEPENSE_REGIE);
        TresorerieRegieEntity regie = regieRepository.findById(request.getRegieId())
                .orElseThrow(() -> new IllegalArgumentException("Régie introuvable: " + request.getRegieId()));

        BigDecimal plafondDepense = configService.getDecimal("tresorerie.regie_plafond_menue_depense", BigDecimal.valueOf(100_000));
        if (request.getMontantDepenseFcfa().compareTo(plafondDepense) > 0) {
            throw new IllegalArgumentException(String.format("La dépense de %s FCFA excède le plafond légal autorisé pour les menues dépenses de régie (%s FCFA)",
                    request.getMontantDepenseFcfa(), plafondDepense));
        }

        if (regie.getSoldeActuelFcfa().compareTo(request.getMontantDepenseFcfa()) < 0) {
            throw new IllegalStateException(String.format("Solde insuffisant en caisse de régie (%s FCFA disponibles pour une dépense de %s FCFA)",
                    regie.getSoldeActuelFcfa(), request.getMontantDepenseFcfa()));
        }

        long count = depenseRepository.count() + 1;
        int annee = LocalDate.now().getYear();
        String numeroPiece = String.format("DEP-%s-%d-%05d", regie.getCode().replace("REGIE_", ""), annee, count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("numeroPiece", numeroPiece);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.REGIE_DEPENSE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Dépense régie soumise au Maker-Checker: {} (Ticket: {})", numeroPiece, uuid);

        trackClickHouse(MakerCheckerEntityName.REGIE_DEPENSE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_DEPENSE_REGIE",
                "numeroPiece", numeroPiece,
                "regie", regie.getNom(),
                "montant", request.getMontantDepenseFcfa(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_DEPENSE_REGIE)) {
            try {
                validateOrRejectDepense(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto dépense régie: {}", e.getMessage());
            }
        }

        return depenseRepository.findById(id)
                .map(TresorerieRegieDepenseResponse::fromEntity)
                .orElseGet(() -> TresorerieRegieDepenseResponse.builder()
                        .id(id)
                        .numeroPiece(numeroPiece)
                        .regieId(regie.getId())
                        .regieNom(regie.getNom())
                        .dateDepense(request.getDateDepense() != null ? request.getDateDepense() : LocalDate.now())
                        .beneficiaire(request.getBeneficiaire())
                        .motif(request.getMotif())
                        .imputationBudgetaire(request.getImputationBudgetaire())
                        .montantDepenseFcfa(request.getMontantDepenseFcfa())
                        .justificatifRef(request.getJustificatifRef() != null ? request.getJustificatifRef() : "Bon de caisse régie")
                        .justifie(true)
                        .build());
    }

    private TresorerieRegieDepenseEntity creationDepense(Map<String, Object> payload, UUID depenseId) {
        UUID regieId = UUID.fromString(payload.get("regieId").toString());
        TresorerieRegieEntity regie = regieRepository.findById(regieId)
                .orElseThrow(() -> new IllegalArgumentException("Régie introuvable: " + regieId));

        BigDecimal montant = new BigDecimal(payload.get("montantDepenseFcfa").toString());
        String numeroPiece = (String) payload.get("numeroPiece");
        String beneficiaire = (String) payload.get("beneficiaire");
        String motif = (String) payload.get("motif");
        String imputation = (String) payload.get("imputationBudgetaire");
        String justificatifRef = (String) payload.get("justificatifRef");

        TresorerieRegieDepenseEntity depense = TresorerieRegieDepenseEntity.builder()
                .id(depenseId)
                .numeroPiece(numeroPiece)
                .regie(regie)
                .dateDepense(LocalDate.now())
                .beneficiaire(beneficiaire)
                .motif(motif)
                .imputationBudgetaire(imputation)
                .montantDepenseFcfa(montant)
                .justificatifRef(justificatifRef != null ? justificatifRef : "Bon de caisse régie")
                .justifie(true)
                .build();

        regie.setSoldeActuelFcfa(regie.getSoldeActuelFcfa().subtract(montant));
        regieRepository.save(regie);

        return depenseRepository.save(depense);
    }

    @Transactional
    public void validateOrRejectDepense(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_DEPENSE_REGIE);
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
            log.info("Dépense régie rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID depenseId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationDepense(checkerOutput.payload(), depenseId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Dépense régie approuvée par Checker {}. Ticket: {}, Dépense PK: {}", checkerId, checkerOutput.id(), depenseId);
        }
    }

    @Transactional(readOnly = true)
    public List<TresorerieRegieApprovisionnementResponse> listerApprovisionnements(UUID regieId) {
        List<TresorerieRegieApprovisionnementEntity> list = regieId != null
                ? approvRepository.findByRegieIdOrderByDateDemandeDesc(regieId)
                : approvRepository.findAll(Sort.by(Sort.Direction.DESC, "dateDemande"));
        return list.stream().map(TresorerieRegieApprovisionnementResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TresorerieRegieDepenseResponse> listerDepenses(UUID regieId, Pageable pageable) {
        Page<TresorerieRegieDepenseEntity> page = regieId != null
                ? depenseRepository.findByRegieId(regieId, pageable)
                : depenseRepository.findAll(pageable);
        return page.map(TresorerieRegieDepenseResponse::fromEntity);
    }
}
