package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.CreerFinancementInput;
import com.sodepa.erp.budget.application.outputs.EcheanceOutput;
import com.sodepa.erp.budget.application.outputs.FinancementOutput;
import com.sodepa.erp.budget.infrastructure.entities.AuditTrailEntity;
import com.sodepa.erp.budget.infrastructure.entities.EcheanceFinancementEntity;
import com.sodepa.erp.budget.infrastructure.entities.LigneFinancementEntity;
import com.sodepa.erp.budget.infrastructure.repo.AuditTrailRepository;
import com.sodepa.erp.budget.infrastructure.repo.EcheanceFinancementRepository;
import com.sodepa.erp.budget.infrastructure.repo.LigneFinancementRepository;
import com.sodepa.erp.comptabilite.generale.application.inputs.LigneInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SaisieEcritureInput;
import com.sodepa.erp.comptabilite.generale.application.usecase.SaisirEcritureUseCase;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.JournalRepository;
import com.sodepa.erp.utils.CodeJournal;
import com.sodepa.erp.utils.Devise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FinancementAdapter {

    private final LigneFinancementRepository ligneFinancementRepository;
    private final EcheanceFinancementRepository echeanceFinancementRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final SaisirEcritureUseCase saisirEcritureUseCase;
    private final JournalRepository journalRepository;
    private final CompteRepository compteRepository;

    public List<EcheanceOutput> genererPlanAmortissement(BigDecimal capital, BigDecimal tauxNominal,
                                                         int dureeMois, String periodicite,
                                                         LocalDate dateEffet) {
        List<EcheanceFinancementEntity> echeances = new ArrayList<>();

        int nbEcheances = dureeMois;
        BigDecimal tauxPeriode = tauxNominal.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);

        if ("TRIMESTRIELLE".equalsIgnoreCase(periodicite)) {
            nbEcheances = dureeMois / 3;
            tauxPeriode = tauxNominal.divide(BigDecimal.valueOf(4 * 100), 10, RoundingMode.HALF_UP);
        } else if ("ANNUELLE".equalsIgnoreCase(periodicite)) {
            nbEcheances = dureeMois / 12;
            tauxPeriode = tauxNominal.divide(BigDecimal.valueOf(1 * 100), 10, RoundingMode.HALF_UP);
        }

        if (nbEcheances <= 0) {
            throw new IllegalArgumentException("La durée en mois est insuffisante pour la périodicité sélectionnée.");
        }

        BigDecimal capitalRestant = capital;
        
        double t = tauxPeriode.doubleValue();
        double factor = Math.pow(1 + t, -nbEcheances);
        BigDecimal annuite = capital.multiply(BigDecimal.valueOf(t))
                .divide(BigDecimal.valueOf(1 - factor), 4, RoundingMode.HALF_UP);

        LocalDate dateEcheance = dateEffet;

        for (int i = 1; i <= nbEcheances; i++) {
            if ("TRIMESTRIELLE".equalsIgnoreCase(periodicite)) {
                dateEcheance = dateEcheance.plusMonths(3);
            } else if ("ANNUELLE".equalsIgnoreCase(periodicite)) {
                dateEcheance = dateEcheance.plusYears(1);
            } else {
                dateEcheance = dateEcheance.plusMonths(1);
            }

            BigDecimal interets = capitalRestant.multiply(tauxPeriode).setScale(4, RoundingMode.HALF_UP);
            BigDecimal principal = annuite.subtract(interets).setScale(4, RoundingMode.HALF_UP);

            if (i == nbEcheances || principal.compareTo(capitalRestant) > 0) {
                principal = capitalRestant;
                annuite = principal.add(interets);
            }

            capitalRestant = capitalRestant.subtract(principal).setScale(4, RoundingMode.HALF_UP);

            EcheanceFinancementEntity ech = EcheanceFinancementEntity.builder()
                    .dateEcheance(dateEcheance)
                    .principal(principal)
                    .interets(interets)
                    .soldeRestantDu(capitalRestant)
                    .statut("A_PAYER")
                    .build();

            echeances.add(ech);
        }

        return echeances.stream().map(this::mapEcheance).collect(Collectors.toList());
    }

    public FinancementOutput enregistrerFinancement(CreerFinancementInput input) {
        LigneFinancementEntity ligne = LigneFinancementEntity.builder()
                .banqueId(input.banqueId())
                .intitule(input.intitule())
                .type(input.type())
                .capitalEmprunte(input.capital())
                .tauxNominal(input.tauxNominal())
                .dateEffet(input.dateEffet())
                .dureeMois(input.dureeMois())
                .periodicite(input.periodicite())
                .statut("ACTIF")
                .build();

        List<EcheanceFinancementEntity> schedule = new ArrayList<>();
        int nbEcheances = input.dureeMois();
        BigDecimal tauxPeriode = input.tauxNominal().divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);

        if ("TRIMESTRIELLE".equalsIgnoreCase(input.periodicite())) {
            nbEcheances = input.dureeMois() / 3;
            tauxPeriode = input.tauxNominal().divide(BigDecimal.valueOf(4 * 100), 10, RoundingMode.HALF_UP);
        } else if ("ANNUELLE".equalsIgnoreCase(input.periodicite())) {
            nbEcheances = input.dureeMois() / 12;
            tauxPeriode = input.tauxNominal().divide(BigDecimal.valueOf(1 * 100), 10, RoundingMode.HALF_UP);
        }

        BigDecimal capitalRestant = input.capital();
        
        double t = tauxPeriode.doubleValue();
        double factor = Math.pow(1 + t, -nbEcheances);
        BigDecimal annuite = input.capital().multiply(BigDecimal.valueOf(t))
                .divide(BigDecimal.valueOf(1 - factor), 4, RoundingMode.HALF_UP);

        LocalDate dateEcheance = input.dateEffet();

        for (int i = 1; i <= nbEcheances; i++) {
            if ("TRIMESTRIELLE".equalsIgnoreCase(input.periodicite())) {
                dateEcheance = dateEcheance.plusMonths(3);
            } else if ("ANNUELLE".equalsIgnoreCase(input.periodicite())) {
                dateEcheance = dateEcheance.plusYears(1);
            } else {
                dateEcheance = dateEcheance.plusMonths(1);
            }

            BigDecimal interets = capitalRestant.multiply(tauxPeriode).setScale(4, RoundingMode.HALF_UP);
            BigDecimal principal = annuite.subtract(interets).setScale(4, RoundingMode.HALF_UP);

            if (i == nbEcheances || principal.compareTo(capitalRestant) > 0) {
                principal = capitalRestant;
                annuite = principal.add(interets);
            }

            capitalRestant = capitalRestant.subtract(principal).setScale(4, RoundingMode.HALF_UP);

            EcheanceFinancementEntity ech = EcheanceFinancementEntity.builder()
                    .dateEcheance(dateEcheance)
                    .principal(principal)
                    .interets(interets)
                    .soldeRestantDu(capitalRestant)
                    .statut("A_PAYER")
                    .build();

            schedule.add(ech);
        }

        for (EcheanceFinancementEntity ech : schedule) {
            ligne.addEcheance(ech);
        }

        LigneFinancementEntity saved = ligneFinancementRepository.save(ligne);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("LigneFinancementEntity")
                .entiteId(saved.getId())
                .action("CREATE")
                .details("Mise en place du financement " + input.intitule() + " d'un capital de " + input.capital())
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());

        comptabiliserReceptionCapital(saved);

        return mapFinancement(saved);
    }

    public void enregistrerPaiementEcheance(UUID echeanceId, UUID userId) {
        EcheanceFinancementEntity echeance = echeanceFinancementRepository.findById(echeanceId)
                .orElseThrow(() -> new IllegalArgumentException("Échéance de financement introuvable."));

        if (!"A_PAYER".equals(echeance.getStatut())) {
            throw new IllegalStateException("L'échéance est déjà payée ou annulée.");
        }

        echeance.setStatut("PAYE");
        echeanceFinancementRepository.save(echeance);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("EcheanceFinancementEntity")
                .entiteId(echeanceId)
                .action("PAY")
                .details("Règlement de l'échéance du " + echeance.getDateEcheance() 
                        + " (Principal: " + echeance.getPrincipal() + ", Intérêts: " + echeance.getInterets() + ")")
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());

        comptabiliserReglementEcheance(echeance);
    }

    private void comptabiliserReceptionCapital(LigneFinancementEntity ligne) {
        JournalEntity journal = journalRepository.findAll().stream()
                .filter(j -> j.getCode() == CodeJournal.BQ || j.getActif())
                .findFirst()
                .orElse(null);

        if (journal == null) return;

        String compteBanque = "521000";
        String compteEmprunt = "162000";

        if (!compteRepository.existsByCode(compteBanque) || !compteRepository.existsByCode(compteEmprunt)) {
            return;
        }

        List<LigneInput> lignes = new ArrayList<>();
        lignes.add(LigneInput.builder()
                .compteCode(compteBanque)
                .debit(ligne.getCapitalEmprunte())
                .credit(BigDecimal.ZERO)
                .libelleLigne("Déblocage fonds emprunt " + ligne.getIntitule())
                .build());

        lignes.add(LigneInput.builder()
                .compteCode(compteEmprunt)
                .debit(BigDecimal.ZERO)
                .credit(ligne.getCapitalEmprunte())
                .libelleLigne("Prise en charge dette emprunt " + ligne.getIntitule())
                .build());

        SaisieEcritureInput request = SaisieEcritureInput.builder()
                .journalId(journal.getId())
                .numeroPiece("FNC-" + ligne.getIntitule().substring(0, Math.min(5, ligne.getIntitule().length())).toUpperCase())
                .libelle("Déblocage de financement: " + ligne.getIntitule())
                .dateComptable(ligne.getDateEffet())
                .typeDevise(Devise.XOF)
                .tauxChange(BigDecimal.ONE)
                .lignes(lignes)
                .build();

        try {
            saisirEcritureUseCase.execute(request);
        } catch (Exception ignored) {
        }
    }

    private void comptabiliserReglementEcheance(EcheanceFinancementEntity echeance) {
        JournalEntity journal = journalRepository.findAll().stream()
                .filter(j -> j.getCode() == CodeJournal.BQ || j.getActif())
                .findFirst()
                .orElse(null);

        if (journal == null) return;

        String compteBanque = "521000";
        String compteEmprunt = "162000";
        String compteInterets = "661100";

        if (!compteRepository.existsByCode(compteBanque) || !compteRepository.existsByCode(compteEmprunt) || !compteRepository.existsByCode(compteInterets)) {
            return;
        }

        BigDecimal totalAnnuite = echeance.getPrincipal().add(echeance.getInterets());

        List<LigneInput> lignes = new ArrayList<>();
        lignes.add(LigneInput.builder()
                .compteCode(compteEmprunt)
                .debit(echeance.getPrincipal())
                .credit(BigDecimal.ZERO)
                .libelleLigne("Remboursement capital échéance du " + echeance.getDateEcheance())
                .build());

        lignes.add(LigneInput.builder()
                .compteCode(compteInterets)
                .debit(echeance.getInterets())
                .credit(BigDecimal.ZERO)
                .libelleLigne("Charges d'intérêts échéance du " + echeance.getDateEcheance())
                .build());

        lignes.add(LigneInput.builder()
                .compteCode(compteBanque)
                .debit(BigDecimal.ZERO)
                .credit(totalAnnuite)
                .libelleLigne("Règlement banque échéance du " + echeance.getDateEcheance())
                .build());

        SaisieEcritureInput request = SaisieEcritureInput.builder()
                .journalId(journal.getId())
                .numeroPiece("FNC-ECH-" + echeance.getId().toString().substring(0, 5).toUpperCase())
                .libelle("Paiement échéance du financement " + echeance.getLigneFinancement().getIntitule())
                .dateComptable(echeance.getDateEcheance())
                .typeDevise(Devise.XOF)
                .tauxChange(BigDecimal.ONE)
                .lignes(lignes)
                .build();

        try {
            saisirEcritureUseCase.execute(request);
        } catch (Exception ignored) {
        }
    }

    private FinancementOutput mapFinancement(LigneFinancementEntity entity) {
        List<EcheanceOutput> echeances = new ArrayList<>();
        if (entity.getEcheances() != null) {
            echeances = entity.getEcheances().stream().map(this::mapEcheance).collect(Collectors.toList());
        }
        return new FinancementOutput(
                entity.getId(),
                entity.getBanqueId(),
                entity.getIntitule(),
                entity.getType(),
                entity.getCapitalEmprunte(),
                entity.getTauxNominal(),
                entity.getDateEffet(),
                entity.getDureeMois(),
                entity.getPeriodicite(),
                entity.getStatut(),
                echeances
        );
    }

    private EcheanceOutput mapEcheance(EcheanceFinancementEntity entity) {
        return new EcheanceOutput(
                entity.getId(),
                entity.getDateEcheance(),
                entity.getPrincipal(),
                entity.getInterets(),
                entity.getSoldeRestantDu(),
                entity.getStatut()
        );
    }
}
