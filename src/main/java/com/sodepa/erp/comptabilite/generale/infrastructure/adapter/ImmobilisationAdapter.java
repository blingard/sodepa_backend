package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.GenerateAmortisationInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.RechercheImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateImmoInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.AmortissementLineOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ImmoOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ImmobilisationEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.event.ImmoEventInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.ImmobilisationRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.JournalRepository;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.CodeJournal;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.ModeAmortissement;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.PageableRecord;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.StatutImmobilisation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptateur gérant la persistance et les calculs pour les immobilisations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImmobilisationAdapter {

    private final static int MAX_PAGE_SIZE = 100;

    /**
     * Préfixe des clés d'entité des campagnes d'amortissement.
     *
     * <p>
     * Une campagne ne vise aucun bien en particulier : elle porte sur un compte
     * d'immobilisation entier. Faute d'identifiant d'entité à lui donner, elle
     * est déposée sous une clé technique — c'est ce préfixe qui, à la
     * validation, la distingue d'une modification de fiche, les deux étant
     * soumises en {@code UPDATE}.
     * </p>
     */
    private final static String PREFIXE_CAMPAGNE = "SYSTEM-";

    private final MakerCheckerEnginePort makerCheckerEngine;
    private final ImmobilisationRepository immobilisationRepository;
    private final EcritureRepository ecritureRepository;
    private final JournalRepository journalRepository;
    private final CompteRepository compteRepository;
    private final ObjectMapper objectMapper;
    private final UtilsService utilsService;

    /**
     * Initialise la création d'une immobilisation (workflow Maker-Checker).
     *
     * @param request les données de création.
     * @return les deux identifiants dont le client a besoin ensuite : celui de
     *         la demande, pour la trancher, et celui que portera le bien une
     *         fois la demande acceptée.
     */
    public SubmissionOutput initCreateImmo(CreateImmoInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_IMMOBILISATION);

        if (immobilisationRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("Une immobilisation avec le code " + request.code() + " existe déjà.");
        }

        UUID entityPk = UUID.randomUUID();
        Map<String, Object> payload = toPayload(request, entityPk);

        UUID requestId = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.IMMOBILISATION,
                entityPk.toString(),
                payload,
                MakerCheckerOperationType.CREATE
        );

        return new SubmissionOutput(requestId, entityPk);
    }

    /**
     * Initialise la modification d'une immobilisation (workflow Maker-Checker).
     *
     * <p>
     * La demande porte l'état complet voulu, pas un delta : c'est ce que le
     * moteur maker-checker sait rejouer, et cela évite qu'une acceptation
     * tardive n'applique une modification partielle sur un bien qui a changé
     * entre-temps.
     * </p>
     *
     * @param request l'état voulu du bien.
     * @return les identifiants de la demande et du bien visé.
     */
    public SubmissionOutput initUpdateImmo(UpdateImmoInput request) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_IMMOBILISATION);

        ImmobilisationEntity existante = immobilisationRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable"));

        // Le code reste unique : on ne le refuse que s'il appartient déjà à un
        // *autre* bien, sinon toute modification sans changement de code
        // échouerait contre elle-même.
        immobilisationRepository.findByCode(request.code()).ifPresent(homonyme -> {
            if (!homonyme.getId().equals(existante.getId())) {
                throw new IllegalArgumentException("Une immobilisation avec le code " + request.code() + " existe déjà.");
            }
        });

        Map<String, Object> payload = toPayload(request);

        UUID requestId = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.IMMOBILISATION,
                request.id().toString(),
                payload,
                MakerCheckerOperationType.UPDATE
        );

        return new SubmissionOutput(requestId, request.id());
    }

    /**
     * Récupère une immobilisation par son identifiant.
     * @param id l'identifiant.
     * @return les données de l'immobilisation.
     */
    public ImmoOutput getImmoById(UUID id) {
        utilsService.hasPermission(Permissions.GET_FULL_IMMOBILISATION_INFO);
        ImmobilisationEntity entity = immobilisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable"));

        return map(entity);
    }

    /**
     * Registre des immobilisations, paginé et filtré.
     *
     * <p>
     * Sans ce point d'entrée, une immobilisation n'était atteignable que par
     * son identifiant — lequel n'était rendu nulle part.
     * </p>
     *
     * @param input pagination, fragment de recherche et statut, tous facultatifs.
     * @return la page demandée.
     */
    @Transactional(readOnly = true)
    public PageRecord<ImmoOutput> getImmobilisationsByPage(RechercheImmoInput input) {
        utilsService.hasPermission(Permissions.GET_FULL_IMMOBILISATION_INFO);

        Pageable pageable = input.pageable();
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }

        String recherche = (input.recherche() == null || input.recherche().isBlank())
                ? null
                : input.recherche().toLowerCase();

        Page<ImmobilisationEntity> page = immobilisationRepository.rechercher(recherche, input.statut(), pageable);
        boolean paged = page.getPageable().isPaged();

        return new PageRecord<>(
                page.getContent().stream().map(this::map).toList(),
                page.isEmpty(),
                page.isFirst(),
                page.isLast(),
                page.getNumber(),
                page.getNumberOfElements(),
                PageableRecord.builder()
                        .offset(paged ? page.getPageable().getOffset() : 0L)
                        .pageNumber(paged ? page.getPageable().getPageNumber() : 0L)
                        .pageSize(paged ? page.getPageable().getPageSize() : 0L)
                        .paged(paged)
                        .sort(page.getPageable().getSort())
                        .unpaged(!paged)
                        .build(),
                page.getSize(),
                page.getSort(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * Obtient le plan d'amortissement prévisionnel.
     * @param id l'identifiant.
     * @return la liste des lignes d'amortissement.
     */
    public List<AmortissementLineOutput> getPlanAmortissement(UUID id) {
        ImmobilisationEntity immo = immobilisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable"));

        if (immo.getModeAmortissement() == ModeAmortissement.DEGRESSIF) {
            return calculerPlanDegressif(immo);
        } else {
            return calculerPlanLineaire(immo);
        }
    }

    /**
     * Initialise la génération des dotations aux amortissements (workflow Maker-Checker).
     *
     * @param request les paramètres.
     * @return l'identifiant de la demande. {@code entityId} est {@code null} :
     *         la campagne porte sur un compte, pas sur un bien.
     */
    public SubmissionOutput initGenerateAmortisation(GenerateAmortisationInput request) {
        utilsService.hasPermission(Permissions.INIT_UPDATE_IMMOBILISATION);

        Map<String, Object> payload = toPayload(request);

        UUID requestId = makerCheckerEngine.submitChange(
                MakerCheckerEntityName.IMMOBILISATION,
                PREFIXE_CAMPAGNE + UUID.randomUUID(),
                payload,
                MakerCheckerOperationType.UPDATE
        );

        return new SubmissionOutput(requestId, null);
    }

    /**
     * Demandes d'immobilisation en attente de décision.
     *
     * <p>
     * Les soumissions du demandeur lui-même sont écartées : la séparation
     * maker-checker lui interdit de les trancher, et {@code validateOrReject}
     * les refuse. Les lister quand même offrait un bouton qui ne pouvait
     * qu'échouer.
     * </p>
     */
    public PageRecord<MakerCheckerSmartOutput> findPendingRequests(Pageable pageable) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_IMMOBILISATION);

        String moi = utilsService.getCurrentUser().getUserData().get().userId();
        return makerCheckerEngine.findAllToValideByOther(
                pageable, MakerCheckerEntityName.IMMOBILISATION, MakerCheckerStatus.PENDING, moi);
    }

    /**
     * Valide ou rejette une demande en attente.
     * @param input la décision.
     */
    @Transactional
    public void validateOrReject(ValidateOrRejectSubmissionInput input) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_IMMOBILISATION);

        MakerCheckerOutput makerCheckerOutput = makerCheckerEngine.findById(input.id());
        
        if (makerCheckerOutput.status() != MakerCheckerStatus.PENDING) {
            throw new IllegalArgumentException("La requête n'est pas en attente.");
        }

        String currentUserId = String.valueOf(utilsService.getCurrentUser().getUserData().get().userId());
        String makerId = String.valueOf(makerCheckerOutput.maker().id());

        if (currentUserId.equals(makerId)) {
            throw new IllegalArgumentException("Le validateur doit être différent de l'initiateur.");
        }

        makerCheckerEngine.validate(input.id(), makerCheckerOutput.createdAt(), makerCheckerOutput.expiredAt());

        if (input.decision() == MakerCheckerStatus.ACCEPTED) {
            if (makerCheckerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                ImmoEventInput eventInput = objectMapper.convertValue(makerCheckerOutput.payload(), ImmoEventInput.class);
                creation(eventInput, UUID.fromString(makerCheckerOutput.entityPk()));
            } else if (makerCheckerOutput.checkerOperationType() == MakerCheckerOperationType.UPDATE) {
                // Deux demandes bien différentes voyagent en UPDATE : la campagne
                // d'amortissement d'un compte, déposée sous une clé technique, et
                // la modification d'une fiche, déposée sous l'identifiant du bien.
                if (makerCheckerOutput.entityPk().startsWith(PREFIXE_CAMPAGNE)) {
                    int annee = (Integer) makerCheckerOutput.payload().get("annee");
                    String compteImmoCode = (String) makerCheckerOutput.payload().get("compteImmoCode");
                    genererEcrituresAmortissement(annee, compteImmoCode);
                } else {
                    ImmoEventInput eventInput = objectMapper.convertValue(makerCheckerOutput.payload(), ImmoEventInput.class);
                    modification(eventInput, UUID.fromString(makerCheckerOutput.entityPk()));
                }
            }
        }

        makerCheckerEngine.update(input.id(), input.decision(), input.notes(), currentUserId);
    }

    private void creation(ImmoEventInput request, UUID id) {
        ImmobilisationEntity immo = ImmobilisationEntity.builder()
                .id(id)
                .code(request.code())
                .designation(request.designation())
                .valeurOrigine(request.valeurOrigine())
                .dateAcquisition(request.dateAcquisition())
                .dateMiseEnService(request.dateMiseEnService())
                .modeAmortissement(request.modeAmortissement())
                .dureeUtile(request.dureeUtile())
                .valeurResiduelle(request.valeurResiduelle() != null ? request.valeurResiduelle() : BigDecimal.ZERO)
                .statut(request.statut())
                .amortissementCumule(BigDecimal.ZERO)
                .build();
        immobilisationRepository.save(immo);
    }

    /**
     * Applique une modification acceptée.
     *
     * <p>
     * L'amortissement cumulé n'est pas touché : il est le reflet des dotations
     * déjà comptabilisées, et le réécrire ici décrocherait la fiche des
     * écritures qui la justifient.
     * </p>
     */
    private void modification(ImmoEventInput request, UUID id) {
        ImmobilisationEntity immo = immobilisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Immobilisation introuvable"));

        immo.setCode(request.code());
        immo.setDesignation(request.designation());
        immo.setValeurOrigine(request.valeurOrigine());
        immo.setDateAcquisition(request.dateAcquisition());
        immo.setDateMiseEnService(request.dateMiseEnService());
        immo.setModeAmortissement(request.modeAmortissement());
        immo.setDureeUtile(request.dureeUtile());
        immo.setValeurResiduelle(request.valeurResiduelle() != null ? request.valeurResiduelle() : BigDecimal.ZERO);
        immo.setStatut(request.statut());

        immobilisationRepository.save(immo);
    }

    private ImmoOutput map(ImmobilisationEntity entity) {
        return ImmoOutput.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .designation(entity.getDesignation())
                .valeurOrigine(entity.getValeurOrigine())
                .dateAcquisition(entity.getDateAcquisition())
                .dateMiseEnService(entity.getDateMiseEnService())
                .modeAmortissement(entity.getModeAmortissement())
                .dureeUtile(entity.getDureeUtile())
                .valeurResiduelle(entity.getValeurResiduelle())
                .statut(entity.getStatut())
                // Oublié jusqu'ici : la fiche repartait sans son cumul, et la
                // valeur nette comptable était donc incalculable côté client.
                .amortissementCumule(entity.getAmortissementCumule())
                .build();
    }

    private void genererEcrituresAmortissement(int annee, String compteImmoDefaut) {
        List<ImmobilisationEntity> actifs = immobilisationRepository.findAll();
        JournalEntity journalOD = journalRepository.findByCode(CodeJournal.OD)
                .orElseThrow(() -> new IllegalArgumentException("Journal des Opérations Diverses (OD) manquant."));

        LocalDate dateFinAnnee = LocalDate.of(annee, 12, 31);

        for (ImmobilisationEntity immo : actifs) {
            if (immo.getStatut() != StatutImmobilisation.ACTIVE) {
                continue;
            }

            List<AmortissementLineOutput> plan = immo.getModeAmortissement() == ModeAmortissement.DEGRESSIF ?
                    calculerPlanDegressif(immo) : calculerPlanLineaire(immo);

            AmortissementLineOutput ligneAnnee = plan.stream()
                    .filter(l -> l.annee() == annee)
                    .findFirst()
                    .orElse(null);

            if (ligneAnnee == null || ligneAnnee.dotation().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal dotation = ligneAnnee.dotation();

            String compteDebitCode = "681300";
            if (!compteRepository.existsByCode(compteDebitCode)) {
                compteRepository.save(CompteEntity.builder()
                        .id(UUID.randomUUID())
                        .code(compteDebitCode)
                        .intitule("Dotations aux amortissements d'immob. corp.")
                        .niveau(3)
                        .parentCode("681")
                        .nature("CHARGE")
                        .isAuxiliaire(false)
                        .build());
            }

            String compteImmo = compteImmoDefaut != null ? compteImmoDefaut : "240000";
            String compteCreditCode = "28" + compteImmo.substring(1);
            if (!compteRepository.existsByCode(compteCreditCode)) {
                compteRepository.save(CompteEntity.builder()
                        .id(UUID.randomUUID())
                        .code(compteCreditCode)
                        .intitule("Amortissements de l'immo " + immo.getCode())
                        .niveau(3)
                        .parentCode("28")
                        .nature("ACTIF")
                        .isAuxiliaire(false)
                        .build());
            }

            EcritureEntity ecriture = EcritureEntity.builder()
                    .id(UUID.randomUUID())
                    .journal(journalOD)
                    .numeroPiece("AMORT-" + immo.getCode() + "-" + annee)
                    .libelle("Dotation amortissement - " + immo.getDesignation() + " (" + annee + ")")
                    .dateComptable(dateFinAnnee)
                    .valide(true)
                    .build();

            ecriture.addLigne(LigneEcritureEntity.builder()
                    .id(UUID.randomUUID())
                    .compteCode(compteDebitCode)
                    .debit(dotation)
                    .credit(BigDecimal.ZERO)
                    .libelleLigne("Dotation d'amortissement de l'exercice " + annee)
                    .build());

            ecriture.addLigne(LigneEcritureEntity.builder()
                    .id(UUID.randomUUID())
                    .compteCode(compteCreditCode)
                    .debit(BigDecimal.ZERO)
                    .credit(dotation)
                    .libelleLigne("Amortissement cumulé - " + immo.getDesignation())
                    .build());

            ecritureRepository.save(ecriture);
        }
    }

    private List<AmortissementLineOutput> calculerPlanLineaire(ImmobilisationEntity immo) {
        List<AmortissementLineOutput> plan = new ArrayList<>();
        BigDecimal base = immo.getValeurOrigine().subtract(immo.getValeurResiduelle());
        BigDecimal taux = BigDecimal.ONE.divide(BigDecimal.valueOf(immo.getDureeUtile()), 6, RoundingMode.HALF_UP);
        BigDecimal dotationAnnuelle = base.multiply(taux).setScale(2, RoundingMode.HALF_UP);

        LocalDate miseEnService = immo.getDateMiseEnService();
        int anneeDebut = miseEnService.getYear();

        int moisDebut = miseEnService.getMonthValue();
        int jourDebut = miseEnService.getDayOfMonth();
        int joursPremiereAnnee = 360 - (moisDebut - 1) * 30 - jourDebut + 1;
        
        BigDecimal ratioPremiereAnnee = BigDecimal.valueOf(joursPremiereAnnee)
                .divide(BigDecimal.valueOf(360), 6, RoundingMode.HALF_UP);
        BigDecimal dotationPremiereAnnee = dotationAnnuelle.multiply(ratioPremiereAnnee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal cumul = BigDecimal.ZERO;
        BigDecimal vnc = immo.getValeurOrigine();

        cumul = cumul.add(dotationPremiereAnnee);
        vnc = vnc.subtract(dotationPremiereAnnee);
        plan.add(new AmortissementLineOutput(
                anneeDebut,
                base,
                dotationPremiereAnnee,
                cumul,
                vnc
        ));

        int anneesCompletes = immo.getDureeUtile() - 1;
        for (int i = 1; i <= anneesCompletes; i++) {
            BigDecimal dotation = dotationAnnuelle;
            if (vnc.subtract(dotation).compareTo(immo.getValeurResiduelle()) < 0) {
                dotation = vnc.subtract(immo.getValeurResiduelle());
            }

            cumul = cumul.add(dotation);
            vnc = vnc.subtract(dotation);

            plan.add(new AmortissementLineOutput(
                    anneeDebut + i,
                    base,
                    dotation,
                    cumul,
                    vnc
            ));
        }

        if (vnc.compareTo(immo.getValeurResiduelle()) > 0) {
            BigDecimal dotationRestante = vnc.subtract(immo.getValeurResiduelle());
            cumul = cumul.add(dotationRestante);
            vnc = vnc.subtract(dotationRestante);

            plan.add(new AmortissementLineOutput(
                    anneeDebut + immo.getDureeUtile(),
                    base,
                    dotationRestante,
                    cumul,
                    vnc
            ));
        }

        return plan;
    }

    private List<AmortissementLineOutput> calculerPlanDegressif(ImmobilisationEntity immo) {
        List<AmortissementLineOutput> plan = new ArrayList<>();
        BigDecimal valeurOrigine = immo.getValeurOrigine();
        BigDecimal base = valeurOrigine.subtract(immo.getValeurResiduelle());
        int duree = immo.getDureeUtile();

        double coef;
        if (duree <= 4) coef = 1.5;
        else if (duree <= 6) coef = 2.0;
        else coef = 2.5;

        BigDecimal tauxLineaire = BigDecimal.ONE.divide(BigDecimal.valueOf(duree), 6, RoundingMode.HALF_UP);
        BigDecimal tauxDegressif = tauxLineaire.multiply(BigDecimal.valueOf(coef));

        LocalDate acquisition = immo.getDateAcquisition();
        int anneeDebut = acquisition.getYear();
        int moisAcquisition = acquisition.getMonthValue();

        double ratioMois = (13.0 - moisAcquisition) / 12.0;
        BigDecimal ratioPremiereAnnee = BigDecimal.valueOf(ratioMois);

        BigDecimal cumul = BigDecimal.ZERO;
        BigDecimal vnc = valeurOrigine;
        BigDecimal baseCourante = base;
        boolean aBasculeEnLineaire = false;

        for (int i = 0; i < duree; i++) {
            int anneePlan = anneeDebut + i;
            int anneesRestantes = duree - i;

            BigDecimal tauxApplicable = tauxDegressif;
            BigDecimal tauxLineaireRestant = BigDecimal.ONE.divide(BigDecimal.valueOf(anneesRestantes), 6, RoundingMode.HALF_UP);

            if (tauxLineaireRestant.compareTo(tauxDegressif) > 0 || aBasculeEnLineaire) {
                tauxApplicable = tauxLineaireRestant;
                aBasculeEnLineaire = true;
            }

            BigDecimal dotation;
            if (i == 0) {
                dotation = baseCourante.multiply(tauxApplicable).multiply(ratioPremiereAnnee).setScale(2, RoundingMode.HALF_UP);
            } else {
                if (aBasculeEnLineaire) {
                    dotation = baseCourante.multiply(tauxApplicable).setScale(2, RoundingMode.HALF_UP);
                } else {
                    dotation = vnc.subtract(immo.getValeurResiduelle()).multiply(tauxApplicable).setScale(2, RoundingMode.HALF_UP);
                }
            }

            if (vnc.subtract(dotation).compareTo(immo.getValeurResiduelle()) < 0) {
                dotation = vnc.subtract(immo.getValeurResiduelle());
            }

            cumul = cumul.add(dotation);
            vnc = vnc.subtract(dotation);

            plan.add(new AmortissementLineOutput(
                    anneePlan,
                    baseCourante,
                    dotation,
                    cumul,
                    vnc
            ));

            if (!aBasculeEnLineaire) {
                baseCourante = vnc;
            }
        }

        return plan;
    }

    private Map<String, Object> toPayload(CreateImmoInput input, UUID id) {
        String currentUserId = String.valueOf(utilsService.getCurrentUser().getUserData().get().userId());
        ImmoEventInput event = new ImmoEventInput(
                id,
                input.code(),
                input.designation(),
                input.valeurOrigine(),
                input.dateAcquisition(),
                input.dateMiseEnService(),
                input.modeAmortissement(),
                input.dureeUtile(),
                input.valeurResiduelle(),
                StatutImmobilisation.ACTIVE,
                currentUserId
        );
        return objectMapper.convertValue(event, Map.class);
    }

    private Map<String, Object> toPayload(UpdateImmoInput input) {
        String currentUserId = String.valueOf(utilsService.getCurrentUser().getUserData().get().userId());
        ImmoEventInput event = new ImmoEventInput(
                input.id(),
                input.code(),
                input.designation(),
                input.valeurOrigine(),
                input.dateAcquisition(),
                input.dateMiseEnService(),
                input.modeAmortissement(),
                input.dureeUtile(),
                input.valeurResiduelle(),
                input.statut(),
                currentUserId
        );
        return objectMapper.convertValue(event, Map.class);
    }

    private Map<String, Object> toPayload(GenerateAmortisationInput input) {
        Map<String, Object> map = new HashMap<>();
        map.put("annee", input.annee());
        map.put("compteImmoCode", input.compteImmoCode());
        return map;
    }
}
