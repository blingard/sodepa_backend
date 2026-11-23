package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.*;
import com.sodepa.erp.utils.StatutEcriture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de génération du Fichier des Écritures Comptables (FEC) pour les contrôles de comptabilité informatisée.
 * Extrait les écritures validées pour un exercice donné au format texte délimité par des barres verticales ("|").
 */
@Service
@RequiredArgsConstructor
public class FecUseCase {

    /**
     * Dépôt de données pour les écritures comptables.
     */
    private final EcritureRepository ecritureRepository;

    /**
     * Dépôt de données pour les comptes du plan comptable.
     */
    private final CompteRepository compteRepository;

    /**
     * Formateur de date standardisé pour le FEC (format AAAAMMJJ, ex: 20261231).
     */
    private static final DateTimeFormatter FEC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Génère le contenu textuel brut du fichier FEC pour une année d'exercice donnée.
     * Le fichier inclut uniquement les écritures validées (statut VALIDE).
     * 
     * @param annee l'année de l'exercice comptable à exporter
     * @return le contenu du fichier FEC sous forme de chaîne de caractères
     */
    @Transactional(readOnly = true)
    public String genererFec(int annee) {
        LocalDate debut = LocalDate.of(annee, 1, 1);
        LocalDate fin = LocalDate.of(annee, 12, 31);
        List<EcritureEntity> ecritures = ecritureRepository
                .findByStatutAndDateComptableBetweenWithDetails(StatutEcriture.VALIDE, debut, fin);

        Map<String, String> compteLibelles = compteRepository.findAll().stream()
                .collect(Collectors.toMap(CompteEntity::getCode, CompteEntity::getIntitule, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        
        // En-tête réglementaire du fichier FEC
        sb.append("JournalCode|JournalLib|EcritureNum|EcritureDate|CompteNum|CompteLib|CompteAuxNum|CompteAuxLib|PieceRef|PieceDate|EcritureLib|Debit|Credit|EcritureLet|DateLet|ValidDate|Montantdevise|Devise\n");

        for (EcritureEntity e : ecritures) {
            String journalCode = e.getJournal() != null && e.getJournal().getCode() != null ? e.getJournal().getCode().name() : "";
            String journalLib = e.getJournal() != null ? e.getJournal().getIntitule() : "";
            String ecritureNum = e.getId().toString();
            String ecritureDate = e.getDateComptable().format(FEC_DATE_FORMATTER);
            String pieceRef = e.getNumeroPiece();
            String pieceDate = e.getDateComptable().format(FEC_DATE_FORMATTER);
            String validDate = e.getDateValidation() != null ? e.getDateValidation().format(FEC_DATE_FORMATTER) : ecritureDate;

            for (LigneEcritureEntity l : e.getLignes()) {
                String compteNum = l.getCompteCode();
                
                // Chercher l'intitulé du compte
                String compteLib = compteLibelles.getOrDefault(compteNum, "Compte inconnu");

                String compteAuxNum = "";
                String compteAuxLib = "";
                if (l.getTiers() != null) {
                    compteAuxNum = l.getTiers().getCode();
                    compteAuxLib = l.getTiers().getRaisonSociale();
                }

                String ecritureLib = l.getLibelleLigne();
                BigDecimal debit = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;

                String montantDevise = "";
                String codeDevise = "";
                if (e.getTypeDevise() != null) {
                    codeDevise = e.getTypeDevise().name();
                    BigDecimal totalLigne = debit.compareTo(BigDecimal.ZERO) > 0 ? debit : credit;
                    if (e.getTauxChange() != null && e.getTauxChange().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal totalDev = totalLigne.divide(e.getTauxChange(), 2, java.math.RoundingMode.HALF_UP);
                        montantDevise = totalDev.toString();
                    }
                }

                // Append line
                sb.append(journalCode).append("|")
                  .append(journalLib).append("|")
                  .append(ecritureNum).append("|")
                  .append(ecritureDate).append("|")
                  .append(compteNum).append("|")
                  .append(compteLib).append("|")
                  .append(compteAuxNum).append("|")
                  .append(compteAuxLib).append("|")
                  .append(pieceRef).append("|")
                  .append(pieceDate).append("|")
                  .append(ecritureLib).append("|")
                  .append(debit.toString()).append("|")
                  .append(credit.toString()).append("|")
                  .append("|") // EcritureLet
                  .append("|") // DateLet
                  .append(validDate).append("|")
                  .append(montantDevise).append("|")
                  .append(codeDevise).append("\n");
            }
        }

        return sb.toString();
    }
}
