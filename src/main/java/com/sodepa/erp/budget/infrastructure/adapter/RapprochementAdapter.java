package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.PreEngagementInput;
import com.sodepa.erp.budget.application.inputs.RejeterInput;
import com.sodepa.erp.budget.application.inputs.ValiderEtapeInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.application.outputs.RecommandationPaiementOutput;
import com.sodepa.erp.budget.infrastructure.entities.*;
import com.sodepa.erp.budget.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.VentilationAnalytiqueEntity;
import com.sodepa.erp.comptabilite.generale.application.usecase.EcritureValidatedEvent;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component("budgetRapprochementAdapter")
@RequiredArgsConstructor
public class RapprochementAdapter {

    private final ReleveBancaireRepository releveBancaireRepository;
    private final LigneReleveBancaireRepository ligneReleveBancaireRepository;
    private final EcritureRepository ecritureRepository;
    private final PrevisionTresorerieRepository previsionTresorerieRepository;
    private final BudgetEngagementRepository budgetEngagementRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final AuditTrailRepository auditTrailRepository;

    @Transactional
    public int matchingAutomatique(UUID releveId) {
        ReleveBancaireEntity releve = releveBancaireRepository.findById(releveId)
                .orElseThrow(() -> new IllegalArgumentException("Relevé bancaire introuvable."));

        List<EcritureEntity> ecritures = ecritureRepository.findAll();
        int matchingCount = 0;

        for (LigneReleveBancaireEntity ligneReleve : releve.getLignes()) {
            if ("RAPPROCHE".equalsIgnoreCase(ligneReleve.getStatutRapprochement())) {
                continue;
            }

            BigDecimal montantReleve = ligneReleve.getMontant();
            LocalDate dateReleve = ligneReleve.getDateValeur();
            String libelleReleve = ligneReleve.getLibelle().toLowerCase();

            boolean found = false;

            for (EcritureEntity e : ecritures) {
                if (e.getStatut() != StatutEcriture.VALIDE) {
                    continue;
                }

                long ecartJours = Math.abs(ChronoUnit.DAYS.between(dateReleve, e.getDateComptable()));
                if (ecartJours > 3) {
                    continue;
                }

                for (LigneEcritureEntity ligneGL : e.getLignes()) {
                    if (!ligneGL.getCompteCode().startsWith("52")) {
                        continue;
                    }

                    BigDecimal montantGL = (ligneGL.getDebit() != null && ligneGL.getDebit().compareTo(BigDecimal.ZERO) > 0)
                            ? ligneGL.getDebit()
                            : (ligneGL.getCredit() != null ? ligneGL.getCredit().negate() : BigDecimal.ZERO);

                    if (montantReleve.compareTo(montantGL) == 0) {
                        String libGL = ligneGL.getLibelleLigne() != null ? ligneGL.getLibelleLigne().toLowerCase() : "";
                        String libEcriture = e.getLibelle() != null ? e.getLibelle().toLowerCase() : "";

                        if (libelleReleve.contains(libGL) || libGL.contains(libelleReleve) ||
                            libelleReleve.contains(libEcriture) || libEcriture.contains(libelleReleve)) {
                            
                            ligneReleve.setStatutRapprochement("RAPPROCHE");
                            ligneReleve.setEcritureId(e.getId());
                            ligneReleveBancaireRepository.save(ligneReleve);
                            matchingCount++;
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    break;
                }
            }
        }
        return matchingCount;
    }

    @Transactional(readOnly = true)
    public List<RecommandationPaiementOutput> recommanderArbitrageDecaissements(BigDecimal fondsSecurite,
                                                                           LocalDate debut, LocalDate fin,
                                                                           BigDecimal soldeBanqueActuel) {
        List<PrevisionTresorerieEntity> previsions = previsionTresorerieRepository.findByDateEcheanceBetween(debut, fin);
        List<RecommandationPaiementOutput> recommandations = new ArrayList<>();

        BigDecimal cashDisponible = soldeBanqueActuel.subtract(fondsSecurite);
        if (cashDisponible.compareTo(BigDecimal.ZERO) < 0) {
            cashDisponible = BigDecimal.ZERO;
        }

        List<PrevisionTresorerieEntity> factures = previsions.stream()
                .filter(p -> "DECAISSEMENT".equalsIgnoreCase(p.getType()) && "FOURNISSEUR".equalsIgnoreCase(p.getSource()))
                .sorted((f1, f2) -> {
                    boolean desc1 = f1.getLibelle().toLowerCase().contains("escompte");
                    boolean desc2 = f2.getLibelle().toLowerCase().contains("escompte");
                    if (desc1 && !desc2) return -1;
                    if (!desc1 && desc2) return 1;
                    return f1.getDateEcheance().compareTo(f2.getDateEcheance());
                })
                .toList();

        BigDecimal cumulDecaissements = BigDecimal.ZERO;

        for (PrevisionTresorerieEntity f : factures) {
            BigDecimal montant = f.getMontant();
            boolean aEscompte = f.getLibelle().toLowerCase().contains("escompte");

            String priorite = "MOYENNE";
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), f.getDateEcheance());
            if (joursRestants < 3 || aEscompte) {
                priorite = "HAUTE";
            } else if (joursRestants > 15) {
                priorite = "BASSE";
            }

            String action = "DIFFÉRÉ_TRÉSORERIE_INSUFFISANTE";
            if (cumulDecaissements.add(montant).compareTo(cashDisponible) <= 0) {
                action = "RECOMMANDÉ";
                cumulDecaissements = cumulDecaissements.add(montant);
            }

            recommandations.add(new RecommandationPaiementOutput(
                    f.getLibelle(),
                    montant,
                    f.getDateEcheance(),
                    aEscompte,
                    priorite,
                    action
            ));
        }

        return recommandations;
    }

