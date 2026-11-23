package com.sodepa.erp.budgetaire.nomenclature;

import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ChangerEtatElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.CreerElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.DeciderDemandeNomenclatureInput;
import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ModifierElementInput;
import com.sodepa.erp.budgetaire.nomenclature.application.outputs.NoeudNomenclatureOutput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.ElementProgrammatiqueEntity;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.event.DemandeNomenclatureEvent;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.repo.ElementProgrammatiqueRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.share.erreurs.ApiException;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.workflow.application.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Règles de US-BUD-104 « Gérer les programmes et les actions ».
 *
 * <p>
 * Tests sur simulacres : ils fixent les règles de l'adaptateur (rattachement,
 * unicité du code, désactivation, quatre yeux, contrôles refaits à la
 * validation). La contrainte d'unicité en base et le circuit réel se
 * vérifient sur l'API démarrée.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class NomenclatureAdapterTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-23T09:00:00Z"), ZoneId.of("Africa/Douala"));

    private static final String CHEF_BUDGET = "11111111-1111-1111-1111-111111111111";
    private static final String VALIDATEUR = "22222222-2222-2222-2222-222222222222";
    private static final UUID RESPONSABLE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private ElementProgrammatiqueRepository elementRepository;
    @Mock private MakerCheckerRequestJpaRepo demandeRepository;
    @Mock private WorkflowService workflowService;
    @Mock private UtilsService utilsService;
    @Mock private UserRepository utilisateurRepository;
    @Mock private UsageReferentiels usageReferentiels;

    private NomenclatureAdapter adapter;

    @BeforeEach
    void preparer() {
        adapter = new NomenclatureAdapter(elementRepository,
                new CircuitReferentielBudgetaire(demandeRepository, workflowService, utilsService, HORLOGE), utilsService,
                utilisateurRepository, usageReferentiels, HORLOGE);
    }

    // ─── Outils ─────────────────────────────────────────────────────────────

    private void connecte(String userId) {
        when(utilsService.getCurrentUserData()).thenReturn(new UserData(
                "u", "Utilisateur", userId, Set.of(), "u@sodepa.cm", Set.of(), "s", "jwt", "iam",
                Set.of(), false, false, false));
    }

    private void responsableActif() {
        when(utilisateurRepository.findById(RESPONSABLE)).thenReturn(Optional.of(
                UtilisateurEntity.builder().id(RESPONSABLE).nom("Ndzana").prenom("Paul").actif(true).build()));
    }

    private void aucuneDemandeEnAttente(UUID elementId) {
        when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                elementId.toString(), MakerCheckerEntityName.BUDGET_NOMENCLATURE, MakerCheckerStatus.PENDING))
                .thenReturn(List.of());
    }

    private static ElementProgrammatiqueEntity element(String code, NiveauProgrammatique niveau, UUID parentId,
                                                       boolean actif) {
        return ElementProgrammatiqueEntity.builder()
                .id(NomenclatureAdapter.identifiantPour(code))
                .code(code)
                .libelle("Libellé " + code)
                .niveau(niveau)
                .parentId(parentId)
                .responsableId(niveau == NiveauProgrammatique.PROGRAMME ? RESPONSABLE : null)
                .actif(actif)
                .creePar(CHEF_BUDGET)
                .creeLe(HORLOGE.instant())
                .validePar(VALIDATEUR)
                .valideLe(HORLOGE.instant())
                .version(0L)
                .build();
    }

    private static ElementProgrammatiqueEntity programme(String code, boolean actif) {
        return element(code, NiveauProgrammatique.PROGRAMME, null, actif);
    }

    private static MakerCheckerRequestEntity demande(String auteur, MakerCheckerStatus statut, UUID elementId,
                                                     DemandeNomenclatureEvent charge) {
        return MakerCheckerRequestEntity.builder()
                .id(UUID.randomUUID())
                .entityName(MakerCheckerEntityName.BUDGET_NOMENCLATURE)
                .entityPk(elementId.toString())
                .makerId(auteur)
                .status(statut)
                .payload(charge.versCharge())
                .checkerOperationType(charge.operation().typeMakerChecker())
                .createdAt(HORLOGE.instant().minus(1, ChronoUnit.HOURS))
                .expiredAt(HORLOGE.instant().plus(10, ChronoUnit.DAYS))
                .build();
    }

    private static void assertCode(String code, ApiException e) {
        assertEquals(code, e.getCode());
    }

    // ─── Création ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Demande de création")
    class Creation {

        @Test
        @DisplayName("Création d'un programme : code mis en majuscules, demande soumise au maker-checker")
        void soumetUnProgramme() {
            UUID demandeId = UUID.randomUUID();
            UUID elementId = NomenclatureAdapter.identifiantPour("P01");
            responsableActif();
            when(elementRepository.existsByCode("P01")).thenReturn(false);
            aucuneDemandeEnAttente(elementId);
            when(workflowService.submitChange(eq(MakerCheckerEntityName.BUDGET_NOMENCLATURE), eq(elementId.toString()),
                    any(), eq(MakerCheckerOperationType.CREATE))).thenReturn(demandeId);

            SubmissionOutput accuse = adapter.initCreer(new CreerElementInput(
                    NiveauProgrammatique.PROGRAMME, " p01 ", " Production animale ", null, null, RESPONSABLE));

            assertEquals(new SubmissionOutput(demandeId, elementId), accuse);
            verify(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_NOMENCLATURE);
            verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_NOMENCLATURE, elementId.toString(),
                    Map.of("operation", "CREER", "niveau", "PROGRAMME", "code", "P01",
                            "libelle", "Production animale", "responsableId", RESPONSABLE.toString()),
                    MakerCheckerOperationType.CREATE);
        }

        @Test
        @DisplayName("Action rattachée : la demande porte le programme parent")
        void soumetUneActionRattachee() {
            ElementProgrammatiqueEntity p01 = programme("P01", true);
            UUID elementId = NomenclatureAdapter.identifiantPour("P01-A01");
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));
            when(elementRepository.existsByCode("P01-A01")).thenReturn(false);
            aucuneDemandeEnAttente(elementId);
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(UUID.randomUUID());

            adapter.initCreer(new CreerElementInput(
                    NiveauProgrammatique.ACTION, "P01-A01", "Santé du cheptel", null, p01.getId(), null));

            verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_NOMENCLATURE, elementId.toString(),
                    Map.of("operation", "CREER", "niveau", "ACTION", "code", "P01-A01",
                            "libelle", "Santé du cheptel", "parentId", p01.getId().toString()),
                    MakerCheckerOperationType.CREATE);
        }

        @Test
        @DisplayName("Code en double : refusé, même si l'élément existant est désactivé")
        void refuseUnCodeEnDouble() {
            ElementProgrammatiqueEntity p01 = programme("P01", true);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));
            when(elementRepository.existsByCode("P01-A01")).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.ACTION, "P01-A01", "Doublon", null, p01.getId(), null)));

            assertCode("CODE_NOMENCLATURE_EXISTANT", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une demande déjà en attente sur le même code est refusée")
        void refuseUneSecondeDemande() {
            UUID elementId = NomenclatureAdapter.identifiantPour("P01");
            responsableActif();
            when(elementRepository.existsByCode("P01")).thenReturn(false);
            when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                    elementId.toString(), MakerCheckerEntityName.BUDGET_NOMENCLATURE, MakerCheckerStatus.PENDING))
                    .thenReturn(List.of(demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, elementId,
                            DemandeNomenclatureEvent.creation(NiveauProgrammatique.PROGRAMME, "P01", "x", null, null, RESPONSABLE))));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "P01", "Production animale", null, null, RESPONSABLE)));

            assertCode("NOMENCLATURE_DEMANDE_EN_ATTENTE", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une action sans programme parent est refusée")
        void refuseUneActionSansParent() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.ACTION, "A01", "Orpheline", null, null, null)));

            assertCode("PARENT_INVALIDE", e);
            assertEquals("Une action se rattache à un programme : indiquez-le.", e.getMessage());
        }

        @Test
        @DisplayName("Une action rattachée à une autre action est refusée")
        void refuseUnParentDuMauvaisNiveau() {
            ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, UUID.randomUUID(), true);
            when(elementRepository.findById(a01.getId())).thenReturn(Optional.of(a01));

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.ACTION, "P01-A02", "x", null, a01.getId(), null)));

            assertCode("PARENT_INVALIDE", e);
        }

        @Test
        @DisplayName("Un programme ne se rattache à aucun parent")
        void refuseUnProgrammeAvecParent() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "P02", "x", null, UUID.randomUUID(), RESPONSABLE)));

            assertCode("PARENT_INVALIDE", e);
        }

        @Test
        @DisplayName("Un programme désactivé ne reçoit plus de nouvelle action")
        void refuseUnParentInactif() {
            ElementProgrammatiqueEntity p01 = programme("P01", false);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.ACTION, "P01-A03", "x", null, p01.getId(), null)));

            assertCode("PARENT_INACTIF", e);
        }

        @Test
        @DisplayName("Un programme sans responsable est refusé")
        void refuseUnProgrammeSansResponsable() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "P02", "x", null, null, null)));

            assertCode("RESPONSABLE_INVALIDE", e);
        }

        @Test
        @DisplayName("Un responsable inconnu est refusé")
        void refuseUnResponsableInconnu() {
            when(utilisateurRepository.findById(RESPONSABLE)).thenReturn(Optional.empty());

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "P02", "x", null, null, RESPONSABLE)));

            assertCode("RESPONSABLE_INVALIDE", e);
        }

        @Test
        @DisplayName("Activités et tâches : pas avant US-BUD-105")
        void refuseLesNiveauxNonGeres() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.ACTIVITE, "P01-A01-01", "x", null, UUID.randomUUID(), null)));

            assertCode("NIVEAU_NON_GERE", e);
            verifyNoInteractions(elementRepository, workflowService);
        }

        @Test
        @DisplayName("Un code avec espace ou accent est refusé")
        void refuseUnCodeMalForme() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "P 01", "x", null, null, RESPONSABLE)));
            assertCode("CODE_INVALIDE", e);

            e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "PRÉ01", "x", null, null, RESPONSABLE)));
            assertCode("CODE_INVALIDE", e);
        }

        @Test
        @DisplayName("Sans habilitation : refus avant toute lecture")
        void refuseSansHabilitation() {
            doThrow(new AccesRefuseException("Habilitation requise"))
                    .when(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_NOMENCLATURE);

            assertThrows(AccesRefuseException.class, () -> adapter.initCreer(
                    new CreerElementInput(NiveauProgrammatique.PROGRAMME, "P01", "x", null, null, RESPONSABLE)));

            verifyNoInteractions(elementRepository, workflowService, utilisateurRepository);
        }
    }

    // ─── Modification ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Modification")
    class Modification {

        @Test
        @DisplayName("Le libellé et le responsable se modifient ; la demande porte leur nouvel état")
        void soumetUneModification() {
            ElementProgrammatiqueEntity p01 = programme("P01", true);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));
            responsableActif();
            aucuneDemandeEnAttente(p01.getId());
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(UUID.randomUUID());

            adapter.initModifier(new ModifierElementInput(p01.getId(), "Productions animales", "Élevage", RESPONSABLE));

            verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_NOMENCLATURE, p01.getId().toString(),
                    Map.of("operation", "MODIFIER", "libelle", "Productions animales", "description", "Élevage",
                            "responsableId", RESPONSABLE.toString()),
                    MakerCheckerOperationType.UPDATE);
        }

        @Test
        @DisplayName("Une demande qui ne change rien est refusée")
        void refuseUneModificationVide() {
            ElementProgrammatiqueEntity p01 = programme("P01", true);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));
            responsableActif();

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initModifier(
                    new ModifierElementInput(p01.getId(), p01.getLibelle(), null, RESPONSABLE)));

            assertCode("AUCUNE_MODIFICATION", e);
        }

        @Test
        @DisplayName("Un élément désactivé ne se modifie pas")
        void refuseUnElementInactif() {
            ElementProgrammatiqueEntity p01 = programme("P01", false);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initModifier(
                    new ModifierElementInput(p01.getId(), "x", null, RESPONSABLE)));

            assertCode("ELEMENT_INACTIF", e);
        }
    }

    // ─── Désactivation et réactivation ──────────────────────────────────────

    @Nested
    @DisplayName("Désactivation et réactivation")
    class ChangementEtat {

        @Test
        @DisplayName("Désactivation soumise avec son motif")
        void soumetUneDesactivation() {
            ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, UUID.randomUUID(), true);
            when(elementRepository.findById(a01.getId())).thenReturn(Optional.of(a01));
            when(elementRepository.existsByParentIdAndActifTrue(a01.getId())).thenReturn(false);
            when(usageReferentiels.elementUtilise(a01.getId())).thenReturn(false);
            aucuneDemandeEnAttente(a01.getId());
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(UUID.randomUUID());

            adapter.initDesactiver(new ChangerEtatElementInput(a01.getId(), "Action fusionnée"));

            verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_NOMENCLATURE, a01.getId().toString(),
                    Map.of("operation", "DESACTIVER", "motif", "Action fusionnée"), MakerCheckerOperationType.DELETE);
        }

        @Test
        @DisplayName("Désactivation d'un élément utilisé : refusée jusqu'à la clôture de l'exercice")
        void refuseUnElementUtilise() {
            ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, UUID.randomUUID(), true);
            when(elementRepository.findById(a01.getId())).thenReturn(Optional.of(a01));
            when(elementRepository.existsByParentIdAndActifTrue(a01.getId())).thenReturn(false);
            when(usageReferentiels.elementUtilise(a01.getId())).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initDesactiver(new ChangerEtatElementInput(a01.getId(), "Fusion")));

            assertCode("ELEMENT_UTILISE", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Un programme qui a encore des actions actives ne se désactive pas")
        void refuseUnParentAvecEnfantsActifs() {
            ElementProgrammatiqueEntity p01 = programme("P01", true);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));
            when(elementRepository.existsByParentIdAndActifTrue(p01.getId())).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initDesactiver(new ChangerEtatElementInput(p01.getId(), "Fin du programme")));

            assertCode("ELEMENT_A_DES_ENFANTS_ACTIFS", e);
        }

        @Test
        @DisplayName("Une désactivation sans motif est refusée")
        void refuseUneDesactivationSansMotif() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class,
                    () -> adapter.initDesactiver(new ChangerEtatElementInput(UUID.randomUUID(), "  ")));

            assertCode("MOTIF_OBLIGATOIRE", e);
            verifyNoInteractions(elementRepository);
        }

        @Test
        @DisplayName("Une action ne se réactive pas sous un programme désactivé")
        void refuseUneReactivationSousParentInactif() {
            ElementProgrammatiqueEntity p01 = programme("P01", false);
            ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, p01.getId(), false);
            when(elementRepository.findById(a01.getId())).thenReturn(Optional.of(a01));
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initReactiver(new ChangerEtatElementInput(a01.getId(), null)));

            assertCode("PARENT_INACTIF", e);
        }
    }

    // ─── Décision ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validation ou rejet")
    class Decision {

        @Test
        @DisplayName("Validation d'une création : l'élément naît actif, avec auteur et validateur")
        void valideUneCreation() {
            ElementProgrammatiqueEntity p01 = programme("P01", true);
            UUID elementId = NomenclatureAdapter.identifiantPour("P01-A01");
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, elementId,
                    DemandeNomenclatureEvent.creation(NiveauProgrammatique.ACTION, "P01-A01", "Santé du cheptel",
                            null, p01.getId(), null));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            when(elementRepository.existsByCode("P01-A01")).thenReturn(false);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

            adapter.decider(new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

            ArgumentCaptor<ElementProgrammatiqueEntity> cree = ArgumentCaptor.forClass(ElementProgrammatiqueEntity.class);
            verify(elementRepository).saveAndFlush(cree.capture());
            ElementProgrammatiqueEntity a01 = cree.getValue();
            assertEquals(elementId, a01.getId());
            assertEquals("P01-A01", a01.getCode());
            assertEquals(NiveauProgrammatique.ACTION, a01.getNiveau());
            assertEquals(p01.getId(), a01.getParentId());
            assertTrue(a01.isActif());
            assertEquals(CHEF_BUDGET, a01.getCreePar());
            assertEquals(VALIDATEUR, a01.getValidePar());
            assertEquals(demande.getId(), a01.getDemandeCreationId());
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.ACCEPTED, null, VALIDATEUR);
        }

        @Test
        @DisplayName("Parent désactivé entre la demande et la validation : validation refusée, demande en attente")
        void refuseSiLeParentAEteDesactive() {
            ElementProgrammatiqueEntity p01 = programme("P01", false);
            UUID elementId = NomenclatureAdapter.identifiantPour("P01-A01");
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, elementId,
                    DemandeNomenclatureEvent.creation(NiveauProgrammatique.ACTION, "P01-A01", "x", null, p01.getId(), null));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            when(elementRepository.existsByCode("P01-A01")).thenReturn(false);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("PARENT_INACTIF", e);
            verify(elementRepository, never()).saveAndFlush(any());
            verify(workflowService, never()).update(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Validation d'une désactivation : l'élément devient inactif et garde le motif")
        void valideUneDesactivation() {
            ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, UUID.randomUUID(), true);
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, a01.getId(),
                    DemandeNomenclatureEvent.changementEtat(OperationReferentiel.DESACTIVER, "Action fusionnée"));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            when(elementRepository.findById(a01.getId())).thenReturn(Optional.of(a01));
            when(elementRepository.existsByParentIdAndActifTrue(a01.getId())).thenReturn(false);
            when(usageReferentiels.elementUtilise(a01.getId())).thenReturn(false);

            adapter.decider(new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, "OK"));

            assertFalse(a01.isActif());
            assertEquals("Action fusionnée", a01.getMotifDesactivation());
            assertEquals(CHEF_BUDGET, a01.getModifiePar());
            assertEquals(HORLOGE.instant(), a01.getModifieLe());
            verify(elementRepository).saveAndFlush(a01);
        }

        @Test
        @DisplayName("Validation d'une réactivation : le motif de désactivation est effacé")
        void valideUneReactivation() {
            ElementProgrammatiqueEntity p01 = programme("P01", false);
            p01.setMotifDesactivation("Programme suspendu");
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, p01.getId(),
                    DemandeNomenclatureEvent.changementEtat(OperationReferentiel.REACTIVER, null));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

            adapter.decider(new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

            assertTrue(p01.isActif());
            assertNull(p01.getMotifDesactivation());
        }

        @Test
        @DisplayName("L'auteur ne valide pas sa propre demande")
        void refuseLAuteur() {
            UUID elementId = NomenclatureAdapter.identifiantPour("P01");
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, elementId,
                    DemandeNomenclatureEvent.creation(NiveauProgrammatique.PROGRAMME, "P01", "x", null, null, RESPONSABLE));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(CHEF_BUDGET);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("VALIDATEUR_EST_AUTEUR", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Un rejet sans motif est refusé ; un rejet motivé ne touche pas à la nomenclature")
        void rejet() {
            UUID elementId = NomenclatureAdapter.identifiantPour("P01");
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, elementId,
                    DemandeNomenclatureEvent.creation(NiveauProgrammatique.PROGRAMME, "P01", "x", null, null, RESPONSABLE));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.decider(
                    new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.REJECTED, " ")));
            assertCode("MOTIF_OBLIGATOIRE", e);

            connecte(VALIDATEUR);
            adapter.decider(new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.REJECTED, "Code erroné"));

            verifyNoInteractions(elementRepository);
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.REJECTED, "Code erroné", VALIDATEUR);
        }

        @Test
        @DisplayName("Une demande déjà tranchée ne se tranche pas deux fois")
        void refuseUneDemandeTranchee() {
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.ACCEPTED,
                    NomenclatureAdapter.identifiantPour("P01"),
                    DemandeNomenclatureEvent.creation(NiveauProgrammatique.PROGRAMME, "P01", "x", null, null, RESPONSABLE));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeNomenclatureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("DEMANDE_DEJA_TRANCHEE", e);
        }
    }

    // ─── Arbre ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Fiche d'une action sans responsable : pas de nom, pas d'erreur")
    void ficheSansResponsable() {
        ElementProgrammatiqueEntity p01 = programme("P01", true);
        ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, p01.getId(), true);
        when(elementRepository.findById(a01.getId())).thenReturn(Optional.of(a01));
        when(elementRepository.findById(p01.getId())).thenReturn(Optional.of(p01));

        var fiche = adapter.consulter(a01.getId());

        assertEquals("P01", fiche.parentCode());
        assertNull(fiche.responsableNom());
        verifyNoInteractions(utilisateurRepository);
    }

    @Test
    @DisplayName("Arbre : les actions apparaissent sous leur programme ; les éléments désactivés sont masqués par défaut")
    void arbre() {
        ElementProgrammatiqueEntity p01 = programme("P01", true);
        ElementProgrammatiqueEntity a01 = element("P01-A01", NiveauProgrammatique.ACTION, p01.getId(), true);
        ElementProgrammatiqueEntity a02 = element("P01-A02", NiveauProgrammatique.ACTION, p01.getId(), false);
        ElementProgrammatiqueEntity p02 = programme("P02", true);
        when(elementRepository.findAll(Sort.by("code"))).thenReturn(List.of(p01, a01, a02, p02));
        when(utilisateurRepository.findAllById(List.of(RESPONSABLE))).thenReturn(List.of(
                UtilisateurEntity.builder().id(RESPONSABLE).nom("Ndzana").prenom("Paul").actif(true).build()));

        List<NoeudNomenclatureOutput> arbre = adapter.arbre(false);

        assertEquals(List.of("P01", "P02"), arbre.stream().map(NoeudNomenclatureOutput::code).toList());
        assertEquals(List.of("P01-A01"), arbre.get(0).enfants().stream().map(NoeudNomenclatureOutput::code).toList());
        assertEquals("Paul Ndzana", arbre.get(0).responsableNom());
        verify(utilsService).hasPermission(Permissions.GET_BUDGET_NOMENCLATURE_INFO);

        assertEquals(2, adapter.arbre(true).get(0).enfants().size());
    }
}
