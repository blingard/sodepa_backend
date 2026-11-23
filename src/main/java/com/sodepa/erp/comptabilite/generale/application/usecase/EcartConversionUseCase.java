package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ReevaluationInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReevaluationLineOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.*;
import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.utils.CodeJournal;
import com.sodepa.erp.utils.Devise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Service métier gérant les écarts de conversion en fin de période (Art. 57 SYSCOHADA).
 */
@Service
@RequiredArgsConstructor
public class EcartConversionUseCase implements UseCase<ReevaluationInput, List<ReevaluationLineOutput>> {

    private final EcritureRepository ecritureRepository;
    private final CompteRepository compteRepository;
    private final JournalRepository journalRepository;

    @Override
    @Transactional
    public List<ReevaluationLineOutput> execute(ReevaluationInput request) {
        LocalDate debutAnnee = LocalDate.of(request.annee(), 1, 1);
        LocalDate finAnnee = LocalDate.of(request.annee(), 12, 31);
        List<EcritureEntity> toutesEcritures = ecritureRepository.findValidatedForeignCurrencyByPeriod(
                debutAnnee, finAnnee, List.of(Devise.XOF, Devise.XAF));
        JournalEntity journalOD = journalRepository.findByCode(CodeJournal.OD)
                .orElseThrow(() -> new IllegalArgumentException("Journal des Opérations Diverses (OD) manquant."));

        Map<String, Map<Devise, List<LigneEcritureEntity>>> lignesParCompteDevise = new HashMap<>();

        for (EcritureEntity e : toutesEcritures) {
            if (!Boolean.TRUE.equals(e.getValide()) || e.getDateComptable() == null) {
                continue;
            }
            Devise devise = e.getTypeDevise();
            if (devise == null || devise == Devise.XOF || devise == Devise.XAF) {
                continue;
            }

            for (LigneEcritureEntity l : e.getLignes()) {
                String c = l.getCompteCode();
                if (c != null && (c.startsWith("40") || c.startsWith("41") || c.startsWith("52") || c.startsWith("53"))) {
                    lignesParCompteDevise
                        .computeIfAbsent(c, k -> new HashMap<>())
                        .computeIfAbsent(devise, k -> new ArrayList<>())
                        .add(l);
                }
            }
        }

        List<ReevaluationLineOutput> reevaluations = new ArrayList<>();
        LocalDate dateReval = LocalDate.of(request.annee(), 12, 31);

        for (Map.Entry<String, Map<Devise, List<LigneEcritureEntity>>> entryCompte : lignesParCompteDevise.entrySet()) {
            String compteCode = entryCompte.getKey();
            for (Map.Entry<Devise, List<LigneEcritureEntity>> entryDevise : entryCompte.getValue().entrySet()) {
                Devise devise = entryDevise.getKey();
                BigDecimal cours = request.coursCloture().get(devise.name());
                if (cours == null || cours.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal soldeDevise = BigDecimal.ZERO;
                BigDecimal valeurLivreXof = BigDecimal.ZERO;

                for (LigneEcritureEntity l : entryDevise.getValue()) {
                    BigDecimal debit = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                    BigDecimal credit = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
                    BigDecimal diffXof = debit.subtract(credit);
                    BigDecimal taux = l.getEcriture().getTauxChange();
                    BigDecimal diffDevise = (taux != null && taux.compareTo(BigDecimal.ZERO) > 0)
                            ? diffXof.divide(taux, 6, RoundingMode.HALF_UP)
                            : diffXof;

                    soldeDevise = soldeDevise.add(diffDevise);
                    valeurLivreXof = valeurLivreXof.add(diffXof);
                }

                if (soldeDevise.compareTo(BigDecimal.ZERO) == 0 && valeurLivreXof.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                BigDecimal valeurReevalueeXof = soldeDevise.multiply(cours).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ecart = valeurReevalueeXof.subtract(valeurLivreXof).setScale(2, RoundingMode.HALF_UP);

                if (ecart.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                String nature = ecart.compareTo(BigDecimal.ZERO) > 0 ? "GAIN_LATENT" : "PERTE_LATENTE";

                reevaluations.add(ReevaluationLineOutput.builder()
                        .compteCode(compteCode)
                        .typeDevise(devise.name())
                        .soldeDevise(soldeDevise)
                        .valeurLivreXof(valeurLivreXof)
                        .coursCloture(cours)
                        .valeurReevalueeXof(valeurReevalueeXof)
                        .ecart(ecart)
                        .natureEcart(nature)
                        .build());

                creerCompteSiAbsent("476000", "Écarts de conversion - Actif (Perte latente)");
                creerCompteSiAbsent("477000", "Écarts de conversion - Passif (Gain latent)");

                EcritureEntity ecriture = EcritureEntity.builder()
                        .journal(journalOD)
                        .numeroPiece("REVAL-" + compteCode + "-" + devise.name() + "-" + request.annee())
                        .libelle("Réévaluation devise " + devise.name() + " - Compte " + compteCode)
                        .dateComptable(dateReval)
                        .valide(true)
                        .build();

                BigDecimal absEcart = ecart.abs();

                if (compteCode.startsWith("41") || compteCode.startsWith("52") || compteCode.startsWith("53")) {
                    if (ecart.compareTo(BigDecimal.ZERO) > 0) {
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode(compteCode)
                                .debit(absEcart)
                                .credit(BigDecimal.ZERO)
                                .libelleLigne("Gain latent de change - Réévaluation " + devise.name())
                                .build());
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode("477000")
                                .debit(BigDecimal.ZERO)
                                .credit(absEcart)
                                .libelleLigne("Écart de conversion Passif")
                                .build());
                    } else {
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode("476000")
                                .debit(absEcart)
                                .credit(BigDecimal.ZERO)
                                .libelleLigne("Écart de conversion Actif")
                                .build());
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode(compteCode)
                                .debit(BigDecimal.ZERO)
                                .credit(absEcart)
                                .libelleLigne("Perte latente de change - Réévaluation " + devise.name())
                                .build());
                    }
                } else if (compteCode.startsWith("40")) {
                    if (ecart.compareTo(BigDecimal.ZERO) > 0) {
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode("476000")
                                .debit(absEcart)
                                .credit(BigDecimal.ZERO)
                                .libelleLigne("Écart de conversion Actif")
                                .build());
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode(compteCode)
                                .debit(BigDecimal.ZERO)
                                .credit(absEcart)
                                .libelleLigne("Perte latente - Réévaluation dette " + devise.name())
                                .build());
                    } else {
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode(compteCode)
                                .debit(absEcart)
                                .credit(BigDecimal.ZERO)
                                .libelleLigne("Gain latent - Réévaluation dette " + devise.name())
                                .build());
                        ecriture.addLigne(LigneEcritureEntity.builder()
                                .compteCode("477000")
                                .debit(BigDecimal.ZERO)
                                .credit(absEcart)
                                .libelleLigne("Écart de conversion Passif")
                                .build());
                    }
                }

                ecritureRepository.save(ecriture);
            }
        }

        return reevaluations;
    }

    private void creerCompteSiAbsent(String code, String intitule) {
        if (!compteRepository.existsByCode(code)) {
            compteRepository.save(CompteEntity.builder()
                    .code(code)
                    .intitule(intitule)
                    .niveau(3)
                    .parentCode(code.substring(0, 2))
                    .nature("ATTENTE")
                    .isAuxiliaire(false)
                    .build());
        }
    }
}
