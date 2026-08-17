package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateBankInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.EcritureOutput;
import com.sodepa.erp.comptabilite.generale.application.outputs.JournalOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.BanqueEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.JournalEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.event.BankEventInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.EcritureRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.JournalRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.LigneEcritureRepository;
import com.sodepa.erp.share.MakerCheckerEnginePort;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Cas d'usage gérant les travaux d'inventaire de fin d'année et la clôture comptable d'un exercice.
 *
 * <p><b>Rôle dans le système :</b></p>
 * Cet Usecase permet d'automatiser la fin de vie d'un exercice fiscal dans le système ERP.
 * Il assure le calcul du résultat (bénéfice/perte) en analysant les flux des comptes de gestion (classes 6, 7 et 8),
 * génère la pièce de clôture en remettant à zéro ces comptes de gestion dans le journal des Opérations Diverses (OD),
 * et prépare automatiquement l'exercice comptable suivant en générant le bilan d'ouverture (les À-nouveaux)
 * dans le journal dédié (RAN), reportant ainsi les soldes des comptes de bilan (classes 1 à 5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalYearClosingAdapter {
    private final EcritureRepository ecritureRepository;
    private final JournalRepository journalRepository;
    private final UtilsService utilsService;
    private final ObjectMapper objectMapper;
    private final MakerCheckerEnginePort makerCheckerEngine;
    private final static String YEAR="year";


    /**
     * Effectue la clôture annuelle automatique pour une année donnée.
     * Cette méthode solde les comptes de charges/produits, calcule le résultat, passe l'écriture de clôture,
     * et crée l'écriture d'ouverture (À-nouveau) au premier jour de l'année suivante.
     *
     * @param annee l'année civile de l'exercice à clôturer.
     * @throws IllegalArgumentException si aucune écriture n'est trouvée pour l'année.
     * @throws IllegalStateException s'il existe des écritures à l'état de brouillon non validées.
     */
    @Transactional
    public void fiscalYearClosing(int annee) {
        utilsService.hasPermission(Permissions.INIT_CREATE_FISCAL_YEAR_CLOSING);
        log.info("Initialisation du processus de clôture automatique pour l'année {}", annee);
        validation(annee);
        UUID entityPk = UUID.randomUUID();
        Map<String, Object> payload = toPayload(Map.of(YEAR, annee));
        makerCheckerEngine.submitChange(
                MakerCheckerEntityName.FISCALYEARCLOSING,
                entityPk.toString(),
                payload, MakerCheckerOperationType.CREATE
        );

    }


    /**
     * Effectue une simulation la clôture annuelle automatique pour une année donnée.
     * Cette méthode solde les comptes de charges/produits, calcule le résultat, passe l'écriture de clôture,
     * et crée l'écriture d'ouverture (À-nouveau) au premier jour de l'année suivante.
     *
     * @param input la donnee initialiser.
     * @throws IllegalArgumentException si aucune écriture n'est trouvée pour l'année.
     * @throws IllegalStateException s'il existe des écritures à l'état de brouillon non validées.
     */
    @Transactional
    public void generateFiscalYearClosingSimulation(ValidateOrRejectSubmissionInput input) {
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
            Map<String, Integer> map = objectMapper.convertValue(checkerOutput.payload(), Map.class);
            if(checkerOutput.checkerOperationType() == MakerCheckerOperationType.CREATE) {
                creation(map.get(YEAR));
                makerCheckerEngine.update(checkerOutput.id(), input.decision(), input.notes(), checkerId.toString());
            }

        }
    }

    private void creation(int annee) {
        log.info("Début du processus de clôture automatique pour l'année {}", annee);

        LocalDate dateDebut = LocalDate.of(annee, 1, 1);
        LocalDate dateFin = LocalDate.of(annee, 12, 31);
        LocalDate dateOuvertureSuivante = LocalDate.of(annee + 1, 1, 1);

        // 1. Récupérer toutes les écritures validées de l'année
        List<EcritureEntity> ecritures = ecritureRepository.findByDateComptableBetweenWithLignes(dateDebut, dateFin);
        if (ecritures.isEmpty()) {
            throw new IllegalArgumentException("Aucune écriture comptable trouvée pour l'année " + annee);
        }

        // Vérifier s'il y a des écritures non validées (brouillon)
        boolean haveDraft = ecritures.stream().anyMatch(e -> !e.getValide());
        if (haveDraft) {
            throw new IllegalStateException("Impossible de clôturer : il existe des écritures de type 'Draft' non validées.");
        }

        // 2. Calculer le solde de chaque compte (Balance de l'exercice)
        Map<String, BigDecimal> balances = new HashMap<>();
        for (EcritureEntity e : ecritures) {
            for (LigneEcritureEntity l : e.getLignes()) {
                String compte = l.getCompteCode();
                BigDecimal debit = l.getDebit();
                BigDecimal credit = l.getCredit();
                BigDecimal soldeCourant = balances.getOrDefault(compte, BigDecimal.ZERO);
                balances.put(compte, soldeCourant.add(debit).subtract(credit));
            }
        }

        // 3. Séparer comptes de gestion (classes 6 et 7) et comptes de bilan (classes 1 à 5)
        Map<String, BigDecimal> gestionBalances = new HashMap<>();
        Map<String, BigDecimal> bilanBalances = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();
            if (solde.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            char premierChiffre = code.charAt(0);
            if (premierChiffre == '6' || premierChiffre == '7' || premierChiffre == '8') {
                gestionBalances.put(code, solde);
            } else if (premierChiffre >= '1' && premierChiffre <= '5') {
                bilanBalances.put(code, solde);
            }
        }

        if (gestionBalances.isEmpty() && bilanBalances.isEmpty()) {
            log.info("Tous les comptes sont déjà à zéro pour l'année {}.", annee);
            throw new IllegalStateException("Tous les comptes sont déjà à zéro pour l'année "+annee+".");
        }

        // 4. Déterminer le résultat (Somme des Produits - Somme des Charges)
        // Note : Dans balances, Solde = Débit - Crédit.
        // Pour les comptes de charges (classe 6), solde > 0 (Débit > Crédit) -> Charge.
        // Pour les comptes de produits (classe 7), solde < 0 (Crédit > Débit) -> Produit.
        // Résultat = - Somme(soldes classe 6 et 7)
        BigDecimal totalGestion = BigDecimal.ZERO;
        for (BigDecimal solde : gestionBalances.values()) {
            totalGestion = totalGestion.add(solde);
        }
        BigDecimal resultatNet = totalGestion.negate(); // Si totalGestion est négatif (Crédit > Débit), resultatNet > 0 (Bénéfice)

        log.info("Résultat Net déterminé pour l'exercice {}: {}", annee, resultatNet);

        // 5. Générer l'écriture de solde des comptes de gestion (Clôture)
        JournalEntity journalOD = journalRepository.findByCode(CodeJournal.OD)
                .orElseGet(() -> journalRepository.save(JournalEntity.builder()
                        .code(CodeJournal.OD)
                        .intitule("Opérations Diverses")
                        .typeJournal("OD")
                        .actif(true)
                        .build()));

        EcritureEntity ecritureCloture = EcritureEntity.builder()
                .journal(journalOD)
                .numeroPiece("CLOTURE-" + annee)
                .libelle("Détermination du résultat de l'exercice " + annee)
                .dateComptable(dateFin)
                .valide(true)
                .build();

        // Pour chaque compte de gestion, passer l'écriture inverse pour le ramener à zéro
        for (Map.Entry<String, BigDecimal> entry : gestionBalances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (solde.compareTo(BigDecimal.ZERO) > 0) {
                // Solde débiteur -> On crédite pour solder
                credit = solde;
            } else {
                // Solde créditeur -> On débite pour solder
                debit = solde.abs();
            }

            ecritureCloture.addLigne(LigneEcritureEntity.builder()
                    .compteCode(code)
                    .debit(debit)
                    .credit(credit)
                    .libelleLigne("Solde du compte " + code + " pour clôture")
                    .build());
        }

        // Ligne de résultat net (131 ou 139)
        String compteResultatCode;
        BigDecimal debitResultat = BigDecimal.ZERO;
        BigDecimal creditResultat = BigDecimal.ZERO;

        if (resultatNet.compareTo(BigDecimal.ZERO) >= 0) {
            // Bénéfice -> Crédit du compte 131
            compteResultatCode = "131";
            creditResultat = resultatNet;
        } else {
            // Perte -> Débit du compte 139
            compteResultatCode = "139";
            debitResultat = resultatNet.abs();
        }

        ecritureCloture.addLigne(LigneEcritureEntity.builder()
                .compteCode(compteResultatCode)
                .debit(debitResultat)
                .credit(creditResultat)
                .libelleLigne("Résultat net de l'exercice " + annee)
                .build());

        ecritureRepository.save(ecritureCloture);
        log.info("Écriture de clôture générée et enregistrée.");

        // 6. Générer les écritures de réouverture (À-nouveau) pour l'exercice suivant
        JournalEntity journalRAN = journalRepository.findByCode(CodeJournal.RAN)
                .orElseGet(() -> journalRepository.save(JournalEntity.builder()
                        .code(CodeJournal.RAN)
                        .intitule("Report à Nouveau")
                        .typeJournal("RAN")
                        .actif(true)
                        .build()));

        EcritureEntity ecritureOuverture = EcritureEntity.builder()
                .journal(journalRAN)
                .numeroPiece("RAN-" + (annee + 1))
                .libelle("Bilans d'ouverture - Exercice " + (annee + 1))
                .dateComptable(dateOuvertureSuivante)
                .valide(true)
                .build();

        // Répercuter les soldes de bilan (classes 1 à 5)
        // Plus le résultat net de l'exercice clôturé
        bilanBalances.put(compteResultatCode, bilanBalances.getOrDefault(compteResultatCode, BigDecimal.ZERO).add(resultatNet));

        for (Map.Entry<String, BigDecimal> entry : bilanBalances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();

            if (solde.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (solde.compareTo(BigDecimal.ZERO) > 0) {
                // Solde débiteur -> Reste débiteur au bilan d'ouverture
                debit = solde;
            } else {
                // Solde créditeur -> Reste créditeur au bilan d'ouverture
                credit = solde.abs();
            }

            ecritureOuverture.addLigne(LigneEcritureEntity.builder()
                    .compteCode(code)
                    .debit(debit)
                    .credit(credit)
                    .libelleLigne("Report à nouveau " + (annee + 1))
                    .build());
        }

        ecritureRepository.save(ecritureOuverture);
        log.info("Écriture de réouverture (À-nouveau) générée pour l'année {}.", annee + 1);
    }

    private void validation(int annee) {
        LocalDate dateDebut = LocalDate.of(annee, 1, 1);
        LocalDate dateFin = LocalDate.of(annee, 12, 31);

        // 1. Récupérer toutes les écritures validées de l'année
        List<EcritureEntity> ecritures = ecritureRepository.findByDateComptableBetweenWithLignes(dateDebut, dateFin);
        if (ecritures.isEmpty()) {
            throw new IllegalArgumentException("Aucune écriture comptable trouvée pour l'année " + annee);
        }

        // Vérifier s'il y a des écritures non validées (brouillon)
        boolean haveDraft = ecritures.stream().anyMatch(e -> !e.getValide());
        if (haveDraft) {
            throw new IllegalStateException("Impossible de clôturer : il existe des écritures de type 'Draft' non validées.");
        }
        // 2. Calculer le solde de chaque compte (Balance de l'exercice)
        Map<String, BigDecimal> balances = new HashMap<>();
        for (EcritureEntity e : ecritures) {
            for (LigneEcritureEntity l : e.getLignes()) {
                String compte = l.getCompteCode();
                BigDecimal debit = l.getDebit();
                BigDecimal credit = l.getCredit();
                BigDecimal soldeCourant = balances.getOrDefault(compte, BigDecimal.ZERO);
                balances.put(compte, soldeCourant.add(debit).subtract(credit));
            }
        }

        // 3. Séparer comptes de gestion (classes 6 et 7) et comptes de bilan (classes 1 à 5)
        Map<String, BigDecimal> gestionBalances = new HashMap<>();
        Map<String, BigDecimal> bilanBalances = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();
            if (solde.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            char premierChiffre = code.charAt(0);
            if (premierChiffre == '6' || premierChiffre == '7' || premierChiffre == '8') {
                gestionBalances.put(code, solde);
            } else if (premierChiffre >= '1' && premierChiffre <= '5') {
                bilanBalances.put(code, solde);
            }
        }

        if (gestionBalances.isEmpty() && bilanBalances.isEmpty()) {
            log.info("Tous les comptes sont déjà à zéro pour l'année {}.", annee);
            throw new IllegalStateException("Tous les comptes sont déjà à zéro pour l'année "+annee+".");
        }

    }

    private void validation(MakerCheckerOutput request) {
        makerCheckerEngine.validate(request.id(), request.createdAt(), request.expiredAt());
        Map<String, Integer> map = objectMapper.convertValue(request.payload(), Map.class);
        validation(map.get(YEAR));

    }

    /**
     * Effectue une simulation la clôture annuelle automatique pour une année donnée.
     * Cette méthode solde les comptes de charges/produits, calcule le résultat, passe l'écriture de clôture,
     * et crée l'écriture d'ouverture (À-nouveau) au premier jour de l'année suivante.
     *
     * @param annee l'année civile de l'exercice à clôturer.
     * @throws IllegalArgumentException si aucune écriture n'est trouvée pour l'année.
     * @throws IllegalStateException s'il existe des écritures à l'état de brouillon non validées.
     */
    @Transactional(readOnly = true)
    public Map<String, EcritureOutput> fiscalYearClosingSimulation(int annee) {
        log.info("Début du processus de clôture automatique pour l'année {}", annee);

        validation(annee);

        LocalDate dateDebut = LocalDate.of(annee, 1, 1);
        LocalDate dateFin = LocalDate.of(annee, 12, 31);
        LocalDate dateOuvertureSuivante = LocalDate.of(annee + 1, 1, 1);

        // 1. Récupérer toutes les écritures validées de l'année
        List<EcritureEntity> ecritures = ecritureRepository.findByDateComptableBetweenWithLignes(dateDebut, dateFin);
        // 2. Calculer le solde de chaque compte (Balance de l'exercice)
        Map<String, BigDecimal> balances = new HashMap<>();
        for (EcritureEntity e : ecritures) {
            for (LigneEcritureEntity l : e.getLignes()) {
                String compte = l.getCompteCode();
                BigDecimal debit = l.getDebit();
                BigDecimal credit = l.getCredit();
                BigDecimal soldeCourant = balances.getOrDefault(compte, BigDecimal.ZERO);
                balances.put(compte, soldeCourant.add(debit).subtract(credit));
            }
        }

        // 3. Séparer comptes de gestion (classes 6 et 7) et comptes de bilan (classes 1 à 5)
        Map<String, BigDecimal> gestionBalances = new HashMap<>();
        Map<String, BigDecimal> bilanBalances = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();
            if (solde.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            char premierChiffre = code.charAt(0);
            if (premierChiffre == '6' || premierChiffre == '7' || premierChiffre == '8') {
                gestionBalances.put(code, solde);
            } else if (premierChiffre >= '1' && premierChiffre <= '5') {
                bilanBalances.put(code, solde);
            }
        }

        BigDecimal totalGestion = BigDecimal.ZERO;
        for (BigDecimal solde : gestionBalances.values()) {
            totalGestion = totalGestion.add(solde);
        }
        BigDecimal resultatNet = totalGestion.negate(); // Si totalGestion est négatif (Crédit > Débit), resultatNet > 0 (Bénéfice)

        log.info("Résultat Net déterminé pour l'exercice {}: {}", annee, resultatNet);

        // 5. Générer l'écriture de solde des comptes de gestion (Clôture)
        JournalEntity journalOD = journalRepository.findByCode(CodeJournal.OD)
                .orElseGet(() -> JournalEntity.builder()
                        .code(CodeJournal.OD)
                        .intitule("Opérations Diverses")
                        .typeJournal("OD")
                        .actif(true)
                        .build());

        Map<String, EcritureOutput> map = new HashMap<String, EcritureOutput>(0);

        EcritureEntity ecritureCloture = EcritureEntity.builder()
                .journal(journalOD)
                .numeroPiece("CLOTURE-" + annee)
                .libelle("Détermination du résultat de l'exercice " + annee)
                .dateComptable(dateFin)
                .valide(true)
                .build();

        // Pour chaque compte de gestion, passer l'écriture inverse pour le ramener à zéro
        for (Map.Entry<String, BigDecimal> entry : gestionBalances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (solde.compareTo(BigDecimal.ZERO) > 0) {
                // Solde débiteur -> On crédite pour solder
                credit = solde;
            } else {
                // Solde créditeur -> On débite pour solder
                debit = solde.abs();
            }

            ecritureCloture.addLigne(LigneEcritureEntity.builder()
                    .compteCode(code)
                    .debit(debit)
                    .credit(credit)
                    .libelleLigne("Solde du compte " + code + " pour clôture")
                    .build());
        }

        // Ligne de résultat net (131 ou 139)
        String compteResultatCode;
        BigDecimal debitResultat = BigDecimal.ZERO;
        BigDecimal creditResultat = BigDecimal.ZERO;

        if (resultatNet.compareTo(BigDecimal.ZERO) >= 0) {
            // Bénéfice -> Crédit du compte 131
            compteResultatCode = "131";
            creditResultat = resultatNet;
        } else {
            // Perte -> Débit du compte 139
            compteResultatCode = "139";
            debitResultat = resultatNet.abs();
        }

        ecritureCloture.addLigne(LigneEcritureEntity.builder()
                .compteCode(compteResultatCode)
                .debit(debitResultat)
                .credit(creditResultat)
                .libelleLigne("Résultat net de l'exercice " + annee)
                .build());



        // 6. Générer les écritures de réouverture (À-nouveau) pour l'exercice suivant
        JournalEntity journalRAN = journalRepository.findByCode(CodeJournal.RAN)
                .orElseGet(() -> JournalEntity.builder()
                        .code(CodeJournal.RAN)
                        .intitule("Report à Nouveau")
                        .typeJournal("RAN")
                        .actif(true)
                        .build());

        EcritureEntity ecritureOuverture = EcritureEntity.builder()
                .journal(journalRAN)
                .numeroPiece("RAN-" + (annee + 1))
                .libelle("Bilans d'ouverture - Exercice " + (annee + 1))
                .dateComptable(dateOuvertureSuivante)
                .valide(true)
                .build();

        // Répercuter les soldes de bilan (classes 1 à 5)
        // Plus le résultat net de l'exercice clôturé
        bilanBalances.put(compteResultatCode, bilanBalances.getOrDefault(compteResultatCode, BigDecimal.ZERO).add(resultatNet));

        for (Map.Entry<String, BigDecimal> entry : bilanBalances.entrySet()) {
            String code = entry.getKey();
            BigDecimal solde = entry.getValue();

            if (solde.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (solde.compareTo(BigDecimal.ZERO) > 0) {
                // Solde débiteur -> Reste débiteur au bilan d'ouverture
                debit = solde;
            } else {
                // Solde créditeur -> Reste créditeur au bilan d'ouverture
                credit = solde.abs();
            }

            ecritureOuverture.addLigne(LigneEcritureEntity.builder()
                    .compteCode(code)
                    .debit(debit)
                    .credit(credit)
                    .libelleLigne("Report à nouveau " + (annee + 1))
                    .build());
        }

        log.info("Écriture de réouverture (À-nouveau) générée pour l'année {}.", annee + 1);

        Map<String, EcritureOutput> result = new HashMap<>();
        result.put("cloture", mapToOutput(ecritureCloture));
        result.put("ouverture", mapToOutput(ecritureOuverture));
        return result;
    }

    private EcritureOutput mapToOutput(EcritureEntity entity) {
        if (entity == null) return null;
        JournalOutput journalOutput = entity.getJournal() != null ? JournalOutput.builder()
                .id(entity.getJournal().getId())
                .code(entity.getJournal().getCode())
                .intitule(entity.getJournal().getIntitule())
                .typeJournal(entity.getJournal().getTypeJournal())
                .actif(entity.getJournal().getActif())
                .build() : null;

        return new EcritureOutput(
                entity.getId(),
                journalOutput,
                entity.getNumeroPiece(),
                entity.getLibelle(),
                entity.getDateComptable(),
                entity.getDateSaisie(),
                entity.getValide(),
                entity.getStatut(),
                null,
                entity.getDateValidation(),
                entity.getTypeDevise(),
                entity.getTauxChange(),
                entity.getLignes()
        );
    }

    private Map<String, Object> toPayload(Map<String, Integer> input) {
        return objectMapper.convertValue(input, new TypeReference<>() {});
    }

}
