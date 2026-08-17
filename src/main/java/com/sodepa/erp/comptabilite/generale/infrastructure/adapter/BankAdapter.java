package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateBankInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateBankInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.BankOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.BankSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.BanqueEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.event.BankEventInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.BanqueRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.share.FileStorageService;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier gérant le référentiel des banques partenaires.
 *
 * <p><b>Rôle dans le système :</b></p>
 * Permet de définir les banques physiques de l'entreprise et de les lier à leurs comptes de trésorerie généraux.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAdapter {
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final FileStorageService fileStorageService;
    private final BanqueRepository banqueRepository;
    private final CompteRepository compteRepository;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;
    private final static String FILE_PATH = "/bank/images";

    private final static int MAX_PAGE_SIZE = 100;

    /**
     * Enregistre une nouvelle banque partenaire dans le système.
     *
     * @param request les données de création de la banque.
     * @throws IllegalArgumentException si le code existe déjà ou si le compte n'est pas présent dans le plan.
     */
    @Transactional
    public void initCreateBank(CreateBankInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_BANK);
        if (banqueRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Le code banque " + request.code() + " est déjà utilisé.");
        }
        if (!compteRepository.existsByCode(request.accountAccountingCode())) {
            throw new IllegalArgumentException("Le compte de banque " + request.accountAccountingCode() + " n'existe pas dans le plan comptable.");
        }
        UUID entityPk = UUID.randomUUID();
        String logoPath = fileStorageService.storeFileImage(request.logo(), FILE_PATH);
        Map<String, Object> payload = toPayload(request, entityPk, logoPath, false);
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.BANQUE,
                entityPk.toString(),
                payload, MakerCheckerOperationType.CREATE
        );
    }

    /**
     * Récupère une banque par son identifiant unique.
     *
     * @param id l'identifiant de la banque.
     * @return la banque correspondante.
     */
    @Transactional(readOnly = true)
    public BankOutput getBankById(UUID id) {
        utilsService.hasPermission(Permissions.GET_FULL_BANK_INFO);
        BanqueEntity banqueEntity = banqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banque introuvable avec l'ID: " + id));

        return new BankOutput(banqueEntity.getId(), banqueEntity.getCode(), banqueEntity.getNom(),
                banqueEntity.getCompteComptableCode(), banqueEntity.getLogo(), banqueEntity.isStatus());
    }

    /**
     * Récupère une banque active par son identifiant unique.
     *
     * @param id l'identifiant de la banque.
     * @return la banque correspondante.
     */
    @Transactional(readOnly = true)
    public BankSmartOutput getActiveBankById(UUID id) {
        BanqueEntity banqueEntity = banqueRepository.findByIdAndStatusIsTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Banque introuvable avec l'ID: " + id));

        return new BankSmartOutput(banqueEntity.getId(), banqueEntity.getCode(), banqueEntity.getNom(),
                banqueEntity.getLogo());
    }

    private BanqueEntity getBank(UUID id){
        BanqueEntity banque = getNullableBank(id);
        if(Objects.isNull(banque))
            throw new IllegalArgumentException("Banque introuvable avec l'ID: " + id);
        return banque;
    }

    private BanqueEntity getNullableBank(UUID id){
        return banqueRepository.findById(id).orElse(null);
    }

    /**
     * Met à jour les informations d'une banque existante.
     * @param request les nouvelles données de la banque.
     */
    @Transactional
    public void updateBank(UpdateBankInput request) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BANK_INFO);
        UUID id = request.id();
        BanqueEntity banque = getBank(id);

        // Validation de l'unicité du code s'il a changé
        if (!banque.getCode().equals(request.code())) {
            throw new IllegalArgumentException("Erreur: vous ne pouvez pas modifier le code d'une banque.");
        }
        if (!banque.getLogo().equals(request.logo())) {
            throw new IllegalArgumentException("Erreur: vous ne pouvez pas modifier le logo depuis ce boutton.");
        }

        Map<String, Object> payload = toPayload(request, id);
        makerCheckerEngine.submitChange(MakerCheckerEntityName.BANQUE, id.toString(), payload, MakerCheckerOperationType.UPDATE
        );
    }

    /**
     * Met à jour le logo de la banque.
     *
     * @param id l'identifiant de la banque.
     * @param file l'image.
     */
    @Transactional
    public void updateBankImage(UUID id, MultipartFile file) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_BANK_INFO);
        BanqueEntity banque = getBank(id);

        String logoPath = fileStorageService.storeFileImage(file, FILE_PATH);

        UpdateBankInput updateBankInput = new UpdateBankInput(id, banque.getCode(), banque.getNom(), banque.getCompteComptableCode(),
                logoPath, banque.isStatus());

        Map<String, Object> payload = toPayload(updateBankInput, id);
        makerCheckerEngine.submitChange(MakerCheckerEntityName.BANQUE, id.toString(), payload, MakerCheckerOperationType.UPDATE_IMAGE
        );
    }

    /**
     * Récupère la liste de toutes les banques enregistrées et active.
     *
     * @return la liste des banques.
     */
    @Transactional(readOnly = true)
    public Set<BankSmartOutput> listAllActiveBank() {
        return banqueRepository.findAllByStatusIsTrueOrderByNomAsc().stream().map(this::map).collect(Collectors.toSet());
    }

    /**
     * Récupère la liste de toutes les banques enregistrées par page.
     *
     * @return la liste des banques.
     */
    @Transactional(readOnly = true)
    public PageRecord<BankSmartOutput> getBankByPage(Pageable pageable) {

        utilsService.hasPermission(Permissions.GET_FULL_BANK_INFO);
        if(pageable.getPageSize() > MAX_PAGE_SIZE)
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    MAX_PAGE_SIZE,
                    pageable.getSort()
            );
        Page<BanqueEntity> banqueEntityPage = banqueRepository.findAll(pageable);

        boolean paged = banqueEntityPage.getPageable().isPaged();
        return new PageRecord<>(
                banqueEntityPage.getContent().stream().map(this::map).toList(),
                banqueEntityPage.isEmpty(),
                banqueEntityPage.isFirst(),
                banqueEntityPage.isLast(),
                banqueEntityPage.getNumber(),
                banqueEntityPage.getNumberOfElements(),
                PageableRecord.builder()
                        .offset(paged ? banqueEntityPage.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? banqueEntityPage.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? banqueEntityPage.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(banqueEntityPage.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                banqueEntityPage.getSize(),
                banqueEntityPage.getSort(),
                banqueEntityPage.getTotalElements(),
                banqueEntityPage.getTotalPages()
        );
    }

    /**
     * Valider, Rejeter ou Expirer une demande.
     */
    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        MakerCheckerOutput checkerOutput = makerCheckerEngine.findById(input.id());
        MakerCheckerStatus currentStatus = checkerOutput.status();
        if (currentStatus != MakerCheckerStatus.PENDING)
            throw new RuntimeException(String.format(
                    "Invalid transition: Cannot transition from %s to %s.", currentStatus, input.decision()));

        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_BANK_INFO);

        UUID checkerId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());

        if (checkerOutput.maker().id().equals(checkerId))
            throw new RuntimeException("A checker cannot vote on their own request.");

        MakerCheckerStatus newStatus;
        if (MakerCheckerStatus.ACCEPTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.ACCEPTED;
        } else if (MakerCheckerStatus.REJECTED.equals(input.decision())) {
            newStatus = MakerCheckerStatus.REJECTED;
        } else {
            throw new RuntimeException("Invalid decision: " + input.decision());
        }
        validation(checkerOutput);
        if(MakerCheckerStatus.REJECTED.equals(newStatus)){
            makerCheckerEngine.update(checkerOutput.id(), newStatus, input.notes(), checkerId.toString());
        } else if (MakerCheckerStatus.ACCEPTED.equals(newStatus)) {
            UUID bankId = UUID.fromString(checkerOutput.entityPk());
            BankEventInput bankEventInput = objectMapper.convertValue(checkerOutput.payload(), BankEventInput.class);
            if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(bankEventInput, bankId);
            }else if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                update(bankEventInput);
            } else if (checkerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE_IMAGE) {
                updateLogo(bankEventInput);
            }
            makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
        }

    }


    private void validation(MakerCheckerOutput request) {
        makerCheckerEngine.validate(request.id(), request.createdAt(), request.expiredAt());
        BanqueEntity banqueEntity = getNullableBank(request.id());
        if ((request.checkerOperationType() == MakerCheckerOperationType.CREATE) && (Objects.nonNull(banqueEntity))) {
            throw new RuntimeException("You cannot create a Bank who already exist");
        } else if ((request.checkerOperationType() == MakerCheckerOperationType.UPDATE) && (Objects.isNull(banqueEntity))) {
            throw new RuntimeException("You cannot update a bank who not exist");
        }
    }




    private void update(BankEventInput input) {
        BanqueEntity entity = getBank(input.id());
        entity.setStatus(input.status());
        entity.setCompteComptableCode(input.compteComptableCode());
        entity.setNom(input.nom());
        banqueRepository.save(entity);
        log.info("Processing approved Bank update. PK: {}", input.id());
    }

    private void updateLogo(BankEventInput input) {
        BanqueEntity entity = getBank(input.id());
        entity.setLogo(input.logo());
        banqueRepository.save(entity);
        log.info("Processing approved Bank update Image. PK: {}", input.id());
    }



    private void creation(BankEventInput input, UUID bankId) {
        BanqueEntity entity = BanqueEntity.builder().id(bankId).code(input.code()).nom(input.nom())
                .compteComptableCode(input.compteComptableCode()).logo(input.logo()).status(input.status())
                .build();
        banqueRepository.save(entity);
        log.info("Processing approved Bank creation. PK: {}", bankId);
    }




    private BankSmartOutput map(BanqueEntity entity){
        return new BankSmartOutput(entity.getId(), entity.getCode(), entity.getNom(), entity.getLogo());
    }

    private Map<String, Object> toPayload(CreateBankInput input, UUID entityPk, String logo, boolean status) {
        BankEventInput event = new BankEventInput(entityPk, input.code(), input.name(), input.accountAccountingCode(),
                logo, status, utilsService.getCurrentUser().getUserData().get().userId());
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }

    private Map<String, Object> toPayload(UpdateBankInput input, UUID entityPk) {
        BankEventInput event = new BankEventInput(entityPk, input.code(), input.name(), input.accountingCode(),
                input.logo(), input.status(), utilsService.getCurrentUser().getUserData().get().userId());
        return objectMapper.convertValue(event, new TypeReference<>() {});
    }
}
