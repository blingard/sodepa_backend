package com.sodepa.erp.budgetaire.source;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.source.application.inputs.CreerSourceInput;
import com.sodepa.erp.budgetaire.source.application.inputs.ModifierSourceInput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.SourceFinancementEntity;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import com.sodepa.erp.budgetaire.source.infrastructure.event.DemandeSourceEvent;
import com.sodepa.erp.budgetaire.source.infrastructure.repo.SourceFinancementRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.ApiException;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.workflow.application.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Règles de US-BUD-108 pour les sources de financement. */
@ExtendWith(MockitoExtension.class)
class SourceFinancementAdapterTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-23T09:00:00Z"), ZoneId.of("Africa/Douala"));
    private static final String CHEF_BUDGET = "11111111-1111-1111-1111-111111111111";
    private static final String VALIDATEUR = "22222222-2222-2222-2222-222222222222";

    @Mock private SourceFinancementRepository sourceRepository;
    @Mock private MakerCheckerRequestJpaRepo demandeRepository;
    @Mock private WorkflowService workflowService;
    @Mock private UtilsService utilsService;
    @Mock private UserRepository utilisateurRepository;
    @Mock private UsageReferentiels usageReferentiels;

    private SourceFinancementAdapter adapter;

    @BeforeEach
    void preparer() {
        adapter = new SourceFinancementAdapter(sourceRepository,
                new CircuitReferentielBudgetaire(demandeRepository, workflowService, utilsService, utilisateurRepository, HORLOGE),
                utilsService, usageReferentiels, HORLOGE);
        lenient().when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(List.of());
    }

    private static SourceFinancementEntity source(String code, TypeSourceFinancement type, String bailleur, boolean actif) {
        return SourceFinancementEntity.builder().id(SourceFinancementAdapter.identifiantPour(code)).code(code)
                .libelle("Source " + code).type(type).bailleur(bailleur).actif(actif)
                .creePar(CHEF_BUDGET).creeLe(HORLOGE.instant()).validePar(VALIDATEUR).valideLe(HORLOGE.instant())
                .version(0L).build();
    }

    private void connecte(String userId) {
        when(utilsService.getCurrentUserData()).thenReturn(new UserData(
                "u", "Utilisateur", userId, Set.of(), "u@sodepa.cm", Set.of(), "s", "jwt", "iam",
                Set.of(), false, false, false));
    }

    private static MakerCheckerRequestEntity demande(UUID sourceId, DemandeSourceEvent charge) {
        return MakerCheckerRequestEntity.builder()
                .id(UUID.randomUUID()).entityName(MakerCheckerEntityName.BUDGET_SOURCE_FINANCEMENT)
                .entityPk(sourceId.toString()).makerId(CHEF_BUDGET).status(MakerCheckerStatus.PENDING)
                .payload(charge.versCharge()).checkerOperationType(charge.operation().typeMakerChecker())
                .createdAt(HORLOGE.instant().minus(1, ChronoUnit.HOURS))
                .expiredAt(HORLOGE.instant().plus(10, ChronoUnit.DAYS)).build();
    }

    private static void assertCode(String code, ApiException e) {
        assertEquals(code, e.getCode());
    }

    @Test
    @DisplayName("Ressources propres : la demande part au maker-checker, sans bailleur")
    void soumetLesRessourcesPropres() {
        UUID demandeId = UUID.randomUUID();
        UUID sourceId = SourceFinancementAdapter.identifiantPour("RP");
        when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(demandeId);

        SubmissionOutput accuse = adapter.initCreer(
                new CreerSourceInput("rp", "Ressources propres", TypeSourceFinancement.RESSOURCES_PROPRES, " "));

        assertEquals(new SubmissionOutput(demandeId, sourceId), accuse);
        verify(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_SOURCE_FINANCEMENT);
        verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_SOURCE_FINANCEMENT, sourceId.toString(),
                Map.of("operation", "CREER", "code", "RP", "libelle", "Ressources propres", "type", "RESSOURCES_PROPRES"),
                MakerCheckerOperationType.CREATE);
    }

    @Test
    @DisplayName("Un partenaire sans bailleur nommé est refusé")
    void refuseUnPartenaireSansBailleur() {
        RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                new CreerSourceInput("BAD", "Financement BAD", TypeSourceFinancement.PARTENAIRE, null)));

        assertCode("BAILLEUR_OBLIGATOIRE", e);
        verifyNoInteractions(workflowService);
    }

    @Test
    @DisplayName("Un code déjà pris est refusé")
    void refuseUnCodeExistant() {
        when(sourceRepository.existsByCode("RP")).thenReturn(true);

        ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                new CreerSourceInput("RP", "Ressources propres", TypeSourceFinancement.RESSOURCES_PROPRES, null)));

        assertCode("CODE_SOURCE_EXISTANT", e);
    }

    @Test
    @DisplayName("Le bailleur d'un partenaire ne s'efface pas")
    void refuseDEffacerLeBailleurDUnPartenaire() {
        SourceFinancementEntity s = source("BAD", TypeSourceFinancement.PARTENAIRE, "Banque africaine de développement", true);
        when(sourceRepository.findById(s.getId())).thenReturn(Optional.of(s));

        RequeteInvalideException e = assertThrows(RequeteInvalideException.class,
                () -> adapter.initModifier(new ModifierSourceInput(s.getId(), "Financement BAD", null)));

        assertCode("BAILLEUR_OBLIGATOIRE", e);
    }

    @Test
    @DisplayName("Une source qui finance des lignes d'un exercice en cours ne se désactive pas")
    void refuseUneSourceUtilisee() {
        SourceFinancementEntity s = source("RP", TypeSourceFinancement.RESSOURCES_PROPRES, null, true);
        when(sourceRepository.findById(s.getId())).thenReturn(Optional.of(s));
        when(usageReferentiels.sourceUtilisee(s.getId())).thenReturn(true);

        ConflitMetierException e = assertThrows(ConflitMetierException.class,
                () -> adapter.initDesactiver(new ChangerEtatInput(s.getId(), "Plus utilisée")));

        assertCode("SOURCE_UTILISEE", e);
    }

    @Test
    @DisplayName("Une source active ne se réactive pas")
    void refuseDeReactiverUneSourceActive() {
        SourceFinancementEntity s = source("RP", TypeSourceFinancement.RESSOURCES_PROPRES, null, true);
        when(sourceRepository.findById(s.getId())).thenReturn(Optional.of(s));

        ConflitMetierException e = assertThrows(ConflitMetierException.class,
                () -> adapter.initReactiver(new ChangerEtatInput(s.getId(), null)));

        assertCode("SOURCE_DEJA_ACTIVE", e);
    }

    @Test
    @DisplayName("Validation d'une création : la source naît active, avec auteur et validateur")
    void valideUneCreation() {
        UUID sourceId = SourceFinancementAdapter.identifiantPour("BAD");
        MakerCheckerRequestEntity demande = demande(sourceId, DemandeSourceEvent.creation(
                "BAD", "Financement BAD", TypeSourceFinancement.PARTENAIRE, "Banque africaine de développement"));
        when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
        connecte(VALIDATEUR);

        adapter.decider(new DeciderDemandeInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

        ArgumentCaptor<SourceFinancementEntity> creee = ArgumentCaptor.forClass(SourceFinancementEntity.class);
        verify(sourceRepository).saveAndFlush(creee.capture());
        SourceFinancementEntity s = creee.getValue();
        assertEquals("BAD", s.getCode());
        assertEquals("Banque africaine de développement", s.getBailleur());
        assertTrue(s.isActif());
        assertEquals(VALIDATEUR, s.getValidePar());
        verify(workflowService).update(demande.getId(), MakerCheckerStatus.ACCEPTED, null, VALIDATEUR);
    }

    @Test
    @DisplayName("Validation d'une désactivation : la source devient inactive et garde le motif")
    void valideUneDesactivation() {
        SourceFinancementEntity s = source("RP", TypeSourceFinancement.RESSOURCES_PROPRES, null, true);
        MakerCheckerRequestEntity demande = demande(s.getId(),
                DemandeSourceEvent.changementEtat(OperationReferentiel.DESACTIVER, "Plus utilisée"));
        when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
        when(sourceRepository.findById(s.getId())).thenReturn(Optional.of(s));
        connecte(VALIDATEUR);

        adapter.decider(new DeciderDemandeInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

        assertFalse(s.isActif());
        assertEquals("Plus utilisée", s.getMotifDesactivation());
        verify(sourceRepository).saveAndFlush(s);
    }

    @Test
    @DisplayName("Un rejet motivé ne crée rien")
    void rejet() {
        UUID sourceId = SourceFinancementAdapter.identifiantPour("RP");
        MakerCheckerRequestEntity demande = demande(sourceId, DemandeSourceEvent.creation(
                "RP", "Ressources propres", TypeSourceFinancement.RESSOURCES_PROPRES, null));
        when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
        connecte(VALIDATEUR);

        adapter.decider(new DeciderDemandeInput(demande.getId(), MakerCheckerStatus.REJECTED, "Doublon"));

        verifyNoInteractions(sourceRepository);
        verify(workflowService).update(demande.getId(), MakerCheckerStatus.REJECTED, "Doublon", VALIDATEUR);
    }
}