    @Transactional
    public void traiterRapprochementBudget(EcritureValidatedEvent event) {
        EcritureEntity ecriture = event.getEcriture();
        int annee = ecriture.getDateComptable().getYear();
        int mois = ecriture.getDateComptable().getMonthValue();

        for (LigneEcritureEntity ligne : ecriture.getLignes()) {
            String compteCode = ligne.getCompteCode();
            BigDecimal montantLigne = ligne.getDebit().compareTo(BigDecimal.ZERO) > 0 ? ligne.getDebit() : ligne.getCredit();

            if (compteCode.startsWith("6") || compteCode.startsWith("7")) {
                if (ligne.getVentilations() != null && !ligne.getVentilations().isEmpty()) {
                    for (VentilationAnalytiqueEntity vent : ligne.getVentilations()) {
                        UUID sectionId = vent.getSection().getId();
                        BigDecimal montantVentile = vent.getMontant();
                        imputerReelBudgetItem(annee, compteCode, sectionId, mois, montantVentile);
                    }
                } else {
                    imputerReelBudgetItem(annee, compteCode, null, mois, montantLigne);
                }
            }
        }
    }

    private void imputerReelBudgetItem(int annee, String compteCode, UUID sectionId, int mois, BigDecimal montant) {
        List<BudgetItemEntity> items = budgetItemRepository.findByCompteCodeAndSectionId(compteCode, sectionId);
        
        for (BudgetItemEntity item : items) {
            if (item.getBudgetPlan().getAnnee().equals(annee)) {
                item.setMontantReal(item.getMontantReal().add(montant));

                for (BudgetItemPeriodeEntity periode : item.getPeriodes()) {
                    if (periode.getPeriodeNum().equals(mois)) {
                        periode.setMontantReal(periode.getMontantReal().add(montant));
                        break;
                    }
                }

                budgetItemRepository.save(item);
            }
        }
    }

    @Transactional
    public BudgetEngagementOutput soumettrePreEngagement(PreEngagementInput input) {
        BudgetItemEntity item = budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(input.planId(), input.compteCode(), input.sectionId())
                .orElseThrow(() -> new IllegalArgumentException("Ligne budgétaire introuvable."));

        if (item.getBudgetPlan().getStatut() != StatutBudget.PUBLISHED) {
            throw new IllegalStateException("Le plan budgétaire de l'exercice doit être publié.");
        }

        BigDecimal disponible = item.getMontantPlanned()
                .subtract(item.getMontantEngage())
                .subtract(item.getMontantReal());

        if (disponible.compareTo(input.montant()) < 0) {
            throw new IllegalArgumentException("Crédit budgétaire insuffisant pour ce pré-engagement (Disponible: " 
                    + disponible + ", Demandé: " + input.montant() + ").");
        }

        BudgetEngagementEntity engagement = BudgetEngagementEntity.builder()
                .budgetItem(item)
                .numeroEngagement(input.numeroEngagement())
                .description(input.description())
                .montant(input.montant())
                .dateEngagement(LocalDateTime.now())
                .statut("ENGAGED")
                .statutWorkflow("PRE_ENGAGEMENT")
                .approbateurCourantRole("CHEF_SERVICE")
                .build();

        item.setMontantEngage(item.getMontantEngage().add(input.montant()));
        budgetItemRepository.save(item);

        BudgetEngagementEntity saved = budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(saved.getId())
                .action("PRE_ENGAGE")
                .details("Création du pré-engagement numéro " + input.numeroEngagement() + " en attente de visa Chef de Service.")
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());

