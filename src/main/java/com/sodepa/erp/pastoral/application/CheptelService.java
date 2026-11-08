package com.sodepa.erp.pastoral.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.pastoral.infrastructure.entities.*;
import com.sodepa.erp.pastoral.infrastructure.repo.*;
import com.sodepa.erp.pastoral.presentation.requests.*;
import com.sodepa.erp.pastoral.presentation.responses.*;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `CheptelService` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheptelService {

    private final PastoralRanchRepository ranchRepository;
    private final PastoralCategorieCheptelRepository categorieRepository;
    private final PastoralBovinRepository bovinRepository;
    private final PastoralMouvementCheptelRepository mouvementRepository;
    private final PastoralHerdCompositionRepository herdCompositionRepository;
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

    // ==========================================
    // RANCHES & RÉFÉRENTIELS
    // ==========================================

    @Transactional(readOnly = true)
    public List<PastoralRanchResponse> listerRanches() {
        return ranchRepository.findAll(Sort.by(Sort.Direction.ASC, "nom"))
                .stream()
                .map(PastoralRanchResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PastoralCategorieCheptelResponse> listerCategories() {
        return categorieRepository.findAll()
                .stream()
                .map(PastoralCategorieCheptelResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================
    // ZOOTECHNIE DU CHEPTEL
    // ==========================================

    @Transactional(readOnly = true)
    public Page<PastoralBovinResponse> listerBovins(UUID ranchId, StatutBovin statut, Pageable pageable) {
        Page<PastoralBovinEntity> page;
        if (ranchId != null) {
            page = bovinRepository.findByRanchId(ranchId, pageable);
        } else if (statut != null) {
            page = bovinRepository.findByStatut(statut, pageable);
        } else {
            page = bovinRepository.findAll(pageable);
        }
        return page.map(PastoralBovinResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public PastoralBovinResponse obtenirBovin(UUID id) {
        return bovinRepository.findById(id)
                .map(PastoralBovinResponse::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Bovin introuvable: " + id));
    }

    @Transactional
    public PastoralBovinResponse enregistrerNaissance(EnregistrerNaissanceRequest request) {
        utilsService.hasPermission(Permissions.CREATE_BOVIN);
        PastoralRanchEntity ranch = ranchRepository.findById(request.getRanchId())
                .orElseThrow(() -> new IllegalArgumentException("Ranch introuvable: " + request.getRanchId()));

        PastoralCategorieCheptelEntity catVeau = categorieRepository.findByCode("VEAUX")
                .orElseThrow(() -> new IllegalStateException("Catégorie VEAUX introuvable dans le référentiel"));

        int annee = request.getDateNaissance().getYear();
        long count = bovinRepository.count() + 1;
        String codeRanch = ranch.getCode().replace("RANCH_", "");
        String masque = configService.getString("pastoral.matricule_masque", "SDP-%s-%d-%04d");
        String matricule = String.format(masque, codeRanch, annee, count);
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = new HashMap<>(objectMapper.convertValue(request, Map.class));
        payload.put("id", id.toString());
        payload.put("matriculeBoucle", matricule);

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.BOVIN_NAISSANCE,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Naissance bovin soumise au Maker-Checker: {} (Ticket: {})", matricule, uuid);

        trackClickHouse(MakerCheckerEntityName.BOVIN_NAISSANCE.name(), id.toString(), Map.of(
                "action", "SOUMISSION_ENREGISTRER_NAISSANCE",
                "matricule", matricule,
                "ranch", ranch.getNom(),
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_BOVIN)) {
            try {
                validateOrRejectBovin(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto naissance bovin: {}", e.getMessage());
            }
        }

        return bovinRepository.findById(id)
                .map(PastoralBovinResponse::fromEntity)
                .orElseGet(() -> PastoralBovinResponse.builder()
                        .id(id)
                        .matriculeBoucle(matricule)
                        .ranchId(ranch.getId())
                        .ranchNom(ranch.getNom())
                        .categorieCode("VEAUX")
                        .sexe(request.getSexe())
                        .race(request.getRace() != null ? request.getRace() : RaceBovine.GUDALI)
                        .dateNaissance(request.getDateNaissance())
                        .poidsActuelKg(request.getPoidsNaissanceKg() != null ? request.getPoidsNaissanceKg() : BigDecimal.valueOf(28.0))
                        .dateDernierePesee(request.getDateNaissance())
                        .mereBoucle(request.getMereBoucle())
                        .pereBoucle(request.getPereBoucle())
                        .etatSante(EtatSanteBovin.BON)
                        .statut(StatutBovin.VIVANT_AU_RANCH)
                        .valeurComptableActuelle(catVeau.getValeurForfaitaireBareme())
                        .build());
    }

    private PastoralBovinEntity creationNaissance(Map<String, Object> payload, UUID bovinId) {
        EnregistrerNaissanceRequest request = objectMapper.convertValue(payload, EnregistrerNaissanceRequest.class);
        String matricule = (String) payload.get("matriculeBoucle");

        PastoralRanchEntity ranch = ranchRepository.findById(request.getRanchId())
                .orElseThrow(() -> new IllegalArgumentException("Ranch introuvable: " + request.getRanchId()));

        PastoralCategorieCheptelEntity catVeau = categorieRepository.findByCode("VEAUX")
                .orElseThrow(() -> new IllegalStateException("Catégorie VEAUX introuvable dans le référentiel"));

        int annee = request.getDateNaissance().getYear();
        BigDecimal valeurInitiale = catVeau.getValeurForfaitaireBareme();
        BigDecimal poidsDefaut = configService.getDecimal("pastoral.poids_naissance_defaut", BigDecimal.valueOf(28.0));

        PastoralBovinEntity bovin = PastoralBovinEntity.builder()
                .id(bovinId)
                .matriculeBoucle(matricule)
                .ranch(ranch)
                .categorieCode("VEAUX")
                .sexe(request.getSexe())
                .race(request.getRace() != null ? request.getRace() : RaceBovine.GUDALI)
                .dateNaissance(request.getDateNaissance())
                .poidsActuelKg(request.getPoidsNaissanceKg() != null ? request.getPoidsNaissanceKg() : poidsDefaut)
                .dateDernierePesee(request.getDateNaissance())
                .mereBoucle(request.getMereBoucle())
                .pereBoucle(request.getPereBoucle())
                .etatSante(EtatSanteBovin.BON)
                .statut(StatutBovin.VIVANT_AU_RANCH)
                .dateEntree(request.getDateNaissance())
                .valeurComptableActuelle(valeurInitiale)
                .build();

        PastoralBovinEntity savedBovin = bovinRepository.save(bovin);

        long mvtCount = mouvementRepository.count() + 1;
        String cptDebitNaissance = configService.getString("pastoral.compte_debit_naissance", "2191");
        String cptCreditNaissance = configService.getString("pastoral.compte_credit_naissance", "7217");

        PastoralMouvementCheptelEntity mvt = PastoralMouvementCheptelEntity.builder()
                .id(UUID.randomUUID())
                .numeroMouvement(String.format("MVT-%d-%05d", annee, mvtCount))
                .bovin(savedBovin)
                .ranchSource(ranch)
                .typeMouvement(TypeMouvementCheptel.NAISSANCE)
                .dateMouvement(request.getDateNaissance())
                .poidsKg(savedBovin.getPoidsActuelKg())
                .nouvelleCategorie("VEAUX")
                .valeurMouvement(valeurInitiale)
                .compteDebitSyscohada(cptDebitNaissance)
                .compteCreditSyscohada(cptCreditNaissance)
                .justificatifRef("Fiche de velage " + matricule)
                .observations(request.getObservations() != null ? request.getObservations() : "Naissance veau ranch " + ranch.getNom())
                .creePar("Service Zootechnique")
                .dateCreation(Instant.now())
                .build();

        mouvementRepository.save(mvt);
        log.info("Naissance enregistrée au ranch {}: Veau bouclé {} (Valeur SYSCOHADA: {})", ranch.getNom(), matricule, valeurInitiale);

        return savedBovin;
    }

    @Transactional
    public void validateOrRejectBovin(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_BOVIN);
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
            log.info("Opération Bovin rejetée par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID bovinId = UUID.fromString(checkerOutput.entityPk());
            if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creationNaissance(checkerOutput.payload(), bovinId);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Opération Bovin approuvée par Checker {}. Ticket: {}, Bovin PK: {}", checkerId, checkerOutput.id(), bovinId);
        }
    }

    @Transactional
    public PastoralBovinResponse enregistrerPesee(EnregistrerPeseeRequest request) {
        PastoralBovinEntity bovin = getBovinOrThrow(request.getBovinId());
        LocalDate date = request.getDatePesee() != null ? request.getDatePesee() : LocalDate.now();

        bovin.setPoidsActuelKg(request.getPoidsKg());
        bovin.setDateDernierePesee(date);
        if (request.getEtatSante() != null) {
            bovin.setEtatSante(request.getEtatSante());
        }

        PastoralBovinEntity saved = bovinRepository.save(bovin);

        long mvtCount = mouvementRepository.count() + 1;
        PastoralMouvementCheptelEntity mvt = PastoralMouvementCheptelEntity.builder()
                .id(UUID.randomUUID())
                .numeroMouvement(String.format("PESEE-%d-%05d", date.getYear(), mvtCount))
                .bovin(saved)
                .ranchSource(bovin.getRanch())
                .typeMouvement(TypeMouvementCheptel.PESEE_CROISSANCE)
                .dateMouvement(date)
                .poidsKg(request.getPoidsKg())
                .observations(request.getObservations() != null ? request.getObservations() : "Pesée de suivi de croissance")
                .creePar("Vétérinaire de Ranch")
                .dateCreation(Instant.now())
                .build();

        mouvementRepository.save(mvt);
        return PastoralBovinResponse.fromEntity(saved);
    }

    @Transactional
    public PastoralBovinResponse enregistrerChangementCategorie(EnregistrerChangementCategorieRequest request) {
        PastoralBovinEntity bovin = getBovinOrThrow(request.getBovinId());
        String ancienneCat = bovin.getCategorieCode();
        String nouvelleCat = request.getNouvelleCategorieCode();

        PastoralCategorieCheptelEntity catDest = categorieRepository.findByCode(nouvelleCat)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie cheptel introuvable: " + nouvelleCat));

        LocalDate date = request.getDateChangement() != null ? request.getDateChangement() : LocalDate.now();
        BigDecimal nouvelleValeur = catDest.getValeurForfaitaireBareme();

        // Détermination des comptes SYSCOHADA selon le manuel SODEPA
        String cptDebit;
        String cptCredit;
        if ("VEAUX".equals(ancienneCat) && "GENISSES_TAURILLONS".equals(nouvelleCat)) {
            cptDebit = "2496"; // Entrée veaux pour génisse/taurillon
            cptCredit = "2191"; // Sortie veaux
        } else if ("GENISSES_TAURILLONS".equals(ancienneCat) && "REPRODUCTEURS".equals(nouvelleCat)) {
            cptDebit = "2462"; // Vaches / Taureaux reproducteurs
            cptCredit = "2496";
        } else if ("GENISSES_TAURILLONS".equals(ancienneCat) && "EMBOUCHE_REFORME".equals(nouvelleCat)) {
            cptDebit = "3880"; // Stock embouche
            cptCredit = "2496";
        } else if ("REPRODUCTEURS".equals(ancienneCat) && "EMBOUCHE_REFORME".equals(nouvelleCat)) {
            cptDebit = "3880"; // Réforme reproducteur
            cptCredit = "2462";
        } else {
            cptDebit = catDest.getCompteActifSyscohada();
            cptCredit = "7222";
        }

        bovin.setCategorieCode(nouvelleCat);
        bovin.setValeurComptableActuelle(nouvelleValeur);
        PastoralBovinEntity saved = bovinRepository.save(bovin);

        long mvtCount = mouvementRepository.count() + 1;
        PastoralMouvementCheptelEntity mvt = PastoralMouvementCheptelEntity.builder()
                .id(UUID.randomUUID())
                .numeroMouvement(String.format("CHG-CAT-%d-%05d", date.getYear(), mvtCount))
                .bovin(saved)
                .ranchSource(bovin.getRanch())
                .typeMouvement(TypeMouvementCheptel.CHANGEMENT_CATEGORIE)
                .dateMouvement(date)
                .poidsKg(saved.getPoidsActuelKg())
                .ancienneCategorie(ancienneCat)
                .nouvelleCategorie(nouvelleCat)
                .valeurMouvement(nouvelleValeur)
                .compteDebitSyscohada(cptDebit)
                .compteCreditSyscohada(cptCredit)
                .justificatifRef(request.getJustificatifRef() != null ? request.getJustificatifRef() : "PV de reclassement zootechnique")
                .observations(request.getObservations())
                .creePar("Responsable Pastoral")
                .dateCreation(Instant.now())
                .build();

        mouvementRepository.save(mvt);
        return PastoralBovinResponse.fromEntity(saved);
    }

    @Transactional
    public PastoralBovinResponse enregistrerTransfertRanch(EnregistrerTransfertRanchRequest request) {
        PastoralBovinEntity bovin = getBovinOrThrow(request.getBovinId());
        PastoralRanchEntity ranchSource = bovin.getRanch();
        PastoralRanchEntity ranchDest = ranchRepository.findById(request.getRanchDestinationId())
                .orElseThrow(() -> new IllegalArgumentException("Ranch destinataire introuvable: " + request.getRanchDestinationId()));

        LocalDate date = request.getDateTransfert() != null ? request.getDateTransfert() : LocalDate.now();

        bovin.setRanch(ranchDest);
        PastoralBovinEntity saved = bovinRepository.save(bovin);

        long mvtCount = mouvementRepository.count() + 1;
        PastoralMouvementCheptelEntity mvt = PastoralMouvementCheptelEntity.builder()
                .id(UUID.randomUUID())
                .numeroMouvement(String.format("TRF-%d-%05d", date.getYear(), mvtCount))
                .bovin(saved)
                .ranchSource(ranchSource)
                .ranchDestination(ranchDest)
                .typeMouvement(TypeMouvementCheptel.TRANSFERT_RANCH)
                .dateMouvement(date)
                .poidsKg(saved.getPoidsActuelKg())
                .nouvelleCategorie(saved.getCategorieCode())
                .valeurMouvement(saved.getValeurComptableActuelle())
                .justificatifRef(request.getJustificatifRef() != null ? request.getJustificatifRef() : "Laissez-passer sanitaire et bon de convoyage")
                .observations(request.getObservations() != null ? request.getObservations() : "Transfert inter-ranches de " + ranchSource.getNom() + " vers " + ranchDest.getNom())
                .creePar("Chef de Convoi")
                .dateCreation(Instant.now())
                .build();

        mouvementRepository.save(mvt);
        return PastoralBovinResponse.fromEntity(saved);
    }

    @Transactional
    public PastoralBovinResponse enregistrerSortie(EnregistrerSortieCheptelRequest request) {
        PastoralBovinEntity bovin = getBovinOrThrow(request.getBovinId());
        LocalDate date = request.getDateSortie() != null ? request.getDateSortie() : LocalDate.now();

        String cptDebit;
        String cptCredit;
        StatutBovin nouveauStatut;

        if (request.getTypeSortie() == TypeMouvementCheptel.MORTALITE) {
            nouveauStatut = StatutBovin.DECEDE;
            cptDebit = configService.getString("pastoral.compte_debit_mortalite", "8310"); // Perte, gratification, mortalité
            cptCredit = "VEAUX".equals(bovin.getCategorieCode()) ? "2191"
                    : "GENISSES_TAURILLONS".equals(bovin.getCategorieCode()) ? "2496"
                    : "REPRODUCTEURS".equals(bovin.getCategorieCode()) ? "2462" : "3880";
        } else if (request.getTypeSortie() == TypeMouvementCheptel.VENTE_ABATTAGE) {
            nouveauStatut = StatutBovin.ABATTU;
            cptDebit = "7360"; // Ventes et abattages animaux embouche / réforme
            cptCredit = "3880";
        } else {
            nouveauStatut = StatutBovin.REFORME;
            cptDebit = "3880";
            cptCredit = "2462";
        }

        bovin.setStatut(nouveauStatut);
        bovin.setDateSortie(date);
        PastoralBovinEntity saved = bovinRepository.save(bovin);

        long mvtCount = mouvementRepository.count() + 1;
        PastoralMouvementCheptelEntity mvt = PastoralMouvementCheptelEntity.builder()
                .id(UUID.randomUUID())
                .numeroMouvement(String.format("SORTIE-%d-%05d", date.getYear(), mvtCount))
                .bovin(saved)
                .ranchSource(bovin.getRanch())
                .typeMouvement(request.getTypeSortie())
                .dateMouvement(date)
                .poidsKg(saved.getPoidsActuelKg())
                .ancienneCategorie(saved.getCategorieCode())
                .valeurMouvement(request.getMontantCession() != null ? request.getMontantCession() : saved.getValeurComptableActuelle())
                .compteDebitSyscohada(cptDebit)
                .compteCreditSyscohada(cptCredit)
                .justificatifRef(request.getJustificatifRef() != null ? request.getJustificatifRef() : "Constat vétérinaire de sortie")
                .observations(request.getObservations())
                .creePar("Commission de Contrôle Pastoral")
                .dateCreation(Instant.now())
                .build();

        mouvementRepository.save(mvt);
        return PastoralBovinResponse.fromEntity(saved);
    }

    private PastoralBovinEntity getBovinOrThrow(UUID id) {
        return bovinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bovin introuvable: " + id));
    }

    // ==========================================
    // HERD COMPOSITION & TABLEAU DE BORD
    // ==========================================

    @Transactional(readOnly = true)
    public List<PastoralHerdCompositionResponse> listerHerdCompositions(Integer annee) {
        int an = annee != null ? annee : LocalDate.now().getYear();
        return herdCompositionRepository.findByAnneeExerciceOrderByDateInventaireDesc(an)
                .stream()
                .map(PastoralHerdCompositionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PastoralHerdCompositionResponse genererHerdComposition(UUID ranchId, Integer annee) {
        utilsService.hasPermission(Permissions.GENERATE_HERD_COMPOSITION);
        PastoralRanchEntity ranch = ranchRepository.findById(ranchId)
                .orElseThrow(() -> new IllegalArgumentException("Ranch introuvable: " + ranchId));

        int an = annee != null ? annee : LocalDate.now().getYear();
        UUID id = UUID.randomUUID();

        Map<String, Object> payload = Map.of(
                "id", id.toString(),
                "ranchId", ranchId.toString(),
                "annee", an
        );

        UUID uuid = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.HERD_COMPOSITION,
                id.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );
        log.info("Génération Herd Composition soumise au Maker-Checker pour ranch {} année {} (Ticket: {})", ranch.getNom(), an, uuid);

        trackClickHouse(MakerCheckerEntityName.HERD_COMPOSITION.name(), id.toString(), Map.of(
                "action", "SOUMISSION_GENERER_HERD_COMPOSITION",
                "ranch", ranch.getNom(),
                "annee", an,
                "ticket", uuid.toString()
        ));

        if (canValidate(Permissions.VALIDATE_HERD_COMPOSITION)) {
            try {
                validateOrRejectHerdComposition(new ValidateOrRejectSubmissionInput(
                        uuid,
                        MakerCheckerStatus.ACCEPTED,
                        "Validation automatique par l'utilisateur",
                        MakerCheckerOperationType.CREATE
                ));
            } catch (Exception e) {
                log.warn("Erreur validation auto herd composition: {}", e.getMessage());
            }
        }

        return herdCompositionRepository.findByRanchIdAndAnneeExercice(ranchId, an)
                .map(PastoralHerdCompositionResponse::fromEntity)
                .orElseGet(() -> PastoralHerdCompositionResponse.builder()
                        .id(id)
                        .ranchId(ranch.getId())
                        .ranchNom(ranch.getNom())
                        .anneeExercice(an)
                        .dateInventaire(LocalDate.now())
                        .statutRapprochement("EN_ATTENTE_VALIDATION")
                        .build());
    }

    private PastoralHerdCompositionEntity applyGenererHerdComposition(Map<String, Object> payload, UUID herdId) {
        UUID ranchId = UUID.fromString(payload.get("ranchId").toString());
        int an = Integer.parseInt(payload.get("annee").toString());

        PastoralRanchEntity ranch = ranchRepository.findById(ranchId)
                .orElseThrow(() -> new IllegalArgumentException("Ranch introuvable: " + ranchId));

        List<PastoralBovinEntity> vivants = bovinRepository.findByRanchIdAndStatut(ranchId, StatutBovin.VIVANT_AU_RANCH);

        int total = vivants.size();
        int veaux = 0;
        int genissesTaurillons = 0;
        int reproducteurs = 0;
        int emboucheReforme = 0;
        BigDecimal valeurTotale = BigDecimal.ZERO;

        for (PastoralBovinEntity b : vivants) {
            valeurTotale = valeurTotale.add(b.getValeurComptableActuelle());
            switch (b.getCategorieCode()) {
                case "VEAUX" -> veaux++;
                case "GENISSES_TAURILLONS" -> genissesTaurillons++;
                case "REPRODUCTEURS" -> reproducteurs++;
                case "EMBOUCHE_REFORME" -> emboucheReforme++;
            }
        }

        PastoralHerdCompositionEntity herd = herdCompositionRepository.findByRanchIdAndAnneeExercice(ranchId, an)
                .orElse(PastoralHerdCompositionEntity.builder()
                        .id(herdId)
                        .anneeExercice(an)
                        .ranch(ranch)
                        .build());

        herd.setDateInventaire(LocalDate.now());
        herd.setEffectifPhysiqueTotal(total);
        herd.setEffectifVeaux(veaux);
        herd.setEffectifGenissesTaurillons(genissesTaurillons);
        herd.setEffectifReproducteurs(reproducteurs);
        herd.setEffectifEmboucheReforme(emboucheReforme);
        herd.setValeurTotaleCheptel(valeurTotale);
        herd.setStatutRapprochement("CONFORME");
        herd.setObservations(String.format("Inventaire Herd Composition au 31 décembre %d certifié pour le ranch %s", an, ranch.getNom()));

        return herdCompositionRepository.save(herd);
    }

    @Transactional
    public void validateOrRejectHerdComposition(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_HERD_COMPOSITION);
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
            log.info("Herd Composition rejeté par Checker {}. Ticket: {}", checkerId, checkerOutput.id());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID herdId = UUID.fromString(checkerOutput.entityPk());
            applyGenererHerdComposition(checkerOutput.payload(), herdId);
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            log.info("Herd Composition approuvé par Checker {}. Ticket: {}, Herd PK: {}", checkerId, checkerOutput.id(), herdId);
        }
    }

    @Transactional(readOnly = true)
    public PastoralTableauBordResponse obtenirTableauBord() {
        long vivantsTotal = bovinRepository.countByStatut(StatutBovin.VIVANT_AU_RANCH);
        long decedesTotal = bovinRepository.countByStatut(StatutBovin.DECEDE);
        long abattusTotal = bovinRepository.countByStatut(StatutBovin.ABATTU) + bovinRepository.countByStatut(StatutBovin.VENDU);

        List<PastoralBovinEntity> tousVivants = bovinRepository.findAll().stream()
                .filter(b -> b.getStatut() == StatutBovin.VIVANT_AU_RANCH)
                .toList();

        BigDecimal valeurTotale = tousVivants.stream()
                .map(PastoralBovinEntity::getValeurComptableActuelle)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> parCat = new HashMap<>();
        parCat.put("VEAUX", tousVivants.stream().filter(b -> "VEAUX".equals(b.getCategorieCode())).count());
        parCat.put("GENISSES_TAURILLONS", tousVivants.stream().filter(b -> "GENISSES_TAURILLONS".equals(b.getCategorieCode())).count());
        parCat.put("REPRODUCTEURS", tousVivants.stream().filter(b -> "REPRODUCTEURS".equals(b.getCategorieCode())).count());
        parCat.put("EMBOUCHE_REFORME", tousVivants.stream().filter(b -> "EMBOUCHE_REFORME".equals(b.getCategorieCode())).count());

        List<PastoralRanchEntity> ranches = ranchRepository.findAll();
        List<PastoralTableauBordResponse.RanchStatResponse> stats = new ArrayList<>();

        for (PastoralRanchEntity r : ranches) {
            long eff = bovinRepository.countByRanchIdAndStatut(r.getId(), StatutBovin.VIVANT_AU_RANCH);
            int cap = r.getCapaciteTetes() != null && r.getCapaciteTetes() > 0 ? r.getCapaciteTetes() : 10000;
            BigDecimal taux = BigDecimal.valueOf(eff)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(cap), 2, RoundingMode.HALF_UP);

            stats.add(PastoralTableauBordResponse.RanchStatResponse.builder()
                    .ranchNom(r.getNom())
                    .region(r.getRegion())
                    .effectifActuel(eff)
                    .capaciteTetes(cap)
                    .tauxOccupationPourcent(taux)
                    .regiePlafondFcfa(r.getRegiePlafondFcfa())
                    .build());
        }

        return PastoralTableauBordResponse.builder()
                .totalTetesVivantes(vivantsTotal)
                .valeurEstimeeTotaleCheptel(valeurTotale)
                .totalNaissancesExercice(tousVivants.stream().filter(b -> b.getDateNaissance().getYear() == LocalDate.now().getYear()).count())
                .totalMortalitesExercice(decedesTotal)
                .totalVentesAbattagesExercice(abattusTotal)
                .ranchesStats(stats)
                .repartitionParCategorie(parCat)
                .build();
    }
}
