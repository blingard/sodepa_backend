package com.sodepa.erp.comptabilite.generale;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import com.sodepa.erp.utils.Devise;
import com.sodepa.erp.utils.StatutEcriture;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CAS DE TEST DE RÉFÉRENCE — à recopier comme modèle.
 *
 * <p>Vérifie la règle de partie double SYSCOHADA : une pièce comptable ne peut être
 * validée que si la somme des débits est strictement égale à la somme des crédits.</p>
 *
 * <p>Ce que ce fichier illustre pour l'équipe :</p>
 * <ul>
 *   <li>la hiérarchie {@code @Epic} / {@code @Feature} / {@code @Story} qui structure
 *       l'arbre « Behaviors » du rapport ;</li>
 *   <li>{@code @Step} pour découper l'exécution en étapes lisibles ;</li>
 *   <li>{@code @Attachment} pour joindre le jeu de données au rapport ;</li>
 *   <li>{@code Allure.parameter} pour tracer les valeurs d'entrée ;</li>
 *   <li>le tag JUnit {@code unit} : aucun conteneur, aucune base, exécutable en CI.</li>
 * </ul>
 */
@Tag("unit")
@Epic("Comptabilité générale")
@Feature("Écritures comptables")
@Owner("equipe-comptabilite")
@DisplayName("Équilibre des écritures comptables (partie double)")
class EcritureEquilibreTest {

    @Test
    @Story("Une écriture équilibrée passe le contrôle de validation")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("SODEPA-1")
    @DisplayName("Débit = crédit : la validation est acceptée")
    @Description("""
            Une écriture d'achat de 1 000 000 XAF est enregistrée avec une ligne au débit
            (compte de charge 6011) et une ligne au crédit (compte fournisseur 4011).
            Les deux montants étant identiques, validateEquilibre() ne doit lever aucune exception.
            """)
    void ecritureEquilibree_estAcceptee() {
        EcritureEntity ecriture = nouvelleEcriture("ACH-2026-001", "Achat de matières premières");
        ajouterLigne(ecriture, "6011", new BigDecimal("1000000.0000"), BigDecimal.ZERO, "Achat matières");
        ajouterLigne(ecriture, "4011", BigDecimal.ZERO, new BigDecimal("1000000.0000"), "Dette fournisseur");

        joindreEcriture(ecriture);

        assertThatCode(ecriture::validateEquilibre)
                .as("une écriture équilibrée ne doit pas être rejetée")
                .doesNotThrowAnyException();

        assertThat(ecriture.getStatut()).isEqualTo(StatutEcriture.BROUILLON);
        assertThat(ecriture.getTypeDevise()).isEqualTo(Devise.XAF);
    }

    @Test
    @Story("Une écriture déséquilibrée est rejetée")
    @Severity(SeverityLevel.CRITICAL)
    @TmsLink("SODEPA-2")
    @Issue("1")
    @DisplayName("Débit ≠ crédit : la validation échoue avec un message explicite")
    void ecritureDesequilibree_estRejetee() {
        EcritureEntity ecriture = nouvelleEcriture("ACH-2026-002", "Achat mal saisi");
        ajouterLigne(ecriture, "6011", new BigDecimal("1000000"), BigDecimal.ZERO, "Achat matières");
        ajouterLigne(ecriture, "4011", BigDecimal.ZERO, new BigDecimal("900000"), "Dette fournisseur");

        joindreEcriture(ecriture);

        assertThatThrownBy(ecriture::validateEquilibre)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non équilibrée")
                .hasMessageContaining("1000000")
                .hasMessageContaining("900000");
    }

    @Test
    @Story("Une écriture équilibrée passe le contrôle de validation")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("SODEPA-3")
    @DisplayName("Écriture multi-lignes : l'équilibre est évalué sur les totaux")
    void ecritureMultiLignes_equilibreSurLesTotaux() {
        EcritureEntity ecriture = nouvelleEcriture("VTE-2026-010", "Vente avec TVA");
        ajouterLigne(ecriture, "4111", new BigDecimal("1195000"), BigDecimal.ZERO, "Créance client TTC");
        ajouterLigne(ecriture, "7011", BigDecimal.ZERO, new BigDecimal("1000000"), "Vente de marchandises");
        ajouterLigne(ecriture, "4431", BigDecimal.ZERO, new BigDecimal("195000"), "TVA collectée 19,25 %");

        joindreEcriture(ecriture);

        assertThatCode(ecriture::validateEquilibre).doesNotThrowAnyException();
        assertThat(ecriture.getLignes()).hasSize(3);
    }

