package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.AjouterItemInput;
import com.sodepa.erp.budget.application.inputs.CreerBudgetPlanInput;
import com.sodepa.erp.budget.application.inputs.EngagementInput;
import com.sodepa.erp.budget.application.inputs.ReallocationInput;
import com.sodepa.erp.budget.application.outputs.BudgetEngagementOutput;
import com.sodepa.erp.budget.application.outputs.BudgetItemOutput;
import com.sodepa.erp.budget.application.outputs.BudgetPlanOutput;
import com.sodepa.erp.budget.infrastructure.entities.*;
import com.sodepa.erp.budget.infrastructure.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BudgetPlanAdapter {

    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final BudgetReallocationRepository budgetReallocationRepository;
    private final BudgetEngagementRepository budgetEngagementRepository;
    private final AuditTrailRepository auditTrailRepository;

    @Transactional
    public BudgetPlanOutput creerBudgetPlan(CreerBudgetPlanInput input) {
        List<BudgetPlanEntity> existants = budgetPlanRepository.findByAnnee(input.annee());
        int nouvelleVersion = existants.size() + 1;

        BudgetPlanEntity plan = BudgetPlanEntity.builder()
                .annee(input.annee())
                .intitule(input.intitule())
                .version(nouvelleVersion)
                .statut(StatutBudget.DRAFT)
                .totalBudget(BigDecimal.ZERO)
                .creeLe(LocalDateTime.now())
                .creePar(input.utilisateurId())
                .build();

        BudgetPlanEntity saved = budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(saved.getId())
                .action("CREATE")
                .details("Création du plan budgétaire exercice " + input.annee() + " version V" + nouvelleVersion)
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());

        return mapPlan(saved);
    }

    @Transactional
    public BudgetItemOutput ajouterItemAuPlan(AjouterItemInput input) {
        BudgetPlanEntity plan = budgetPlanRepository.findById(input.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));

        if (plan.getStatut() != StatutBudget.DRAFT && plan.getStatut() != StatutBudget.REJECTED) {
            throw new IllegalStateException("Impossible d'ajouter des lignes budgétaires. Le plan n'est plus modifiable.");
        }

        Optional<BudgetItemEntity> existantOpt = budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(
                input.planId(), input.compteCode(), input.sectionId());
        if (existantOpt.isPresent()) {
            throw new IllegalArgumentException("Une ligne budgétaire pour ce compte et cette section existe déjà dans ce plan.");
        }

        BudgetItemEntity item = BudgetItemEntity.builder()
                .compteCode(input.compteCode())
                .sectionId(input.sectionId())
                .montantAnnuel(input.montant())
                .montantPlanned(input.montant())
                .montantEngage(BigDecimal.ZERO)
                .montantReal(BigDecimal.ZERO)
                .build();

        BigDecimal partMensuelle = input.montant().divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal cumul = BigDecimal.ZERO;
        for (int i = 1; i <= 12; i++) {
            BigDecimal montantPeriode = partMensuelle;
            if (i == 12) {
                montantPeriode = input.montant().subtract(cumul);
            } else {
                cumul = cumul.add(partMensuelle);
            }
            BudgetItemPeriodeEntity periode = BudgetItemPeriodeEntity.builder()
                    .periodeNum(i)
                    .montantPlanned(montantPeriode)
                    .montantReal(BigDecimal.ZERO)
                    .build();
            item.addPeriode(periode);
        }

        plan.addItem(item);
        plan.setTotalBudget(plan.getTotalBudget().add(input.montant()));
        budgetPlanRepository.save(plan);

        BudgetItemEntity saved = budgetItemRepository.save(item);
        return mapItem(saved);
    }

    @Transactional
    public void soumettrePlan(UUID planId, UUID userId) {
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));
        if (plan.getStatut() != StatutBudget.DRAFT && plan.getStatut() != StatutBudget.REJECTED) {
            throw new IllegalStateException("Le plan n'est pas à l'état DRAFT ou REJECTED.");
        }

        plan.setStatut(StatutBudget.SUBMITTED);
        plan.setModifieLe(LocalDateTime.now());
        plan.setModifiePar(userId);
        budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("SUBMIT")
                .details("Soumission du budget pour approbation.")
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());
    }

    @Transactional
    public void approuverPlan(UUID planId, UUID userId) {
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));
        if (plan.getStatut() != StatutBudget.SUBMITTED) {
            throw new IllegalStateException("Le plan doit être soumis pour être approuvé.");
        }

        plan.setStatut(StatutBudget.PUBLISHED);
        plan.setModifieLe(LocalDateTime.now());
        plan.setModifiePar(userId);
        budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("APPROVE")
                .details("Approbation et publication du plan budgétaire.")
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());
    }

    @Transactional
    public void rejeterPlan(UUID planId, UUID userId) {
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));
        if (plan.getStatut() != StatutBudget.SUBMITTED) {
            throw new IllegalStateException("Le plan doit être soumis pour être rejeté.");
        }

        plan.setStatut(StatutBudget.REJECTED);
        plan.setModifieLe(LocalDateTime.now());
        plan.setModifiePar(userId);
        budgetPlanRepository.save(plan);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("REJECT")
                .details("Rejet du plan budgétaire lors du workflow de validation.")
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());
    }

    @Transactional
    public void effectuerReallocation(ReallocationInput input) {
        BudgetItemEntity source = budgetItemRepository.findById(input.sourceItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item source introuvable."));
        BudgetItemEntity dest = budgetItemRepository.findById(input.destItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item destinataire introuvable."));

        if (!source.getBudgetPlan().getId().equals(dest.getBudgetPlan().getId())) {
            throw new IllegalArgumentException("Les lignes budgétaires doivent appartenir au même plan.");
        }

        if (source.getBudgetPlan().getStatut() != StatutBudget.PUBLISHED) {
            throw new IllegalStateException("Les réallocations ne sont permises que sur un budget publié.");
        }

        BigDecimal disponible = source.getMontantPlanned()
                .subtract(source.getMontantEngage())
                .subtract(source.getMontantReal());

        if (disponible.compareTo(input.montant()) < 0) {
            throw new IllegalArgumentException("Crédits insuffisants sur le poste source. Disponible: " + disponible);
        }

        source.setMontantPlanned(source.getMontantPlanned().subtract(input.montant()));
        dest.setMontantPlanned(dest.getMontantPlanned().add(input.montant()));

        BigDecimal diffPeriode = input.montant().divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal cumul = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) {
            BigDecimal adj = diffPeriode;
            if (i == 11) {
                adj = input.montant().subtract(cumul);
            } else {
                cumul = cumul.add(diffPeriode);
            }
            source.getPeriodes().get(i).setMontantPlanned(source.getPeriodes().get(i).getMontantPlanned().subtract(adj));
            dest.getPeriodes().get(i).setMontantPlanned(dest.getPeriodes().get(i).getMontantPlanned().add(adj));
        }

        budgetItemRepository.save(source);
        budgetItemRepository.save(dest);

        BudgetReallocationEntity reallocation = BudgetReallocationEntity.builder()
                .sourceItem(source)
                .destItem(dest)
                .montant(input.montant())
                .dateTransfert(LocalDateTime.now())
                .validePar(input.responsableId())
                .raison(input.raison())
                .build();
        budgetReallocationRepository.save(reallocation);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetItemEntity")
                .entiteId(input.sourceItemId())
                .action("REALLOCATE")
                .details("Transfert de " + input.montant() + " vers l'item ID " + input.destItemId() + ". Raison : " + input.raison())
                .timestamp(LocalDateTime.now())
                .utilisateur(input.responsableId())
                .build());
    }

    @Transactional
    public BudgetEngagementOutput enregistrerEngagement(EngagementInput input) {
        BudgetItemEntity item = budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(input.planId(), input.compteCode(), input.sectionId())
                .orElseThrow(() -> new IllegalArgumentException("Ligne budgétaire inexistante pour ce plan, compte et section."));

        if (item.getBudgetPlan().getStatut() != StatutBudget.PUBLISHED) {
            throw new IllegalStateException("Impossible d'effectuer un engagement sur un plan budgétaire non publié.");
        }

        BigDecimal disponible = item.getMontantPlanned()
                .subtract(item.getMontantEngage())
                .subtract(item.getMontantReal());

        if (disponible.compareTo(input.montant()) < 0) {
            throw new IllegalArgumentException("Crédit budgétaire insuffisant pour cet engagement (Solde disponible: " 
                    + disponible + ", Demandé: " + input.montant() + "). Opération bloquée.");
        }

        BudgetEngagementEntity engagement = BudgetEngagementEntity.builder()
                .budgetItem(item)
                .numeroEngagement(input.numeroEngagement())
                .description(input.description())
                .montant(input.montant())
                .dateEngagement(LocalDateTime.now())
                .statut("ENGAGED")
                .build();

        item.setMontantEngage(item.getMontantEngage().add(input.montant()));
        budgetItemRepository.save(item);

        BudgetEngagementEntity saved = budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(saved.getId())
                .action("CREATE")
                .details("Engagement de dépenses numéro " + input.numeroEngagement() + " pour " + input.montant())
                .timestamp(LocalDateTime.now())
                .utilisateur(input.utilisateurId())
                .build());

        return mapEngagement(saved);
    }

    @Transactional
    public void liquiderEngagement(String numeroEngagement, UUID userId) {
        BudgetEngagementEntity engagement = budgetEngagementRepository.findByNumeroEngagement(numeroEngagement)
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));

        if (!"ENGAGED".equals(engagement.getStatut())) {
            throw new IllegalStateException("L'engagement n'est plus à l'état ENGAGED (état actuel: " + engagement.getStatut() + ").");
        }

        engagement.setStatut("CONVERTED_TO_REAL");
        BudgetItemEntity item = engagement.getBudgetItem();
        item.setMontantEngage(item.getMontantEngage().subtract(engagement.getMontant()));
        
        budgetItemRepository.save(item);
        budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(engagement.getId())
                .action("LIQUIDATE")
                .details("Liquidation (conversion en réel) de l'engagement " + numeroEngagement)
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());
    }

    @Transactional
    public void annulerEngagement(String numeroEngagement, UUID userId) {
        BudgetEngagementEntity engagement = budgetEngagementRepository.findByNumeroEngagement(numeroEngagement)
                .orElseThrow(() -> new IllegalArgumentException("Engagement introuvable."));

        if (!"ENGAGED".equals(engagement.getStatut())) {
            throw new IllegalStateException("Seuls les engagements à l'état ENGAGED peuvent être annulés.");
        }

        engagement.setStatut("CANCELLED");
        BudgetItemEntity item = engagement.getBudgetItem();
        item.setMontantEngage(item.getMontantEngage().subtract(engagement.getMontant()));

        budgetItemRepository.save(item);
        budgetEngagementRepository.save(engagement);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetEngagementEntity")
                .entiteId(engagement.getId())
                .action("CANCEL")
                .details("Annulation de l'engagement budgétaire " + numeroEngagement)
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());
    }

    private BudgetPlanOutput mapPlan(BudgetPlanEntity p) {
        List<BudgetItemOutput> items = new ArrayList<>();
        if (p.getItems() != null) {
            items = p.getItems().stream().map(this::mapItem).collect(Collectors.toList());
        }
        return new BudgetPlanOutput(
                p.getId(),
                p.getAnnee(),
                p.getIntitule(),
                p.getVersion(),
                p.getStatut(),
                p.getTotalBudget(),
                p.getCreeLe(),
                p.getCreePar(),
                p.getModifieLe(),
                p.getModifiePar(),
                items
        );
    }

    private BudgetItemOutput mapItem(BudgetItemEntity i) {
        return new BudgetItemOutput(
                i.getId(),
                i.getCompteCode(),
                i.getSectionId(),
                i.getMontantAnnuel(),
                i.getMontantPlanned(),
                i.getMontantEngage(),
                i.getMontantReal()
        );
    }

    private BudgetEngagementOutput mapEngagement(BudgetEngagementEntity e) {
        return new BudgetEngagementOutput(
                e.getId(),
                e.getBudgetItem().getId(),
                e.getNumeroEngagement(),
                e.getDescription(),
                e.getMontant(),
                e.getDateEngagement(),
                e.getStatut()
        );
    }
}
