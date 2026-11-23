package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.sodepa.erp.comptabilite.generale.application.inputs.LigneInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SaisieEcritureInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SimulationTvaInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.SimulationTvaResponse;
import com.sodepa.erp.comptabilite.generale.application.usecase.EcritureValidatedEvent;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user.UserAdapterInterface;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.JournalRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.TiersRepository;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.StatutEcriture;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.workflow.application.WorkflowStateService;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.presentation.responses.WorkflowCurrentDataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Adaptateur pour la gestion des écritures comptables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcritureAdapter {

    private static final List<String> ECRITURE_WORKFLOW_ENTITY_NAMES = List.of("ECRITURE", "ECRITURE_COMPTABLE", "EcritureEntity");

    private final EcritureRepository ecritureRepository;
    private final JournalRepository journalRepository;
    private final CompteRepository compteRepository;
    private final TiersRepository tiersRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UtilsService utilsService;
    private final UserAdapterInterface userAdapterInterface;
    private final WorkflowService workflowService;
    private final WorkflowStateService workflowStateService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sodepa.erp.comptabilite.generale.infrastructure.repo.ExerciceComptableRepository exerciceComptableRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.sodepa.erp.comptabilite.generale.infrastructure.repo.PeriodeComptableRepository periodeComptableRepository;

    /**
     * Saisit une nouvelle écriture comptable en démarrant son circuit de validation workflow (ClickHouse State Store).
     * Zéro écriture anticipée dans les tables PostgreSQL `ecritures` / `lignes_ecriture` avant la validation finale.
     *
     * @param request les données de l'écriture
     * @return l'écriture en cours de validation (`enCoursDeModification = true`)
     */
    @Transactional
    public EcritureOutput saisirEcriture(SaisieEcritureInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_ECRITURE);
        JournalEntity journal = verifierEtPreparerSaisie(request);

        UUID ecritureId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", ecritureId.toString());
        payload.put("journalId", journal.getId().toString());
        payload.put("numeroPiece", request.numeroPiece());
        payload.put("libelle", request.libelle());
        payload.put("dateComptable", request.dateComptable() != null ? request.dateComptable().toString() : LocalDate.now().toString());
        payload.put("typeDevise", (request.typeDevise() != null ? request.typeDevise() : Devise.XAF).name());
        payload.put("tauxChange", (request.tauxChange() != null ? request.tauxChange() : BigDecimal.ONE).toString());

        List<Map<String, Object>> lignesPayload = new ArrayList<>();
        for (LigneInput lr : request.lignes()) {
            Map<String, Object> lMap = new LinkedHashMap<>();
            lMap.put("id", UUID.randomUUID().toString());
            lMap.put("compteCode", lr.compteCode());
            lMap.put("tiersId", lr.tiersId() != null ? lr.tiersId().toString() : null);
            lMap.put("debit", (lr.debit() != null ? lr.debit() : BigDecimal.ZERO).toString());
            lMap.put("credit", (lr.credit() != null ? lr.credit() : BigDecimal.ZERO).toString());
            lMap.put("libelleLigne", lr.libelleLigne() != null ? lr.libelleLigne() : request.libelle());
            lignesPayload.add(lMap);
        }
        payload.put("lignes", lignesPayload);

        workflowService.submitChange(
                MakerCheckerEntityName.ECRITURE,
                ecritureId.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );

        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(ECRITURE_WORKFLOW_ENTITY_NAMES, ecritureId);
        if (activeWfOpt.isPresent()) {
            return buildDraftEcritureFromWorkflow(ecritureId, activeWfOpt.get());
        }
        return buildDraftEcritureFromData(ecritureId, payload, null, List.of());
    }

    /**
     * Saisie directe en base PostgreSQL réservée au peuplement historique initial (`TestDataSeeder`).
     */
    @Transactional
    public EcritureOutput saisirEcritureDirectePourSeeder(SaisieEcritureInput request) {
        JournalEntity journal = verifierEtPreparerSaisie(request);
        EcritureEntity ecriture = EcritureEntity.builder()
                .id(UUID.randomUUID())
                .journal(journal)
                .numeroPiece(request.numeroPiece())
                .libelle(request.libelle())
                .dateComptable(request.dateComptable())
                .typeDevise(request.typeDevise() != null ? request.typeDevise() : Devise.XAF)
                .tauxChange(request.tauxChange() != null ? request.tauxChange() : BigDecimal.ONE)
                .valide(false)
                .statut(StatutEcriture.BROUILLON)
                .build();

        for (LigneInput lr : request.lignes()) {
            TiersEntity tiers = lr.tiersId() != null ? tiersRepository.findById(lr.tiersId()).orElse(null) : null;
            LigneEcritureEntity ligne = LigneEcritureEntity.builder()
                    .id(UUID.randomUUID())
                    .compteCode(lr.compteCode())
                    .tiers(tiers)
                    .debit(lr.debit() != null ? lr.debit() : BigDecimal.ZERO)
                    .credit(lr.credit() != null ? lr.credit() : BigDecimal.ZERO)
                    .libelleLigne(lr.libelleLigne() != null ? lr.libelleLigne() : request.libelle())
                    .build();
            ecriture.addLigne(ligne);
        }
        return mapToOutput(ecritureRepository.save(ecriture));
    }

    private JournalEntity verifierEtPreparerSaisie(SaisieEcritureInput request) {
        JournalEntity journal = journalRepository.findById(request.journalId())
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + request.journalId()));

        if (!Boolean.TRUE.equals(journal.getActif())) {
            throw new IllegalArgumentException("Le journal " + journal.getCode() + " est inactif.");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LigneInput lr : request.lignes()) {
            if (!compteRepository.existsByCode(lr.compteCode())) {
                throw new IllegalArgumentException("Le compte " + lr.compteCode() + " n'existe pas dans le plan comptable.");
            }
            if (lr.tiersId() != null && tiersRepository.findById(lr.tiersId()).isEmpty()) {
                throw new IllegalArgumentException("Tiers introuvable avec l'ID: " + lr.tiersId());
            }
            if (lr.debit() != null) totalDebit = totalDebit.add(lr.debit());
            if (lr.credit() != null) totalCredit = totalCredit.add(lr.credit());
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("L'écriture n'est pas équilibrée. Total Débit: " + totalDebit + ", Total Crédit: " + totalCredit);
        }

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de l'écriture doit être supérieur à zéro.");
        }

        LocalDate dateOperation = request.dateComptable() != null ? request.dateComptable() : LocalDate.now();
        if (exerciceComptableRepository != null) {
            exerciceComptableRepository.findByAnnee(dateOperation.getYear()).ifPresent(ex -> {
                if ("CLOTURE_DEFINITIF".equalsIgnoreCase(ex.getStatut())) {
                    throw new IllegalStateException("Saisie impossible : l'exercice comptable " + dateOperation.getYear() + " est définitivement clôturé.");
                }
            });
        }
        if (periodeComptableRepository != null) {
            String codePeriode = String.format("%d-%02d", dateOperation.getYear(), dateOperation.getMonthValue());
            periodeComptableRepository.findByCodePeriode(codePeriode).ifPresent(periode -> {
                if ("CLOTUREE".equalsIgnoreCase(periode.getStatut()) || "VERROUILLEE".equalsIgnoreCase(periode.getStatut())) {
                    throw new IllegalStateException("Saisie impossible : la période comptable " + codePeriode + " est " + periode.getStatut().toLowerCase() + ".");
                }
            });
        }
        return journal;
    }

    /**
     * Simule les lignes d'écriture comptable pour une opération soumise à la TVA.
     *
     * @param request la requête de simulation
     * @return la réponse de simulation
     */
    public SimulationTvaResponse simulerTva(SimulationTvaInput request) {
        BigDecimal montantTva = request.montantHt().multiply(request.tauxTva()).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal montantTtc = request.montantHt().add(montantTva);

        String compteTvaCode;
        String compteTiersCode;

        if (request.compteHtCode().startsWith("6")) {
            compteTvaCode = "445200";
            compteTiersCode = "401100";
        } else if (request.compteHtCode().startsWith("7")) {
            compteTvaCode = "443100";
            compteTiersCode = "411100";
        } else {
            compteTvaCode = "445000";
            compteTiersCode = "471000";
        }

        creerCompteSiAbsent(compteTvaCode, "TVA Déductible / Collectée");
        creerCompteSiAbsent(compteTiersCode, "Compte Collectif Tiers");

        return new SimulationTvaResponse(
                request.compteHtCode(),
                request.montantHt(),
                compteTvaCode,
                montantTva,
                compteTiersCode,
                montantTtc
        );
    }

    private void creerCompteSiAbsent(String code, String intitule) {
        if (!compteRepository.existsByCode(code)) {
            compteRepository.save(CompteEntity.builder()
                    .id(UUID.randomUUID())
                    .code(code)
                    .intitule(intitule)
                    .niveau(3)
                    .parentCode(code.substring(0, 2))
                    .nature("ATTENTE")
                    .isAuxiliaire(false)
                    .build());
        }
    }

    /**
     * Soumet une écriture brouillon pour validation.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture mise à jour
     */
    @Transactional
    public EcritureOutput soumettrePourValidation(UUID id) {
        Optional<EcritureEntity> opt = ecritureRepository.findById(id);
        if (opt.isPresent()) {
            EcritureEntity ecriture = opt.get();
            if (ecriture.getStatut() != StatutEcriture.BROUILLON) {
                throw new IllegalArgumentException("Seules les écritures au statut BROUILLON peuvent être soumises.");
            }
            ecriture.setStatut(StatutEcriture.SOUMIS);
            return mapToOutput(ecritureRepository.save(ecriture));
        }
        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(ECRITURE_WORKFLOW_ENTITY_NAMES, id);
        if (activeWfOpt.isPresent()) {
            return buildDraftEcritureFromWorkflow(id, activeWfOpt.get());
        }
        throw new IllegalArgumentException("Écriture introuvable avec l'ID: " + id);
    }

    /**
     * Valide définitivement une écriture soumise (ou matérialise l'écriture en cours de workflow ClickHouse).
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture validée
     */
    @Transactional
    public EcritureOutput validerEcriture(UUID id) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_ECRITURE);
        UUID validateurId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());

        Optional<EcritureEntity> opt = ecritureRepository.findById(id);
        if (opt.isPresent()) {
            EcritureEntity ecriture = opt.get();
            if (ecriture.getStatut() != StatutEcriture.SOUMIS && ecriture.getStatut() != StatutEcriture.BROUILLON) {
                throw new IllegalArgumentException("Seules les écritures au statut SOUMIS peuvent être validées.");
            }
            ecriture.validateEquilibre();
            ecriture.setValide(true);
            ecriture.setStatut(StatutEcriture.VALIDE);
            ecriture.setValidePar(validateurId);
            ecriture.setDateValidation(LocalDateTime.now());

            EcritureEntity saved = ecritureRepository.save(ecriture);
            eventPublisher.publishEvent(new EcritureValidatedEvent(this, saved));
            return mapToOutput(saved);
        }

        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(ECRITURE_WORKFLOW_ENTITY_NAMES, id);
        if (activeWfOpt.isPresent()) {
            WorkflowCurrentDataResponse current = workflowStateService.getCurrentData(activeWfOpt.get());
            EcritureEntity saved = materialiserEcritureDepuisSnapshot(id, current.getData(), validateurId);
            return mapToOutput(saved);
        }

        throw new IllegalArgumentException("Écriture introuvable avec l'ID: " + id);
    }

    /**
     * Rejette une écriture soumise.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture rejetée
     */
    @Transactional
    public EcritureOutput rejeterEcriture(UUID id) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_ECRITURE);

        Optional<EcritureEntity> opt = ecritureRepository.findById(id);
        if (opt.isPresent()) {
            EcritureEntity ecriture = opt.get();
            if (ecriture.getStatut() != StatutEcriture.SOUMIS && ecriture.getStatut() != StatutEcriture.BROUILLON) {
                throw new IllegalArgumentException("Seules les écritures au statut SOUMIS peuvent être rejetées.");
            }
            ecriture.setStatut(StatutEcriture.REJETE);
            return mapToOutput(ecritureRepository.save(ecriture));
        }
        return null;
    }

    /**
     * Valide ou rejette une soumission Maker-Checker / Workflow d'écriture comptable.
     * Sur ACCEPTED, matérialise l'écriture et ses lignes dans PostgreSQL avec l'état consolidé ClickHouse.
     * Sur REJECTED, n'écrit aucune ligne dans PostgreSQL.
     */
    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = workflowService.findById(input.id());
        if (checkerOutput.status() != MakerCheckerStatus.PENDING) {
            throw new IllegalArgumentException("La requête d'écriture n'est pas en attente.");
        }
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_ECRITURE);
        UUID checkerId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());
        if (checkerOutput.maker().id().equals(checkerId)) {
            throw new IllegalArgumentException("Le validateur doit être différent de l'initiateur.");
        }
        workflowService.validate(checkerOutput.id(), checkerOutput.createdAt(), checkerOutput.expiredAt());

        if (input.decision() == MakerCheckerStatus.ACCEPTED) {
            UUID ecritureId = UUID.fromString(checkerOutput.entityPk());
            materialiserEcritureDepuisSnapshot(ecritureId, checkerOutput.payload(), checkerId);
        }
        workflowService.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
    }

    @SuppressWarnings("unchecked")
    private EcritureEntity materialiserEcritureDepuisSnapshot(UUID ecritureId, Map<String, Object> data, UUID validateurId) {
        Optional<EcritureEntity> existingOpt = ecritureRepository.findById(ecritureId);
        if (existingOpt.isPresent()) {
            EcritureEntity existing = existingOpt.get();
            if (data != null) {
                if (data.get("libelle") != null) existing.setLibelle(data.get("libelle").toString());
                if (data.get("numeroPiece") != null) existing.setNumeroPiece(data.get("numeroPiece").toString());
            }
            existing.validateEquilibre();
            existing.setValide(true);
            existing.setStatut(StatutEcriture.VALIDE);
            existing.setValidePar(validateurId);
            existing.setDateValidation(LocalDateTime.now());
            EcritureEntity saved = ecritureRepository.save(existing);
            eventPublisher.publishEvent(new EcritureValidatedEvent(this, saved));
            return saved;
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Snapshot d'écriture comptable introuvable pour l'ID: " + ecritureId);
        }

        UUID journalId = UUID.fromString(data.get("journalId").toString());
        JournalEntity journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + journalId));

        LocalDate dateComptable = data.get("dateComptable") != null
                ? LocalDate.parse(data.get("dateComptable").toString())
                : LocalDate.now();
        Devise devise = Devise.XAF;
        if (data.get("typeDevise") != null) {
            try { devise = Devise.valueOf(data.get("typeDevise").toString()); } catch (Exception ignored) {}
        }
        BigDecimal tauxChange = data.get("tauxChange") != null
                ? new BigDecimal(data.get("tauxChange").toString())
                : BigDecimal.ONE;

        EcritureEntity ecriture = EcritureEntity.builder()
                .id(ecritureId)
                .journal(journal)
                .numeroPiece(data.get("numeroPiece") != null ? data.get("numeroPiece").toString() : "EC-" + ecritureId.toString().substring(0, 8))
                .libelle(data.get("libelle") != null ? data.get("libelle").toString() : "")
                .dateComptable(dateComptable)
                .typeDevise(devise)
                .tauxChange(tauxChange)
                .valide(true)
                .statut(StatutEcriture.VALIDE)
                .validePar(validateurId)
                .dateValidation(LocalDateTime.now())
                .build();

        Object rawLignes = data.get("lignes");
        if (rawLignes instanceof Collection<?> col) {
            for (Object item : col) {
                if (item instanceof Map<?, ?> lMap) {
                    String compteCode = lMap.get("compteCode") != null ? lMap.get("compteCode").toString() : "471000";
                    TiersEntity tiers = null;
                    if (lMap.get("tiersId") != null && !lMap.get("tiersId").toString().isBlank()) {
                        try {
                            tiers = tiersRepository.findById(UUID.fromString(lMap.get("tiersId").toString())).orElse(null);
                        } catch (Exception ignored) {}
                    }
                    BigDecimal debit = lMap.get("debit") != null ? new BigDecimal(lMap.get("debit").toString()) : BigDecimal.ZERO;
                    BigDecimal credit = lMap.get("credit") != null ? new BigDecimal(lMap.get("credit").toString()) : BigDecimal.ZERO;
                    String libLigne = lMap.get("libelleLigne") != null ? lMap.get("libelleLigne").toString() : ecriture.getLibelle();
                    UUID ligneId = UUID.randomUUID();
                    if (lMap.get("id") != null) {
                        try { ligneId = UUID.fromString(lMap.get("id").toString()); } catch (Exception ignored) {}
                    }
                    ecriture.addLigne(LigneEcritureEntity.builder()
                            .id(ligneId)
                            .compteCode(compteCode)
                            .tiers(tiers)
                            .debit(debit)
                            .credit(credit)
                            .libelleLigne(libLigne)
                            .build());
                }
            }
        }

        ecriture.validateEquilibre();
        EcritureEntity saved = ecritureRepository.save(ecriture);
        eventPublisher.publishEvent(new EcritureValidatedEvent(this, saved));
        return saved;
    }

    /**
     * Liste les écritures comptables (incluant celles en cours de validation dans ClickHouse avec `enCoursDeModification = true`).
     */
    @Transactional
    public List<EcritureOutput> listerEcritures() {
        List<EcritureEntity> list = ecritureRepository.findByOrderByDateComptableDesc(
                org.springframework.data.domain.PageRequest.of(0, 500));
        Map<UUID, WorkflowInstanceEntity> activeById = workflowStateService.findActiveInstancesByEntityUuid(ECRITURE_WORKFLOW_ENTITY_NAMES);
        Set<UUID> materializedIds = new HashSet<>();
        List<EcritureOutput> outputs = new ArrayList<>();

        for (EcritureEntity entity : list) {
            materializedIds.add(entity.getId());
            outputs.add(mapToOutput(entity));
        }

        for (Map.Entry<UUID, WorkflowInstanceEntity> entry : activeById.entrySet()) {
            if (materializedIds.contains(entry.getKey())) {
                continue;
            }
            outputs.add(buildDraftEcritureFromWorkflow(entry.getKey(), entry.getValue()));
        }
        return outputs;
    }

    /**
     * Récupère une écriture par son identifiant (y compris en cours de validation dans ClickHouse).
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture correspondante
     */
    @Transactional
    public EcritureOutput getEcritureById(UUID id) {
        Optional<EcritureEntity> opt = ecritureRepository.findById(id);
        Optional<WorkflowInstanceEntity> activeWfOpt = workflowStateService.findActiveInstanceForEntity(ECRITURE_WORKFLOW_ENTITY_NAMES, id);

        if (opt.isPresent()) {
            return mapToOutput(opt.get());
        }
        if (activeWfOpt.isPresent()) {
            return buildDraftEcritureFromWorkflow(id, activeWfOpt.get());
        }
        throw new IllegalArgumentException("Écriture introuvable avec l'ID: " + id);
    }

    private EcritureOutput buildDraftEcritureFromWorkflow(UUID id, WorkflowInstanceEntity wf) {
        WorkflowCurrentDataResponse current = workflowStateService.getCachedCurrentData(wf);
        Map<String, Object> data = current.getData() != null ? current.getData() : Map.of();
        List<String> attachments = current.getAttachments() == null ? List.of()
                : current.getAttachments().stream().map(m -> m.get("url") != null ? m.get("url").toString() : m.toString()).toList();
        return buildDraftEcritureFromData(id, data, wf, attachments);
    }

    private EcritureOutput buildDraftEcritureFromData(UUID id, Map<String, Object> data, WorkflowInstanceEntity wf, List<String> attachments) {
        JournalEntity journal = null;
        if (data.get("journalId") != null) {
            try {
                journal = journalRepository.findById(UUID.fromString(data.get("journalId").toString())).orElse(null);
            } catch (Exception ignored) {}
        }
        LocalDate dateComptable = LocalDate.now();
        if (data.get("dateComptable") != null) {
            try { dateComptable = LocalDate.parse(data.get("dateComptable").toString()); } catch (Exception ignored) {}
        }
        Devise devise = Devise.XAF;
        if (data.get("typeDevise") != null) {
            try { devise = Devise.valueOf(data.get("typeDevise").toString()); } catch (Exception ignored) {}
        }
        BigDecimal tauxChange = BigDecimal.ONE;
        if (data.get("tauxChange") != null) {
            try { tauxChange = new BigDecimal(data.get("tauxChange").toString()); } catch (Exception ignored) {}
        }
        List<LigneEcritureEntity> lignes = new ArrayList<>();
        Object rawLignes = data.get("lignes");
        if (rawLignes instanceof Collection<?> col) {
            for (Object item : col) {
                if (item instanceof Map<?, ?> lMap) {
                    UUID lId = UUID.randomUUID();
                    if (lMap.get("id") != null) {
                        try { lId = UUID.fromString(lMap.get("id").toString()); } catch (Exception ignored) {}
                    }
                    BigDecimal debit = lMap.get("debit") != null ? new BigDecimal(lMap.get("debit").toString()) : BigDecimal.ZERO;
                    BigDecimal credit = lMap.get("credit") != null ? new BigDecimal(lMap.get("credit").toString()) : BigDecimal.ZERO;
                    lignes.add(LigneEcritureEntity.builder()
                            .id(lId)
                            .compteCode(lMap.get("compteCode") != null ? lMap.get("compteCode").toString() : "")
                            .debit(debit)
                            .credit(credit)
                            .libelleLigne(lMap.get("libelleLigne") != null ? lMap.get("libelleLigne").toString() : "")
                            .build());
                }
            }
        }
        return new EcritureOutput(
                id,
                mapJournal(journal),
                data.get("numeroPiece") != null ? data.get("numeroPiece").toString() : "",
                data.get("libelle") != null ? data.get("libelle").toString() : "",
                dateComptable,
                LocalDateTime.now(),
                false,
                StatutEcriture.BROUILLON,
                null,
                null,
                devise,
                tauxChange,
                lignes,
                true,
                true,
                wf != null ? wf.getId() : null,
                wf != null && wf.getStatut() != null ? wf.getStatut().name() : "EN_COURS",
                "CREATE",
                attachments
        );
    }

    /**
     * Mappe une entité Ecriture en DTO Output.
     *
     * @param ecriture l'entité
     * @return le DTO Output
     */
    public EcritureOutput mapToOutput(EcritureEntity ecriture) {
        return new EcritureOutput(
                ecriture.getId(),
                mapJournal(ecriture.getJournal()),
                ecriture.getNumeroPiece(),
                ecriture.getLibelle(),
                ecriture.getDateComptable(),
                ecriture.getDateSaisie(),
                ecriture.getValide(),
                ecriture.getStatut(),
                ecriture.getValidePar() != null ? userAdapterInterface.getUserById(ecriture.getValidePar()) : null,
                ecriture.getDateValidation(),
                ecriture.getTypeDevise(),
                ecriture.getTauxChange(),
                ecriture.getLignes()
        );
    }

    /**
     * Mappe une entité Journal en DTO Output.
     *
     * @param journal l'entité
     * @return le DTO Output
     */
    public JournalOutput mapJournal(JournalEntity journal) {
        if (journal == null) return null;
        return JournalOutput.builder()
                .id(journal.getId())
                .code(journal.getCode())
                .intitule(journal.getIntitule())
                .typeJournal(journal.getTypeJournal())
                .actif(journal.getActif())
                .build();
    }
}