        return new BudgetEngagementOutput(saved.getId(), saved.getBudgetItem().getId(), saved.getNumeroEngagement(), saved.getDescription(), saved.getMontant(), saved.getDateEngagement(), saved.getStatut());
    }

    @Transactional
    public void validerEtapeWorkflow(ValiderEtapeInput input) {
        BudgetEngagementEntity eng = budgetEngagementRepository.findByNumeroEngagement(input.numeroEngagement())
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));

        if ("APPROVED".equalsIgnoreCase(eng.getStatutWorkflow()) || "CANCELLED".equalsIgnoreCase(eng.getStatut())) {
            throw new IllegalStateException("Cet engagement est déjà traité ou annulé.");
        }

        if (!input.roleApprobateur().equalsIgnoreCase(eng.getApprobateurCourantRole())) {
            throw new IllegalArgumentException("Action interdite : rôle '" + input.roleApprobateur() + "' non habilité pour valider cette étape (Attendu: '" 
                    + eng.getApprobateurCourantRole() + "').");
        }

        BigDecimal montant = eng.getMontant();

        if ("CHEF_SERVICE".equalsIgnoreCase(input.roleApprobateur())) {
            if (montant.compareTo(BigDecimal.valueOf(500000)) <= 0) {
                eng.setStatutWorkflow("APPROVED");
                eng.setApprobateurCourantRole(null);
            } else {
                eng.setStatutWorkflow("APPROVED_BY_CHEF");
                eng.setApprobateurCourantRole("DIRECTEUR_FINANCIER");
            }
        } else if ("DIRECTEUR_FINANCIER".equalsIgnoreCase(input.roleApprobateur())) {
            if (montant.compareTo(BigDecimal.valueOf(5000000)) <= 0) {
                eng.setStatutWorkflow("APPROVED");
                eng.setApprobateurCourantRole(null);
            } else {
                eng.setStatutWorkflow("APPROVED_BY_DF");
                eng.setApprobateurCourantRole("DIRECTEUR_GENERAL");
            }
        } else if ("DIRECTEUR_GENERAL".equalsIgnoreCase(input.roleApprobateur())) {
            eng.setStatutWorkflow("APPROVED");
            eng.setApprobateurCourantRole(null);
        }

        budgetEngagementRepository.save(eng);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(eng.getId())
                .action("APPROVE_STEP")
                .details("Approbation de l'étape par le rôle " + input.roleApprobateur() 
                        + ". Statut workflow actuel: " + eng.getStatutWorkflow())
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());
    }

    @Transactional
    public void rejeterEngagement(RejeterInput input) {
        BudgetEngagementEntity eng = budgetEngagementRepository.findByNumeroEngagement(input.numeroEngagement())
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));

        if ("APPROVED".equalsIgnoreCase(eng.getStatutWorkflow()) || "CANCELLED".equalsIgnoreCase(eng.getStatut())) {
            throw new IllegalStateException("L'engagement ne peut plus être rejeté.");
        }

        eng.setStatut("CANCELLED");
        eng.setStatutWorkflow("REJECTED");
        eng.setApprobateurCourantRole(null);
        eng.setDescription(eng.getDescription() + " [REJETÉ : " + input.motif() + "]");

        BudgetItemEntity item = eng.getBudgetItem();
        item.setMontantEngage(item.getMontantEngage().subtract(eng.getMontant()));
        budgetItemRepository.save(item);

        budgetEngagementRepository.save(eng);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(eng.getId())
                .action("REJECT_ENGAGEMENT")
                .details("Rejet de l'engagement " + input.numeroEngagement() + " pour motif : " + input.motif() + ". Crédits libérés.")
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());
    }
}
