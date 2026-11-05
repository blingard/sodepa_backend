package com.sodepa.erp.paie.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.paie.infrastructure.entities.*;
import com.sodepa.erp.paie.infrastructure.repo.*;
import com.sodepa.erp.paie.presentation.requests.AjouterVariablePaieRequest;
import com.sodepa.erp.paie.presentation.requests.CreerPeriodePaieRequest;
import com.sodepa.erp.paie.presentation.responses.*;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaieService {

    private final PeriodePaieRepository periodeRepository;
    private final PrimeConfigRepository primeConfigRepository;
    private final VariablePaieRepository variableRepository;
    private final BulletinPaieRepository bulletinRepository;
    private final BulletinLigneRepository bulletinLigneRepository;
    private final EmployeRepository employeRepository;
    private final PaieCalculateurService calculateur;
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
    public List<PeriodePaieResponse> listerPeriodes() {
        return periodeRepository.findAll().stream()
                .sorted(Comparator.comparing(PeriodePaieEntity::getAnnee).reversed()
                        .thenComparing(Comparator.comparing(PeriodePaieEntity::getMois).reversed()))
                .map(this::mapToPeriodeResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PeriodePaieResponse getPeriodeById(UUID id) {
        PeriodePaieEntity p = periodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Période de paie introuvable: " + id));
        return mapToPeriodeResponse(p);
    }

    @Transactional
    public PeriodePaieResponse creerPeriode(CreerPeriodePaieRequest req) {
        utilsService.hasPermission(Permissions.CREATE_PERIODE_PAIE);
        String code = String.format("%d-%02d", req.getAnnee(), req.getMois());
        if (periodeRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("La période de paie " + code + " existe déjà.");
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());
        payload.put("code", code);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.PAIE_PERIODE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Période de paie soumise au Maker-Checker: {} (Ticket: {})", code, uuid);

        trackClickHouse(MakerCheckerEntityName.PAIE_PERIODE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_CREER_PERIODE_PAIE",
                "code", code,
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_PERIODE_PAIE)) {
            try {
                validateOrRejectPeriodePaie(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur créateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto période paie: {}", e.getMessage());
            }
        }

        YearMonth ym = YearMonth.of(req.getAnnee(), req.getMois());
        LocalDate debut = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        return periodeRepository.findById(id)
                .map(this::mapToPeriodeResponse)
                .orElseGet(() -> PeriodePaieResponse.builder()
                        .id(id)
                        .annee(req.getAnnee())
                        .mois(req.getMois())
                        .code(code)
                        .dateDebut(debut)
                        .dateFin(fin)
                        .statut(StatutPeriodePaie.OUVERTE)
                        .masseSalarialeNette(BigDecimal.ZERO)
                        .masseSalarialeBrute(BigDecimal.ZERO)
                        .totalChargesPatronales(BigDecimal.ZERO)
                        .coutGlobalSodepa(BigDecimal.ZERO)
                        .build());
    }

    private PeriodePaieEntity creationPeriodePaie(Map<String, Object> payload, UUID periodeId) {
        CreerPeriodePaieRequest req = objectMapper.convertValue(payload, CreerPeriodePaieRequest.class);
        String code = (String) payload.get("code");
        YearMonth ym = YearMonth.of(req.getAnnee(), req.getMois());
        LocalDate debut = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        PeriodePaieEntity entity = PeriodePaieEntity.builder()
                .id(periodeId)
                .annee(req.getAnnee())
                .mois(req.getMois())
                .code(code)
                .dateDebut(debut)
                .dateFin(fin)
                .statut(StatutPeriodePaie.OUVERTE)
                .masseSalarialeNette(BigDecimal.ZERO)
                .masseSalarialeBrute(BigDecimal.ZERO)
                .totalChargesPatronales(BigDecimal.ZERO)
                .coutGlobalSodepa(BigDecimal.ZERO)
                .build();

        return periodeRepository.save(entity);
    }

    @Transactional
    public void validateOrRejectPeriodePaie(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_PERIODE_PAIE);
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
            log.info("Période de paie rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID periodeId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationPeriodePaie(checkerOutput.payload(), periodeId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Période de paie approuvée par Checker {}. Ticket: {}, Période PK: {}", checkerId, checkerOutput.id(), periodeId);
        }
    }

    @Transactional
    public void ajouterVariable(UUID periodeId, AjouterVariablePaieRequest req) {
        utilsService.hasPermission(Permissions.CREATE_VARIABLE_PAIE);
        PeriodePaieEntity periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable: " + periodeId));

        EmployeEntity employe = employeRepository.findById(req.getEmployeId())
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable: " + req.getEmployeId()));

        UUID id = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(req, Map.class));
        payload.put("id", id.toString());
        payload.put("periodeId", periodeId.toString());

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.PAIE_VARIABLE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Variable de paie soumise au Maker-Checker pour {} (Ticket: {})", employe.getMatricule(), uuid);

        trackClickHouse(MakerCheckerEntityName.PAIE_VARIABLE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_AJOUTER_VARIABLE_PAIE",
                "matricule", employe.getMatricule(),
                "rubrique", req.getCodeRubrique(),
                "montant", req.getMontant(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_VARIABLE_PAIE)) {
            try {
                validateOrRejectVariablePaie(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto variable paie: {}", e.getMessage());
            }
        }
    }

    private VariablePaieEntity creationVariablePaie(Map<String, Object> payload, UUID variableId) {
        AjouterVariablePaieRequest req = objectMapper.convertValue(payload, AjouterVariablePaieRequest.class);
        UUID periodeId = UUID.fromString(payload.get("periodeId").toString());
        PeriodePaieEntity periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable: " + periodeId));

        EmployeEntity employe = employeRepository.findById(req.getEmployeId())
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable: " + req.getEmployeId()));

        VariablePaieEntity v = VariablePaieEntity.builder()
                .id(variableId)
                .periode(periode)
                .employe(employe)
                .codeRubrique(req.getCodeRubrique().trim())
                .libelle(req.getLibelle() != null ? req.getLibelle().trim() : req.getCodeRubrique())
                .montant(req.getMontant())
                .quantite(req.getQuantite() != null ? req.getQuantite() : BigDecimal.ONE)
                .commentaire(req.getCommentaire())
                .build();

        return variableRepository.save(v);
    }

    @Transactional
    public void validateOrRejectVariablePaie(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_VARIABLE_PAIE);
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
            log.info("Variable de paie rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID variableId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationVariablePaie(checkerOutput.payload(), variableId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Variable de paie approuvée par Checker {}. Ticket: {}, Variable PK: {}", checkerId, checkerOutput.id(), variableId);
        }
    }

    @Transactional
    public PeriodePaieResponse calculerPaie(UUID periodeId) {
        PeriodePaieEntity periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable: " + periodeId));

        if (periode.getStatut() == StatutPeriodePaie.CLOTUREE) {
            throw new IllegalStateException("Impossible de recalculer une période de paie clôturée.");
        }

        // Supprimer les anciens bulletins générés pour cette période
        List<BulletinPaieEntity> anciens = bulletinRepository.findByPeriodeId(periodeId);
        bulletinRepository.deleteAll(anciens);

        // Sélectionner le personnel actif SODEPA
        List<EmployeEntity> employes = employeRepository.findAll().stream()
                .filter(e -> e.getStatutActivite() == StatutActivite.EN_FONCTION ||
                             e.getStatutActivite() == StatutActivite.EN_ESSAI)
                .collect(Collectors.toList());

        List<VariablePaieEntity> variablesPeriode = variableRepository.findByPeriodeId(periodeId);
        Map<UUID, List<VariablePaieEntity>> varsParEmploye = variablesPeriode.stream()
                .collect(Collectors.groupingBy(v -> v.getEmploye().getId()));

        BigDecimal totalMasseNette = BigDecimal.ZERO;
        BigDecimal totalMasseBrute = BigDecimal.ZERO;
        BigDecimal totalChargesPat = BigDecimal.ZERO;
        BigDecimal totalCoutGlobal = BigDecimal.ZERO;

        for (EmployeEntity emp : employes) {
            BulletinPaieEntity bulletin = calculerBulletinPourEmploye(periode, emp, varsParEmploye.getOrDefault(emp.getId(), List.of()));
            bulletinRepository.save(bulletin);

            totalMasseNette = totalMasseNette.add(bulletin.getNetAPayer());
            totalMasseBrute = totalMasseBrute.add(bulletin.getSalaireBrutTaxable());
            totalChargesPat = totalChargesPat.add(bulletin.getTotalChargesPatronales());
            totalCoutGlobal = totalCoutGlobal.add(bulletin.getCoutGlobalEmployeur());
        }

        periode.setMasseSalarialeNette(totalMasseNette);
        periode.setMasseSalarialeBrute(totalMasseBrute);
        periode.setTotalChargesPatronales(totalChargesPat);
        periode.setCoutGlobalSodepa(totalCoutGlobal);
        periode.setStatut(StatutPeriodePaie.CALCULEE);
        periode.setDateCalcul(Instant.now());

        PeriodePaieEntity saved = periodeRepository.save(periode);
        log.info("Paie calculée pour {} agents sur la période {}. Masse nette: {} FCFA",
                employes.size(), periode.getCode(), totalMasseNette);

        return mapToPeriodeResponse(saved);
    }

    private BulletinPaieEntity calculerBulletinPourEmploye(
            PeriodePaieEntity periode,
            EmployeEntity emp,
            List<VariablePaieEntity> variables) {

        BigDecimal salaireBase = BigDecimal.ZERO;
        if (emp.getGrilleSalariale() != null && emp.getGrilleSalariale().getSalaireBase() != null) {
            salaireBase = emp.getGrilleSalariale().getSalaireBase();
        }

        List<BulletinLigneEntity> lignes = new ArrayList<>();
        int ordre = 1;

        // 1. Salaire de base (Gain)
        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("100_BASE")
                .libelle("Salaire de base indiciaire")
                .sens(SensRubrique.GAIN)
                .baseCalcul(salaireBase)
                .gain(salaireBase)
                .build());

        BigDecimal primesImposables = BigDecimal.ZERO;
        BigDecimal primesNonImposables = BigDecimal.ZERO;
        BigDecimal heuresSup = BigDecimal.ZERO;
        BigDecimal acomptes = BigDecimal.ZERO;

        // Primes conventionnelles SODEPA par défaut selon statut / poste
        // Prime de transport légale (non imposable, non cotisable) = 20 000 FCFA
        BigDecimal indemTransport = new BigDecimal("20000");
        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("200_TRANSPORT")
                .libelle("Indemnité légale de transport")
                .sens(SensRubrique.GAIN)
                .baseCalcul(indemTransport)
                .gain(indemTransport)
                .build());
        primesNonImposables = primesNonImposables.add(indemTransport);

        // Indemnité de logement si cadre ou maîtrise (Cat >= 7)
        int cat = (emp.getGrilleSalariale() != null && emp.getGrilleSalariale().getCategorie() != null)
                ? emp.getGrilleSalariale().getCategorie()
                : 1;
        String ech = (emp.getGrilleSalariale() != null && emp.getGrilleSalariale().getEchelon() != null)
                ? emp.getGrilleSalariale().getEchelon()
                : "A";
        if (cat >= 7) {
            BigDecimal indemLogement = new BigDecimal("35000");
            lignes.add(BulletinLigneEntity.builder()
                    .id(UUID.randomUUID())
                    .ordre(ordre++)
                    .codeRubrique("210_LOGEMENT")
                    .libelle("Indemnité compensatrice de logement")
                    .sens(SensRubrique.GAIN)
                    .baseCalcul(indemLogement)
                    .gain(indemLogement)
                    .build());
            primesImposables = primesImposables.add(indemLogement);
        }

        // Intégration des variables saisies pour le mois (primes, heures sup, acomptes)
        for (VariablePaieEntity v : variables) {
            if ("ACOMPTE".equalsIgnoreCase(v.getCodeRubrique())) {
                acomptes = acomptes.add(v.getMontant());
            } else if ("HEURES_SUP".equalsIgnoreCase(v.getCodeRubrique())) {
                heuresSup = heuresSup.add(v.getMontant());
                lignes.add(BulletinLigneEntity.builder()
                        .id(UUID.randomUUID())
                        .ordre(ordre++)
                        .codeRubrique("150_HEURES_SUP")
                        .libelle(v.getLibelle() != null ? v.getLibelle() : "Heures supplémentaires")
                        .sens(SensRubrique.GAIN)
                        .baseCalcul(v.getMontant())
                        .gain(v.getMontant())
                        .build());
            } else {
                primesImposables = primesImposables.add(v.getMontant());
                lignes.add(BulletinLigneEntity.builder()
                        .id(UUID.randomUUID())
                        .ordre(ordre++)
                        .codeRubrique("250_" + v.getCodeRubrique())
                        .libelle(v.getLibelle())
                        .sens(SensRubrique.GAIN)
                        .baseCalcul(v.getMontant())
                        .gain(v.getMontant())
                        .build());
            }
        }

        // Salaires bruts
        BigDecimal brutTaxable = salaireBase.add(primesImposables).add(heuresSup);
        BigDecimal brutCotisableCnps = calculateur.calculerAssietteCnps(salaireBase.add(primesImposables).add(heuresSup));

        // Retenues salariales
        BigDecimal cnpsSal = calculateur.calculerCnpsSalariale(brutCotisableCnps);
        BigDecimal cfcSal = calculateur.calculerCfcSalariale(brutTaxable);
        BigDecimal tc = calculateur.calculerTaxeCommunale(brutTaxable);
        BigDecimal irpp = calculateur.calculerIrpp(brutTaxable, cnpsSal);
        BigDecimal cac = calculateur.calculerCac(irpp);

        // Lignes de retenues
        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("500_CNPS_PVID_SAL")
                .libelle("CNPS - Régime Vieillesse, Invalidité & Décès (PVID 4.2%)")
                .sens(SensRubrique.RETENUE)
                .baseCalcul(brutCotisableCnps)
                .taux(PaieCalculateurService.TAUX_CNPS_PVID_SALARIAL)
                .retenue(cnpsSal)
                .build());

        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("510_CFC_SAL")
                .libelle("Crédit Foncier du Cameroun (CFC Salarial 1%)")
                .sens(SensRubrique.RETENUE)
                .baseCalcul(brutTaxable)
                .taux(PaieCalculateurService.TAUX_CFC_SALARIAL)
                .retenue(cfcSal)
                .build());

        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("520_TC")
                .libelle("Taxe de Développement Local / Taxe Communale")
                .sens(SensRubrique.RETENUE)
                .baseCalcul(brutTaxable)
                .retenue(tc)
                .build());

        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("530_IRPP")
                .libelle("Impôt sur le Revenu des Personnes Physiques (IRPP)")
                .sens(SensRubrique.RETENUE)
                .baseCalcul(brutTaxable)
                .retenue(irpp)
                .build());

        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("540_CAC")
                .libelle("Centimes Additionnels Communaux (CAC 10% IRPP)")
                .sens(SensRubrique.RETENUE)
                .baseCalcul(irpp)
                .taux(PaieCalculateurService.TAUX_CAC_SUR_IRPP)
                .retenue(cac)
                .build());

        if (acomptes.compareTo(BigDecimal.ZERO) > 0) {
            lignes.add(BulletinLigneEntity.builder()
                    .id(UUID.randomUUID())
                    .ordre(ordre++)
                    .codeRubrique("550_ACOMPTE")
                    .libelle("Retenue sur acompte ou avance quinzaine")
                    .sens(SensRubrique.RETENUE)
                    .baseCalcul(acomptes)
                    .retenue(acomptes)
                    .build());
        }

        // Charges patronales
        BigDecimal cnpsPatPvid = calculateur.calculerCnpsPatronalePvid(brutCotisableCnps);
        BigDecimal cnpsPatPf = calculateur.calculerCnpsPatronalePf(brutCotisableCnps);
        BigDecimal cnpsPatAt = calculateur.calculerCnpsPatronaleAt(brutCotisableCnps);
        BigDecimal cfcPat = calculateur.calculerCfcPatronale(brutTaxable);
        BigDecimal fnePat = calculateur.calculerFnePatronale(brutTaxable);

        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("600_CNPS_PAT")
                .libelle("Charges patronales CNPS (PVID 4.2% + PF 7% + AT 2.5%)")
                .sens(SensRubrique.CHARGE_PATRONALE)
                .baseCalcul(brutCotisableCnps)
                .chargePatronale(cnpsPatPvid.add(cnpsPatPf).add(cnpsPatAt))
                .build());

        lignes.add(BulletinLigneEntity.builder()
                .id(UUID.randomUUID())
                .ordre(ordre++)
                .codeRubrique("610_CFC_FNE_PAT")
                .libelle("Charges patronales État (CFC 1.5% + FNE 1%)")
                .sens(SensRubrique.CHARGE_PATRONALE)
                .baseCalcul(brutTaxable)
                .chargePatronale(cfcPat.add(fnePat))
                .build());

        BigDecimal totalGains = salaireBase.add(primesImposables).add(primesNonImposables).add(heuresSup);
        BigDecimal totalRetenuesSal = cnpsSal.add(cfcSal).add(tc).add(irpp).add(cac).add(acomptes);
        BigDecimal netAPayer = totalGains.subtract(totalRetenuesSal);
        BigDecimal totalChargesPat = cnpsPatPvid.add(cnpsPatPf).add(cnpsPatAt).add(cfcPat).add(fnePat);
        BigDecimal coutGlobalEmployeur = totalGains.add(totalChargesPat);

        BulletinPaieEntity bulletin = BulletinPaieEntity.builder()
                .id(UUID.randomUUID())
                .periode(periode)
                .employe(emp)
                .matricule(emp.getMatricule())
                .nomComplet(emp.getPrenom() + " " + emp.getNom())
                .structureNom(emp.getUniteOrganisationnelle() != null ? emp.getUniteOrganisationnelle().getIntitule() : "Direction Générale")
                .posteNom(emp.getPoste() != null ? emp.getPoste().getIntitule() : "Agent SODEPA")
                .categorie(cat)
                .echelon(ech)
                .numeroCnps(emp.getNumeroCnps())
                .modePaiement(emp.getCompteBancaireRib() != null && !emp.getCompteBancaireRib().isBlank() ? ModePaiement.VIREMENT : ModePaiement.BILLETAGE)
                .banqueNom(emp.getBanqueNom())
                .compteBancaireRib(emp.getCompteBancaireRib())
                .salaireBase(salaireBase)
                .sursalaire(BigDecimal.ZERO)
                .totalPrimesImposables(primesImposables)
                .totalPrimesNonImposables(primesNonImposables)
                .totalHeuresSup(heuresSup)
                .salaireBrutTaxable(brutTaxable)
                .salaireBrutCotisableCnps(brutCotisableCnps)
                .retenueIrpp(irpp)
                .retenueCac(cac)
                .retenueTc(tc)
                .retenueCfcSalariale(cfcSal)
                .retenueCnpsSalariale(cnpsSal)
                .retenueSyndicat(BigDecimal.ZERO)
                .acomptesAvances(acomptes)
                .autresRetenues(BigDecimal.ZERO)
                .totalRetenuesSalariales(totalRetenuesSal)
                .netAPayer(netAPayer)
                .chargeCnpsPvid(cnpsPatPvid)
                .chargeCnpsPf(cnpsPatPf)
                .chargeCnpsAt(cnpsPatAt)
                .chargeCfcPatronale(cfcPat)
                .chargeFnePatronale(fnePat)
                .totalChargesPatronales(totalChargesPat)
                .coutGlobalEmployeur(coutGlobalEmployeur)
                .statut("CALCULE")
                .dateCalcul(Instant.now())
                .build();

        for (BulletinLigneEntity l : lignes) {
            l.setBulletin(bulletin);
        }
        bulletin.setLignes(lignes);

        return bulletin;
    }

    @Transactional
    public PeriodePaieResponse validerPaie(UUID periodeId) {
        PeriodePaieEntity periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable: " + periodeId));

        if (periode.getStatut() == StatutPeriodePaie.OUVERTE) {
            throw new IllegalStateException("La paie doit d'abord être calculée avant validation.");
        }

        periode.setStatut(StatutPeriodePaie.VALIDEE);
        periode.setDateValidation(Instant.now());
        PeriodePaieEntity saved = periodeRepository.save(periode);

        log.info("Paie de la période {} VALIDÉE avec succès ! Prête pour engagement budgétaire et ordonnancement.", saved.getCode());
        return mapToPeriodeResponse(saved);
    }

    @Transactional
    public PeriodePaieResponse cloturerPeriode(UUID periodeId) {
        PeriodePaieEntity periode = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable: " + periodeId));

        periode.setStatut(StatutPeriodePaie.CLOTUREE);
        periode.setDateCloture(Instant.now());
        PeriodePaieEntity saved = periodeRepository.save(periode);

        log.info("Période de paie {} CLÔTURÉE définitivement.", saved.getCode());
        return mapToPeriodeResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<BulletinPaieResponse> getBulletins(UUID periodeId, Pageable pageable) {
        return bulletinRepository.findByPeriodeId(periodeId, pageable)
                .map(this::mapToBulletinResponse);
    }

    @Transactional(readOnly = true)
    public BulletinPaieResponse getBulletinById(UUID bulletinId) {
        BulletinPaieEntity b = bulletinRepository.findById(bulletinId)
                .orElseThrow(() -> new IllegalArgumentException("Bulletin de paie introuvable: " + bulletinId));
        return mapToBulletinResponse(b);
    }

    @Transactional(readOnly = true)
    public DipeDeclarationResponse genererDipe(UUID periodeId) {
        PeriodePaieEntity p = periodeRepository.findById(periodeId)
                .orElseThrow(() -> new IllegalArgumentException("Période introuvable: " + periodeId));

        List<BulletinPaieEntity> bulletins = bulletinRepository.findByPeriodeId(periodeId);

        BigDecimal totalBrut = BigDecimal.ZERO;
        BigDecimal totalCotisableCnps = BigDecimal.ZERO;
        BigDecimal totalCnpsSal = BigDecimal.ZERO;
        BigDecimal totalCnpsPat = BigDecimal.ZERO;
        BigDecimal totalIrpp = BigDecimal.ZERO;
        BigDecimal totalCac = BigDecimal.ZERO;
        BigDecimal totalTc = BigDecimal.ZERO;
        BigDecimal totalCfcSal = BigDecimal.ZERO;
        BigDecimal totalCfcPat = BigDecimal.ZERO;
        BigDecimal totalFnePat = BigDecimal.ZERO;

        List<DipeDeclarationResponse.LigneDipe> lignesDipe = new ArrayList<>();

        for (BulletinPaieEntity b : bulletins) {
            BigDecimal cnpsPat = b.getChargeCnpsPvid().add(b.getChargeCnpsPf()).add(b.getChargeCnpsAt());

            totalBrut = totalBrut.add(b.getSalaireBrutTaxable());
            totalCotisableCnps = totalCotisableCnps.add(b.getSalaireBrutCotisableCnps());
            totalCnpsSal = totalCnpsSal.add(b.getRetenueCnpsSalariale());
            totalCnpsPat = totalCnpsPat.add(cnpsPat);
            totalIrpp = totalIrpp.add(b.getRetenueIrpp());
            totalCac = totalCac.add(b.getRetenueCac());
            totalTc = totalTc.add(b.getRetenueTc());
            totalCfcSal = totalCfcSal.add(b.getRetenueCfcSalariale());
            totalCfcPat = totalCfcPat.add(b.getChargeCfcPatronale());
            totalFnePat = totalFnePat.add(b.getChargeFnePatronale());

            lignesDipe.add(DipeDeclarationResponse.LigneDipe.builder()
                    .matricule(b.getMatricule())
                    .numeroCnps(b.getNumeroCnps())
                    .nom(b.getEmploye() != null ? b.getEmploye().getNom() : "")
                    .prenom(b.getEmploye() != null ? b.getEmploye().getPrenom() : "")
                    .dateEmbauche(b.getEmploye() != null && b.getEmploye().getDateEmbauche() != null ? b.getEmploye().getDateEmbauche().toString() : "")
                    .categorie(b.getCategorie())
                    .echelon(b.getEchelon())
                    .salaireBrut(b.getSalaireBrutTaxable())
                    .salairePlafonneCnps(b.getSalaireBrutCotisableCnps())
                    .cnpsSalariale(b.getRetenueCnpsSalariale())
                    .cnpsPatronale(cnpsPat)
                    .irpp(b.getRetenueIrpp())
                    .cac(b.getRetenueCac())
                    .taxeCommunale(b.getRetenueTc())
                    .cfcSalarial(b.getRetenueCfcSalariale())
                    .netPercu(b.getNetAPayer())
                    .build());
        }

        return DipeDeclarationResponse.builder()
                .periodeId(p.getId())
                .periodeCode(p.getCode())
                .mois(p.getMois())
                .annee(p.getAnnee())
                .employeurRaisonSociale("Société de Développement et d'Exploitation des Productions Animales (SODEPA)")
                .employeurNumeroContribuable("M017400000000A")
                .employeurNumeroCnps("100-0001-A")
                .effectifTotal(bulletins.size())
                .totalBrutSalaires(totalBrut)
                .totalCotisableCnps(totalCotisableCnps)
                .totalCnpsSalariale(totalCnpsSal)
                .totalCnpsPatronale(totalCnpsPat)
                .totalCnpsGlobal(totalCnpsSal.add(totalCnpsPat))
                .totalIrpp(totalIrpp)
                .totalCac(totalCac)
                .totalTaxeCommunale(totalTc)
                .totalCfcSalarial(totalCfcSal)
                .totalCfcPatronal(totalCfcPat)
                .totalFnePatronal(totalFnePat)
                .totalRetenuesFiscales(totalIrpp.add(totalCac).add(totalTc).add(totalCfcSal))
                .lignes(lignesDipe)
                .build();
    }

    private PeriodePaieResponse mapToPeriodeResponse(PeriodePaieEntity p) {
        int nb = (p.getBulletins() != null) ? p.getBulletins().size() : (int) bulletinRepository.findByPeriodeId(p.getId()).size();
        return PeriodePaieResponse.builder()
                .id(p.getId())
                .mois(p.getMois())
                .annee(p.getAnnee())
                .code(p.getCode())
                .dateDebut(p.getDateDebut())
                .dateFin(p.getDateFin())
                .statut(p.getStatut())
                .masseSalarialeNette(p.getMasseSalarialeNette())
                .masseSalarialeBrute(p.getMasseSalarialeBrute())
                .totalChargesPatronales(p.getTotalChargesPatronales())
                .coutGlobalSodepa(p.getCoutGlobalSodepa())
                .nombreBulletins(nb)
                .dateCalcul(p.getDateCalcul())
                .dateValidation(p.getDateValidation())
                .dateCloture(p.getDateCloture())
                .build();
    }

    private BulletinPaieResponse mapToBulletinResponse(BulletinPaieEntity b) {
        List<BulletinLigneResponse> lignes = b.getLignes() != null
                ? b.getLignes().stream().map(this::mapToLigneResponse).collect(Collectors.toList())
                : bulletinLigneRepository.findByBulletinIdOrderByOrdreAsc(b.getId()).stream()
                    .map(this::mapToLigneResponse)
                    .collect(Collectors.toList());

        return BulletinPaieResponse.builder()
                .id(b.getId())
                .periodeId(b.getPeriode() != null ? b.getPeriode().getId() : null)
                .periodeCode(b.getPeriode() != null ? b.getPeriode().getCode() : null)
                .employeId(b.getEmploye() != null ? b.getEmploye().getId() : null)
                .matricule(b.getMatricule())
                .nomComplet(b.getNomComplet())
                .structureNom(b.getStructureNom())
                .posteNom(b.getPosteNom())
                .categorie(b.getCategorie())
                .echelon(b.getEchelon())
                .numeroCnps(b.getNumeroCnps())
                .modePaiement(b.getModePaiement())
                .banqueNom(b.getBanqueNom())
                .compteBancaireRib(b.getCompteBancaireRib())
                .salaireBase(b.getSalaireBase())
                .sursalaire(b.getSursalaire())
                .totalPrimesImposables(b.getTotalPrimesImposables())
                .totalPrimesNonImposables(b.getTotalPrimesNonImposables())
                .totalHeuresSup(b.getTotalHeuresSup())
                .salaireBrutTaxable(b.getSalaireBrutTaxable())
                .salaireBrutCotisableCnps(b.getSalaireBrutCotisableCnps())
                .retenueIrpp(b.getRetenueIrpp())
                .retenueCac(b.getRetenueCac())
                .retenueTc(b.getRetenueTc())
                .retenueCfcSalariale(b.getRetenueCfcSalariale())
                .retenueCnpsSalariale(b.getRetenueCnpsSalariale())
                .retenueSyndicat(b.getRetenueSyndicat())
                .acomptesAvances(b.getAcomptesAvances())
                .autresRetenues(b.getAutresRetenues())
                .totalRetenuesSalariales(b.getTotalRetenuesSalariales())
                .netAPayer(b.getNetAPayer())
                .chargeCnpsPvid(b.getChargeCnpsPvid())
                .chargeCnpsPf(b.getChargeCnpsPf())
                .chargeCnpsAt(b.getChargeCnpsAt())
                .chargeCfcPatronale(b.getChargeCfcPatronale())
                .chargeFnePatronale(b.getChargeFnePatronale())
                .totalChargesPatronales(b.getTotalChargesPatronales())
                .coutGlobalEmployeur(b.getCoutGlobalEmployeur())
                .statut(b.getStatut())
                .dateCalcul(b.getDateCalcul())
                .lignes(lignes)
                .build();
    }

    private BulletinLigneResponse mapToLigneResponse(BulletinLigneEntity l) {
        return BulletinLigneResponse.builder()
                .id(l.getId())
                .ordre(l.getOrdre())
                .codeRubrique(l.getCodeRubrique())
                .libelle(l.getLibelle())
                .sens(l.getSens())
                .baseCalcul(l.getBaseCalcul())
                .taux(l.getTaux())
                .gain(l.getGain())
                .retenue(l.getRetenue())
                .chargePatronale(l.getChargePatronale())
                .build();
    }
}
