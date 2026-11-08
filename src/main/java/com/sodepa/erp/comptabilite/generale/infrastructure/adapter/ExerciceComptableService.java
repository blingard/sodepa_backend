package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ExerciceComptableEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.PeriodeComptableEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.SchemaEcritureComptableEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.ExerciceComptableRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.PeriodeComptableRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.SchemaEcritureComptableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service métier de gestion des Exercices Comptables, des 12 Périodes Mensuelles et des Schémas d'écritures
 * (REF-04 / F-REF-017 / CA-REF-017-03 / RG-REF-023).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciceComptableService {

    private final ExerciceComptableRepository exerciceRepository;
    private final PeriodeComptableRepository periodeRepository;
    private final SchemaEcritureComptableRepository schemaRepository;

    /**
     * Exécute l'opération de lister exercices.
     * @return Résultat du traitement (List<ExerciceComptableEntity>).
     */
    @Transactional(readOnly = true)
    public List<ExerciceComptableEntity> listerExercices() {
        return exerciceRepository.findAllByOrderByAnneeDesc();
    }

    /**
     * Exécute l'opération de obtenir exercice par annee.
     * @param annee Annee dans le contexte du traitement obtenirExerciceParAnnee.
     * @return Résultat du traitement (ExerciceComptableEntity).
     */
    @Transactional(readOnly = true)
    public ExerciceComptableEntity obtenirExerciceParAnnee(int annee) {
        return exerciceRepository.findByAnnee(annee)
                .orElseThrow(() -> new IllegalArgumentException("Exercice comptable introuvable pour l'année " + annee));
    }

    /**
     * Ouvre un nouvel exercice comptable et génère automatiquement ses 12 périodes mensuelles (CA-REF-017-03).
     */
    @Transactional
    public ExerciceComptableEntity ouvrirExercice(int annee, String libelle, String journauxAutorises) {
        if (exerciceRepository.findByAnnee(annee).isPresent()) {
            throw new IllegalArgumentException("L'exercice comptable " + annee + " existe déjà.");
        }

        String journaux = (journauxAutorises != null && !journauxAutorises.isBlank())
                ? journauxAutorises
                : "ACHAT,VENTE,BANQUE,CAISSE,OD,ANOUVEAU";

        ExerciceComptableEntity exercice = ExerciceComptableEntity.builder()
                .code("EX-" + annee)
                .annee(annee)
                .libelle(libelle != null && !libelle.isBlank() ? libelle : "Exercice Comptable SODEPA " + annee)
                .dateDebut(LocalDate.of(annee, 1, 1))
                .dateFin(LocalDate.of(annee, 12, 31))
                .statut("OUVERT")
                .periodes(new ArrayList<>())
                .build();

        for (int mois = 1; mois <= 12; mois++) {
            YearMonth ym = YearMonth.of(annee, mois);
            String nomMois = Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
            String nomCap = nomMois.substring(0, 1).toUpperCase() + nomMois.substring(1);

            PeriodeComptableEntity periode = PeriodeComptableEntity.builder()
                    .exercice(exercice)
                    .numeroMois(mois)
                    .codePeriode(String.format("%d-%02d", annee, mois))
                    .libelle(nomCap + " " + annee)
                    .dateDebut(ym.atDay(1))
                    .dateFin(ym.atEndOfMonth())
                    .statut("OUVERTE")
                    .journauxAutorises(journaux)
                    .build();
            exercice.getPeriodes().add(periode);
        }

        ExerciceComptableEntity saved = exerciceRepository.save(exercice);
        log.info("Exercice comptable {} ouvert avec ses 12 périodes mensuelles (CA-REF-017-03).", annee);
        return saved;
    }

    /**
     * Exécute l'opération de lister periodes exercice.
     * @param annee Annee dans le contexte du traitement listerPeriodesExercice.
     * @return Résultat du traitement (List<PeriodeComptableEntity>).
     */
    @Transactional(readOnly = true)
    public List<PeriodeComptableEntity> listerPeriodesExercice(int annee) {
        return periodeRepository.findByExerciceAnneeOrderByNumeroMoisAsc(annee);
    }

    /**
     * Exécute l'opération de changer statut periode.
     * @param codePeriode Code mnémonique unique normalisé.
     * @param nouveauStatut Statut actuel dans le cycleée de vie métier.
     * @return Résultat du traitement (PeriodeComptableEntity).
     */
    @Transactional
    public PeriodeComptableEntity changerStatutPeriode(String codePeriode, String nouveauStatut) {
        PeriodeComptableEntity periode = periodeRepository.findByCodePeriode(codePeriode)
                .orElseThrow(() -> new IllegalArgumentException("Période comptable introuvable : " + codePeriode));
        if ("CLOTUREE".equalsIgnoreCase(periode.getStatut()) && !"CLOTUREE".equalsIgnoreCase(nouveauStatut)) {
            throw new IllegalStateException("Interdiction (RG-REF-023) : une période comptable définitivement cleéôturée ne peut pas être réouverte.");
        }
        periode.setStatut(nouveauStatut.toUpperCase());
        return periodeRepository.save(periode);
    }

    /**
     * Exécute l'opération de cleéoturer exercice et periodes.
     * @param annee Annee dans le contexte du traitement cleéoturerExerciceEtPeriodes.
     * @return Résultat du traitement (ExerciceComptableEntity).
     */
    @Transactional
    public ExerciceComptableEntity cleéoturerExerciceEtPeriodes(int annee) {
        ExerciceComptableEntity exercice = obtenirExerciceParAnnee(annee);
        exercice.setStatut("CLOTURE_DEFINITIF");
        exercice.setDateCloture(Instant.now());
        if (exercice.getPeriodes() != null) {
            exercice.getPeriodes().forEach(p -> p.setStatut("CLOTUREE"));
        }
        return exerciceRepository.save(exercice);
    }

    // --- SCHÉMAS D'ÉCRITURES COMPTABLES AUTOMATIQUES (BF-REF-025) ---

    /**
     * Exécute l'opération de lister schemas ecritures.
     * @return Résultat du traitement (List<SchemaEcritureComptableEntity>).
     */
    @Transactional(readOnly = true)
    public List<SchemaEcritureComptableEntity> listerSchemasEcritures() {
        return schemaRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Exécute l'opération de creer schema ecriture.
     * @param schema Schema dans le contexte du traitement creerSchemaEcriture.
     * @return Résultat du traitement (SchemaEcritureComptableEntity).
     */
    @Transactional
    public SchemaEcritureComptableEntity creerSchemaEcriture(SchemaEcritureComptableEntity schema) {
        return schemaRepository.save(schema);
    }
}
