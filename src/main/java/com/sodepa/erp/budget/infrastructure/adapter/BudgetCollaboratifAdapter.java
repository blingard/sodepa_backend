package com.sodepa.erp.budget.infrastructure.adapter;

import com.sodepa.erp.budget.application.inputs.CadrageInput;
import com.sodepa.erp.budget.application.inputs.GenererHistoriqueInput;
import com.sodepa.erp.budget.application.inputs.SaisirDemandeInput;
import com.sodepa.erp.budget.application.outputs.BudgetDemandeOutput;
import com.sodepa.erp.budget.application.usecase.AjouterItemPlanUseCase;
import com.sodepa.erp.budget.infrastructure.entities.BudgetDemandeEntity;
import com.sodepa.erp.budget.infrastructure.entities.BudgetPlanEntity;
import com.sodepa.erp.budget.infrastructure.entities.AuditTrailEntity;
import com.sodepa.erp.budget.infrastructure.repo.BudgetDemandeRepository;
import com.sodepa.erp.budget.infrastructure.repo.BudgetPlanRepository;
import com.sodepa.erp.budget.infrastructure.repo.AuditTrailRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.VentilationAnalytiqueEntity;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BudgetCollaboratifAdapter {

    private final BudgetDemandeRepository budgetDemandeRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final EcritureRepository ecritureRepository;
    private final AjouterItemPlanUseCase ajouterItemPlanUseCase;

    @Transactional
    public BudgetDemandeOutput saisirDemande(SaisirDemandeInput input) {
        BudgetDemandeEntity demande = BudgetDemandeEntity.builder()
                .departementId(input.departementId())
                .annee(input.annee())
                .compteCode(input.compteCode())
                .sectionId(input.sectionId())
                .montantDemande(input.montant())
                .statut("DRAFT")
                .commentaires(input.commentaires())
                .build();

        BudgetDemandeEntity saved = budgetDemandeRepository.save(demande);
        return mapDemande(saved);
    }

    @Transactional
    public void soumettreDemandes(UUID departementId, int annee) {
        List<BudgetDemandeEntity> demandes = budgetDemandeRepository.findByDepartementIdAndAnnee(departementId, annee);
        for (BudgetDemandeEntity d : demandes) {
            if ("DRAFT".equals(d.getStatut())) {
                d.setStatut("SUBMITTED");
                budgetDemandeRepository.save(d);
            }
        }
    }

    @Transactional
    public void approuverDemande(UUID demandeId, UUID valideurId) {
        BudgetDemandeEntity d = budgetDemandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande budgétaire introuvable."));
        d.setStatut("APPROVED");
        budgetDemandeRepository.save(d);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetDemandeEntity")
                .entiteId(demandeId)
                .action("APPROVE")
                .details("Approbation de la ligne budgétaire du département " + d.getDepartementId() + " pour le compte " + d.getCompteCode())
                .timestamp(LocalDateTime.now())
                .utilisateur(valideurId)
                .build());
    }

    @Transactional
    public void rejeterDemande(UUID demandeId, String motif, UUID valideurId) {
        BudgetDemandeEntity d = budgetDemandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande budgétaire introuvable."));
        d.setStatut("REJECTED");
        d.setCommentaires(d.getCommentaires() + " [REJETÉ : " + motif + "]");
        budgetDemandeRepository.save(d);

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetDemandeEntity")
                .entiteId(demandeId)
                .action("REJECT")
                .details("Rejet de la demande budgétaire : " + motif)
                .timestamp(LocalDateTime.now())
                .utilisateur(valideurId)
                .build());
    }

    @Transactional
    public void appliquerTauxCadrage(CadrageInput input) {
        List<BudgetDemandeEntity> demandes = budgetDemandeRepository.findByAnnee(input.annee());
        int cpt = 0;
        for (BudgetDemandeEntity d : demandes) {
            if (d.getCompteCode().startsWith(input.comptePrefix()) && ("SUBMITTED".equals(d.getStatut()) || "DRAFT".equals(d.getStatut()))) {
                BigDecimal nouveauMontant = d.getMontantDemande().multiply(input.coefficient()).setScale(4, RoundingMode.HALF_UP);
                d.setCommentaires(d.getCommentaires() + " [Cadrage financier Top-Down : " + d.getMontantDemande() + " -> " + nouveauMontant + "]");
                d.setMontantDemande(nouveauMontant);
                budgetDemandeRepository.save(d);
                cpt++;
            }
        }

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetDemandeEntity")
                .entiteId(input.responsableId())
                .action("TOP_DOWN_ADJUST")
                .details("Cadrage global : application du coefficient " + input.coefficient() + " sur les comptes commençant par '" 
                        + input.comptePrefix() + "' (" + cpt + " lignes ajustées)")
                .timestamp(LocalDateTime.now())
                .utilisateur(input.responsableId())
                .build());
    }

    @Transactional
    public void genererBudgetDepuisHistorique(GenererHistoriqueInput input) {
        List<EcritureEntity> ecritures = ecritureRepository.findAll();
        Map<String, Map<UUID, BigDecimal>> cumulsMap = new HashMap<>();

        for (EcritureEntity e : ecritures) {
            if (e.getDateComptable().getYear() == input.anneeSource() && e.getStatut() == StatutEcriture.VALIDE) {
                for (LigneEcritureEntity l : e.getLignes()) {
                    String compteCode = l.getCompteCode();
                    if (compteCode.startsWith("6") || compteCode.startsWith("7")) {
                        BigDecimal montantLigne = l.getDebit().compareTo(BigDecimal.ZERO) > 0 ? l.getDebit() : l.getCredit();

                        if (l.getVentilations() != null && !l.getVentilations().isEmpty()) {
                            for (VentilationAnalytiqueEntity v : l.getVentilations()) {
                                UUID secId = v.getSection().getId();
                                BigDecimal montVent = v.getMontant();
                                cumulerMontant(cumulsMap, compteCode, secId, montVent);
                            }
                        } else {
                            cumulerMontant(cumulsMap, compteCode, null, montantLigne);
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, Map<UUID, BigDecimal>> entry : cumulsMap.entrySet()) {
            String cCode = entry.getKey();
            BigDecimal coefficient = cCode.startsWith("7") ? input.coeffVentes() : input.coeffCharges();

            for (Map.Entry<UUID, BigDecimal> subEntry : entry.getValue().entrySet()) {
                UUID sId = subEntry.getKey();
                BigDecimal montantReelSource = subEntry.getValue();
                BigDecimal montantProjete = montantReelSource.multiply(coefficient).setScale(4, RoundingMode.HALF_UP);

                BudgetDemandeEntity demande = BudgetDemandeEntity.builder()
                        .departementId(input.departementId())
                        .annee(input.anneeCible())
                        .compteCode(cCode)
                        .sectionId(sId)
                        .montantDemande(montantProjete)
                        .statut("DRAFT")
                        .commentaires("Génération automatique d'après historique réel " + input.anneeSource() + " (Coeff: " + coefficient + ")")
                        .build();

                budgetDemandeRepository.save(demande);
            }
        }
    }

    @Transactional
    public void consoliderDemandesDansPlan(int annee, UUID planId, UUID userId) {
        BudgetPlanEntity plan = budgetPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan budgétaire introuvable."));

        List<BudgetDemandeEntity> demandesApprouvees = budgetDemandeRepository.findByAnnee(annee).stream()
                .filter(d -> "APPROVED".equalsIgnoreCase(d.getStatut()))
                .toList();

        for (BudgetDemandeEntity d : demandesApprouvees) {
            try {
                ajouterItemPlanUseCase.execute(new com.sodepa.erp.budget.application.inputs.AjouterItemInput(plan.getId(), d.getCompteCode(), d.getSectionId(), d.getMontantDemande()));
            } catch (Exception ignored) {
            }
        }

        auditTrailRepository.save(AuditTrailEntity.builder()
                .entiteNom("BudgetPlanEntity")
                .entiteId(planId)
                .action("CONSOLIDATE")
                .details("Consolidation et intégration des propositions budgétaires de l'exercice dans le plan.")
                .timestamp(LocalDateTime.now())
                .utilisateur(userId)
                .build());
    }

    private void cumulerMontant(Map<String, Map<UUID, BigDecimal>> cumulsMap, String compte, UUID section, BigDecimal montant) {
        cumulsMap.putIfAbsent(compte, new HashMap<>());
        Map<UUID, BigDecimal> subMap = cumulsMap.get(compte);
        UUID key = (section == null) ? UUID.fromString("00000000-0000-0000-0000-000000000000") : section;
        subMap.put(key, subMap.getOrDefault(key, BigDecimal.ZERO).add(montant));
    }

    private BudgetDemandeOutput mapDemande(BudgetDemandeEntity e) {
        return new BudgetDemandeOutput(
                e.getId(),
                e.getDepartementId(),
                e.getAnnee(),
                e.getCompteCode(),
                e.getSectionId(),
                e.getMontantDemande(),
                e.getStatut(),
                e.getCommentaires()
        );
    }
}
