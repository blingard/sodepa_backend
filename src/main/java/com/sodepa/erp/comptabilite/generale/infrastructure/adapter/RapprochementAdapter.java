package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.sodepa.erp.comptabilite.generale.application.inputs.LigneReleveInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.RapprochementInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ReleveManuelInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SyncInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.LigneReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.BanqueEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneReleveBancaireEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ReleveBancaireEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.BanqueRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.LigneEcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.LigneReleveBancaireRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.ReleveBancaireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur pour la gestion et le rapprochement des relevés bancaires.
 */
@Component("generalAccountingRapprochementAdapter")
@RequiredArgsConstructor
@Slf4j
public class RapprochementAdapter {

    private final ReleveBancaireRepository releveBancaireRepository;
    private final LigneReleveBancaireRepository ligneReleveBancaireRepository;
    private final LigneEcritureRepository ligneEcritureRepository;
    private final MockBankService mockBankService;
    private final BanqueRepository banqueRepository;

    public ReleveBancaireOutput saisirReleveManuel(ReleveManuelInput request) {
        BanqueEntity banque = banqueRepository.findById(request.banqueId())
                .orElseThrow(() -> new IllegalArgumentException("Banque introuvable avec l'ID: " + request.banqueId()));

        ReleveBancaireEntity releve = ReleveBancaireEntity.builder()
                .banque(banque)
                .dateReleve(request.dateReleve())
                .soldeInitial(request.soldeInitial())
                .soldeFinal(request.soldeFinal())
                .valide(false)
                .lignes(new ArrayList<>())
                .build();

        for (LigneReleveInput lr : request.lignes()) {
            releve.addLigne(LigneReleveBancaireEntity.builder()
                    .dateTransaction(lr.dateTransaction())
                    .libelle(lr.libelle())
                    .montant(lr.montant())
                    .rapproche(false)
                    .build());
        }

        ReleveBancaireEntity saved = releveBancaireRepository.save(releve);
        return mapToOutput(saved);
    }

    public ReleveBancaireOutput synchroniserReleveAutomatique(SyncInput request) {
        BanqueEntity banque = banqueRepository.findById(request.banqueId())
                .orElseThrow(() -> new IllegalArgumentException("Banque introuvable avec l'ID: " + request.banqueId()));

        List<MockBankService.MockBankTransaction> mockTx = mockBankService.fetchTransactions(banque.getNom(), request.dateReleve());
        
        BigDecimal soldeFinal = request.soldeInitial();
        for (MockBankService.MockBankTransaction tx : mockTx) {
            soldeFinal = soldeFinal.add(tx.getMontant());
        }

        ReleveBancaireEntity releve = ReleveBancaireEntity.builder()
                .banque(banque)
                .dateReleve(request.dateReleve())
                .soldeInitial(request.soldeInitial())
                .soldeFinal(soldeFinal)
                .valide(false)
                .lignes(new ArrayList<>())
                .build();

        for (MockBankService.MockBankTransaction tx : mockTx) {
            releve.addLigne(LigneReleveBancaireEntity.builder()
                    .dateTransaction(tx.getDateTransaction())
                    .libelle(tx.getLibelle())
                    .montant(tx.getMontant())
                    .rapproche(false)
                    .build());
        }

        ReleveBancaireEntity saved = releveBancaireRepository.save(releve);
        return mapToOutput(saved);
    }

    public int effectuerRapprochementAutomatique(RapprochementInput request) {
        ReleveBancaireEntity releve = releveBancaireRepository.findById(request.releveId())
                .orElseThrow(() -> new IllegalArgumentException("Relevé introuvable avec l'ID: " + request.releveId()));

        List<LigneReleveBancaireEntity> lignesReleve = ligneReleveBancaireRepository.findByReleveIdAndRapproche(request.releveId(), false);
        
        List<LigneEcritureEntity> lignesCompta = ligneEcritureRepository.findByCompteCodeAndIdNotIn(request.compteBanqueCode(), Collections.singletonList(UUID.randomUUID()));

        List<UUID> ecrituresRapprocheesIds = new ArrayList<>();
        int count = 0;

        for (LigneReleveBancaireEntity lReleve : lignesReleve) {
            BigDecimal montantReleve = lReleve.getMontant();
            LocalDate dateReleveTx = lReleve.getDateTransaction();

            for (LigneEcritureEntity lCompta : lignesCompta) {
                if (ecrituresRapprocheesIds.contains(lCompta.getId())) {
                    continue;
                }

                boolean matchMontant = false;
                if (montantReleve.compareTo(BigDecimal.ZERO) > 0) {
                    matchMontant = lCompta.getDebit().compareTo(montantReleve) == 0 && lCompta.getCredit().compareTo(BigDecimal.ZERO) == 0;
                } else {
                    matchMontant = lCompta.getCredit().compareTo(montantReleve.abs()) == 0 && lCompta.getDebit().compareTo(BigDecimal.ZERO) == 0;
                }

                boolean matchDate = Math.abs(lCompta.getEcriture().getDateComptable().toEpochDay() - dateReleveTx.toEpochDay()) <= 5;

                if (matchMontant && matchDate) {
                    lReleve.setRapproche(true);
                    ligneReleveBancaireRepository.save(lReleve);
                    ecrituresRapprocheesIds.add(lCompta.getId());
                    count++;
                    log.info("Rapprochement automatique réussi entre ligne relevé {} et ligne compta {}", lReleve.getId(), lCompta.getId());
                    break;
                }
            }
        }

        boolean toutRapproche = releve.getLignes().stream().allMatch(LigneReleveBancaireEntity::getRapproche);
        if (toutRapproche) {
            releve.setValide(true);
            releveBancaireRepository.save(releve);
        }

        return count;
    }

    private ReleveBancaireOutput mapToOutput(ReleveBancaireEntity entity) {
        return ReleveBancaireOutput.builder()
                .id(entity.getId())
                .banque(ReleveBancaireOutput.BanqueInfo.builder()
                        .id(entity.getBanque().getId())
                        .nom(entity.getBanque().getNom())
                        .build())
                .dateReleve(entity.getDateReleve())
                .soldeInitial(entity.getSoldeInitial())
                .soldeFinal(entity.getSoldeFinal())
                .valide(entity.getValide())
                .lignes(entity.getLignes() == null ? new ArrayList<>() : entity.getLignes().stream().map(this::mapLigneToOutput).collect(Collectors.toList()))
                .build();
    }

    private LigneReleveBancaireOutput mapLigneToOutput(LigneReleveBancaireEntity ligne) {
        return LigneReleveBancaireOutput.builder()
                .id(ligne.getId())
                .dateTransaction(ligne.getDateTransaction())
                .libelle(ligne.getLibelle())
                .montant(ligne.getMontant())
                .rapproche(ligne.getRapproche())
                .build();
    }
}
