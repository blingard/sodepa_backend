package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.sodepa.erp.comptabilite.generale.application.inputs.SaisieEcritureInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SimulationTvaInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.LigneInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.SimulationTvaResponse;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.JournalRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.TiersRepository;
import com.sodepa.erp.comptabilite.generale.application.usecase.EcritureValidatedEvent;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user.UserAdapterInterface;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur pour la gestion des écritures comptables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcritureAdapter {

    private final EcritureRepository ecritureRepository;
    private final JournalRepository journalRepository;
    private final CompteRepository compteRepository;
    private final TiersRepository tiersRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UtilsService utilsService;
    private final UserAdapterInterface userAdapterInterface;

    /**
     * Saisit une nouvelle écriture comptable.
     *
     * @param request les données de l'écriture
     * @return l'écriture créée
     */
    public EcritureOutput saisirEcriture(SaisieEcritureInput request) {
        utilsService.hasPermission(Permissions.INIT_CREATE_ECRITURE);

        JournalEntity journal = journalRepository.findById(request.journalId())
                .orElseThrow(() -> new IllegalArgumentException("Journal introuvable avec l'ID: " + request.journalId()));

        if (!Boolean.TRUE.equals(journal.getActif())) {
            throw new IllegalArgumentException("Le journal " + journal.getCode() + " est inactif.");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LigneInput lr : request.lignes()) {
            if (lr.debit() != null) totalDebit = totalDebit.add(lr.debit());
            if (lr.credit() != null) totalCredit = totalCredit.add(lr.credit());
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("L'écriture n'est pas équilibrée. Total Débit: " + totalDebit + ", Total Crédit: " + totalCredit);
        }

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de l'écriture doit être supérieur à zéro.");
        }

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
            if (!compteRepository.existsByCode(lr.compteCode())) {
                throw new IllegalArgumentException("Le compte " + lr.compteCode() + " n'existe pas dans le plan comptable.");
            }

            TiersEntity tiers = null;
            if (lr.tiersId() != null) {
                tiers = tiersRepository.findById(lr.tiersId())
                        .orElseThrow(() -> new IllegalArgumentException("Tiers introuvable avec l'ID: " + lr.tiersId()));
            }

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

        EcritureEntity saved = ecritureRepository.save(ecriture);
        return mapToOutput(saved);
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
    public EcritureOutput soumettrePourValidation(UUID id) {
        EcritureEntity ecriture = ecritureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Écriture introuvable avec l'ID: " + id));
        if (ecriture.getStatut() != StatutEcriture.BROUILLON) {
            throw new IllegalArgumentException("Seules les écritures au statut BROUILLON peuvent être soumises.");
        }
        ecriture.setStatut(StatutEcriture.SOUMIS);
        return mapToOutput(ecritureRepository.save(ecriture));
    }

    /**
     * Valide définitivement une écriture soumise.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture validée
     */
    public EcritureOutput validerEcriture(UUID id) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_ECRITURE);

        EcritureEntity ecriture = ecritureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Écriture introuvable avec l'ID: " + id));
        if (ecriture.getStatut() != StatutEcriture.SOUMIS) {
            throw new IllegalArgumentException("Seules les écritures au statut SOUMIS peuvent être validées.");
        }

        ecriture.validateEquilibre();

        ecriture.setValide(true);
        ecriture.setStatut(StatutEcriture.VALIDE);
        
        UUID validateurId = UUID.fromString(utilsService.getCurrentUser().getUserData().get().userId());
        ecriture.setValidePar(validateurId);
        ecriture.setDateValidation(LocalDateTime.now());

        EcritureEntity saved = ecritureRepository.save(ecriture);
        eventPublisher.publishEvent(new EcritureValidatedEvent(this, saved));
        return mapToOutput(saved);
    }

    /**
     * Rejette une écriture soumise.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture rejetée
     */
    public EcritureOutput rejeterEcriture(UUID id) {
        utilsService.hasPermission(Permissions.VALIDATE_OR_REJECT_ECRITURE);

        EcritureEntity ecriture = ecritureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Écriture introuvable avec l'ID: " + id));
        if (ecriture.getStatut() != StatutEcriture.SOUMIS) {
            throw new IllegalArgumentException("Seules les écritures au statut SOUMIS peuvent être rejetées.");
        }
        ecriture.setStatut(StatutEcriture.REJETE);
        return mapToOutput(ecritureRepository.save(ecriture));
    }

    /**
     * Récupère une écriture par son identifiant.
     *
     * @param id l'identifiant de l'écriture
     * @return l'écriture correspondante
     */
    public EcritureOutput getEcritureById(UUID id) {
        EcritureEntity ecriture = ecritureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Écriture introuvable avec l'ID: " + id));
        return mapToOutput(ecriture);
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