    @ParameterizedTest(name = "débit={0} / crédit={1} → équilibrée = {2}")
    @CsvSource({
            "1000000, 1000000, true",
            "1000000.0000, 1000000, true",
            "1000000,  999999, false",
            "0,             0, true",
            "500000,  1000000, false"
    })
    @Story("Une écriture équilibrée passe le contrôle de validation")
    @Severity(SeverityLevel.NORMAL)
    @TmsLink("SODEPA-4")
    @DisplayName("Table de décision de l'équilibre débit/crédit")
    void tableDeDecisionEquilibre(String debit, String credit, boolean equilibree) {
        Allure.parameter("Débit attendu", debit);
        Allure.parameter("Crédit attendu", credit);

        EcritureEntity ecriture = nouvelleEcriture("TST-2026-001", "Jeu de données paramétré");
        ajouterLigne(ecriture, "6011", new BigDecimal(debit), BigDecimal.ZERO, "Ligne de débit");
        ajouterLigne(ecriture, "4011", BigDecimal.ZERO, new BigDecimal(credit), "Ligne de crédit");

        if (equilibree) {
            assertThatCode(ecriture::validateEquilibre).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(ecriture::validateEquilibre).isInstanceOf(IllegalStateException.class);
        }
    }

    // ─────────────────────────── Étapes réutilisables ───────────────────────────
    // Chaque @Step apparaît comme une ligne dépliable dans le rapport Allure.

    @Step("Créer la pièce {numeroPiece} — {libelle}")
    private EcritureEntity nouvelleEcriture(String numeroPiece, String libelle) {
        return EcritureEntity.builder()
                .id(UUID.randomUUID())
                .numeroPiece(numeroPiece)
                .libelle(libelle)
                .dateComptable(LocalDate.of(2026, 1, 31))
                .build();
    }

    @Step("Ajouter la ligne {compteCode} : débit={debit} crédit={credit}")
    private void ajouterLigne(EcritureEntity ecriture, String compteCode,
                              BigDecimal debit, BigDecimal credit, String libelleLigne) {
        ecriture.addLigne(LigneEcritureEntity.builder()
                .id(UUID.randomUUID())
                .compteCode(compteCode)
                .debit(debit)
                .credit(credit)
                .libelleLigne(libelleLigne)
                .build());
    }

    @Step("Joindre le détail de la pièce au rapport")
    private void joindreEcriture(EcritureEntity ecriture) {
        detailEcriture(ecriture);
    }

    /** Le retour de la méthode est attaché au rapport sous forme de fichier texte. */
    @Attachment(value = "Détail de la pièce comptable", type = "text/plain")
    private String detailEcriture(EcritureEntity ecriture) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pièce   : ").append(ecriture.getNumeroPiece()).append('\n');
        sb.append("Libellé : ").append(ecriture.getLibelle()).append('\n');
        sb.append("Date    : ").append(ecriture.getDateComptable()).append('\n');
        sb.append("Devise  : ").append(ecriture.getTypeDevise()).append("\n\n");
        sb.append(String.format("%-10s %18s %18s  %s%n", "COMPTE", "DÉBIT", "CRÉDIT", "LIBELLÉ"));
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (LigneEcritureEntity ligne : ecriture.getLignes()) {
            sb.append(String.format("%-10s %18s %18s  %s%n",
                    ligne.getCompteCode(), ligne.getDebit(), ligne.getCredit(), ligne.getLibelleLigne()));
            totalDebit = totalDebit.add(ligne.getDebit());
            totalCredit = totalCredit.add(ligne.getCredit());
        }
        sb.append(String.format("%-10s %18s %18s%n", "TOTAUX", totalDebit, totalCredit));
        return sb.toString();
    }
}
