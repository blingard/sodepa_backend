package com.sodepa.erp.budgetaire.structure;

import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.structure.application.inputs.ChangerEtatStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.CreerStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.DeciderDemandeStructureInput;
import com.sodepa.erp.budgetaire.structure.application.inputs.ModifierStructureInput;
import com.sodepa.erp.budgetaire.structure.application.outputs.StructureBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.budgetaire.structure.infrastructure.entities.StructureBudgetaireEntity;
import com.sodepa.erp.budgetaire.structure.infrastructure.event.DemandeStructureEvent;
import com.sodepa.erp.budgetaire.structure.infrastructure.repo.StructureBudgetaireRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutPoste;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutUnite;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Règles de US-BUD-107 « Déclarer les structures budgétaires ».
 *
 * <p>
 * L'organigramme est simulé par un petit arbre fidèle à celui de la SODEPA :
 * CA → DG → DAF → SDFBM → Service du Budget, et DG → ranch de Ndokayo → son
 * service administratif et financier (absent de l'organigramme réel, voir la PR).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class StructureBudgetaireAdapterTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-23T09:00:00Z"), ZoneId.of("Africa/Douala"));
    private static final String CHEF_BUDGET = "11111111-1111-1111-1111-111111111111";
    private static final String VALIDATEUR = "22222222-2222-2222-2222-222222222222";

    @Mock private StructureBudgetaireRepository structureRepository;
    @Mock private UniteOrganisationnelleRepository uniteRepository;
    @Mock private PosteRepository posteRepository;
    @Mock private AffectationPosteRepository affectationRepository;
    @Mock private MakerCheckerRequestJpaRepo demandeRepository;
    @Mock private WorkflowService workflowService;
    @Mock private UtilsService utilsService;
    @Mock private UsageReferentiels usageReferentiels;

    private StructureBudgetaireAdapter adapter;

    private final Map<UUID, UniteOrganisationnelleEntity> unites = new HashMap<>();
    private final Map<UUID, PosteEntity> postes = new HashMap<>();
    private UniteOrganisationnelleEntity dg;
    private UniteOrganisationnelleEntity sdfbm;
    private UniteOrganisationnelleEntity serviceBudget;
    private UniteOrganisationnelleEntity ranch;
    private UniteOrganisationnelleEntity safcRanch;
    private PosteEntity posteDg;
    private PosteEntity posteSdfbm;
    private PosteEntity posteDirRanch;

    @BeforeEach
    void preparer() {
        adapter = new StructureBudgetaireAdapter(structureRepository, uniteRepository, posteRepository,
                affectationRepository, new CircuitReferentielBudgetaire(demandeRepository, workflowService, utilsService, HORLOGE),
                utilsService, usageReferentiels, HORLOGE);

        unite("CA", "Conseil d'Administration", "/CA/");
        dg = unite("DG", "Direction Générale", "/CA/DG/");
        unite("DAF", "Direction Administrative et Financière", "/CA/DG/DAF/");
        sdfbm = unite("SDFBM", "Sous-Direction Finances, Budget & Matériel", "/CA/DG/DAF/SDFBM/");
        serviceBudget = unite("SERV_BUDGET", "Service du Budget", "/CA/DG/DAF/SDFBM/SERV_BUDGET/");
        ranch = unite("RANCH_NDOKAYO", "Ranch Pastoral de Ndokayo", "/CA/DG/RANCH_NDOKAYO/");
        safcRanch = unite("SAFC_NDOKAYO", "Service administratif et financier de Ndokayo", "/CA/DG/RANCH_NDOKAYO/SAFC_NDOKAYO/");
        posteDg = poste("POSTE_DG", "Directeur Général", dg);
        posteSdfbm = poste("POSTE_SD_FBM", "Sous-Directeur FBM", sdfbm);
        posteDirRanch = poste("POSTE_DIR_RANCH_NDOKAYO", "Directeur Ranch Pastoral de Ndokayo", ranch);

        lenient().when(uniteRepository.findById(any())).thenAnswer(i -> Optional.ofNullable(unites.get(i.<UUID>getArgument(0))));
        lenient().when(uniteRepository.findAllById(any())).thenAnswer(i -> filtrer(unites, i.getArgument(0)));
        lenient().when(posteRepository.findById(any())).thenAnswer(i -> Optional.ofNullable(postes.get(i.<UUID>getArgument(0))));
        lenient().when(posteRepository.findAllById(any())).thenAnswer(i -> filtrer(postes, i.getArgument(0)));
        lenient().when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                anyString(), any(), any())).thenReturn(List.of());
    }

    // ─── Outils ─────────────────────────────────────────────────────────────

    private UniteOrganisationnelleEntity unite(String code, String intitule, String chemin) {
        UniteOrganisationnelleEntity u = UniteOrganisationnelleEntity.builder()
                .id(UUID.nameUUIDFromBytes(code.getBytes())).code(code).intitule(intitule)
                .materializedPath(chemin).statut(StatutUnite.ACTIF).build();
        unites.put(u.getId(), u);
        return u;
    }

    private PosteEntity poste(String code, String intitule, UniteOrganisationnelleEntity unite) {
        PosteEntity p = PosteEntity.builder().id(UUID.nameUUIDFromBytes(code.getBytes())).codePoste(code)
                .intitule(intitule).uniteOrganisationnelle(unite).statut(StatutPoste.OUVERT).build();
        postes.put(p.getId(), p);
        return p;
    }

    private static <T> List<T> filtrer(Map<UUID, T> source, Iterable<UUID> ids) {
        List<T> resultat = new ArrayList<>();
        ids.forEach(id -> { if (source.containsKey(id)) resultat.add(source.get(id)); });
        return resultat;
    }

    private StructureBudgetaireEntity structure(UniteOrganisationnelleEntity unite, PosteEntity ordonnateur,
                                                UniteOrganisationnelleEntity service, boolean actif) {
        return StructureBudgetaireEntity.builder()
                .id(StructureBudgetaireAdapter.identifiantPour(unite.getId()))
                .uniteId(unite.getId()).ordonnateurPosteId(ordonnateur.getId())
                .serviceEngagementUniteId(service.getId()).actif(actif)
                .creePar(CHEF_BUDGET).creeLe(HORLOGE.instant()).validePar(VALIDATEUR).valideLe(HORLOGE.instant())
                .version(0L).build();
    }

    private void connecte(String userId) {
        when(utilsService.getCurrentUserData()).thenReturn(new UserData(
                "u", "Utilisateur", userId, Set.of(), "u@sodepa.cm", Set.of(), "s", "jwt", "iam",
                Set.of(), false, false, false));
    }

    private static MakerCheckerRequestEntity demande(String auteur, MakerCheckerStatus statut, UUID structureId,
                                                     DemandeStructureEvent charge) {
        return MakerCheckerRequestEntity.builder()
                .id(UUID.randomUUID()).entityName(MakerCheckerEntityName.BUDGET_STRUCTURE)
                .entityPk(structureId.toString()).makerId(auteur).status(statut)
                .payload(charge.versCharge()).checkerOperationType(charge.operation().typeMakerChecker())
                .createdAt(HORLOGE.instant().minus(1, ChronoUnit.HOURS))
                .expiredAt(HORLOGE.instant().plus(10, ChronoUnit.DAYS))
                .build();
    }

    private static void assertCode(String code, ApiException e) {
        assertEquals(code, e.getCode());
    }

    // ─── Déclaration ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Demande de déclaration")
    class Declaration {

        @Test
        @DisplayName("Direction Générale : ordonnateur DG, service d'engagement le Service du Budget")
        void declareLaDirectionGenerale() {
            UUID demandeId = UUID.randomUUID();
            UUID structureId = StructureBudgetaireAdapter.identifiantPour(dg.getId());
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(demandeId);

            SubmissionOutput accuse = adapter.initCreer(new CreerStructureInput(dg.getId(), posteDg.getId(), serviceBudget.getId()));

            assertEquals(new SubmissionOutput(demandeId, structureId), accuse);
            verify(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_STRUCTURE);
            verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_STRUCTURE, structureId.toString(),
                    Map.of("operation", "CREER", "uniteId", dg.getId().toString(),
                            "ordonnateurPosteId", posteDg.getId().toString(),
                            "serviceEngagementUniteId", serviceBudget.getId().toString()),
                    MakerCheckerOperationType.CREATE);
        }

        @Test
        @DisplayName("Unité opérationnelle : ordonnateur le directeur du ranch, service son service administratif et financier")
        void declareUnRanch() {
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(UUID.randomUUID());

            adapter.initCreer(new CreerStructureInput(ranch.getId(), posteDirRanch.getId(), safcRanch.getId()));

            verify(workflowService).submitChange(any(), anyString(), any(), any());
        }

        @Test
        @DisplayName("Unité inexistante : la structure se choisit dans l'organigramme")
        void refuseUneUniteInexistante() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerStructureInput(UUID.randomUUID(), posteDg.getId(), serviceBudget.getId())));

            assertCode("UNITE_INVALIDE", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une unité inactive dans l'organigramme ne se déclare pas")
        void refuseUneUniteInactive() {
            ranch.setStatut(StatutUnite.INACTIF);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerStructureInput(ranch.getId(), posteDirRanch.getId(), safcRanch.getId())));

            assertCode("UNITE_INACTIVE", e);
        }

        @Test
        @DisplayName("Une unité déjà déclarée est refusée")
        void refuseUneUniteDejaDeclaree() {
            when(structureRepository.existsByUniteId(dg.getId())).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerStructureInput(dg.getId(), posteDg.getId(), serviceBudget.getId())));

            assertCode("STRUCTURE_EXISTANTE", e);
        }

        @Test
        @DisplayName("Une seconde demande sur la même unité est refusée tant que la première attend")
        void refuseUneSecondeDemande() {
            UUID structureId = StructureBudgetaireAdapter.identifiantPour(dg.getId());
            when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                    structureId.toString(), MakerCheckerEntityName.BUDGET_STRUCTURE, MakerCheckerStatus.PENDING))
                    .thenReturn(List.of(demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, structureId,
                            DemandeStructureEvent.creation(dg.getId(), posteDg.getId(), serviceBudget.getId()))));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerStructureInput(dg.getId(), posteDg.getId(), serviceBudget.getId())));

            assertCode("STRUCTURE_DEMANDE_EN_ATTENTE", e);
        }

        @Test
        @DisplayName("L'ordonnateur est un poste de l'unité elle-même")
        void refuseUnOrdonnateurDUneAutreUnite() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerStructureInput(ranch.getId(), posteDg.getId(), safcRanch.getId())));

            assertCode("ORDONNATEUR_INVALIDE", e);
        }

        @Test
        @DisplayName("Un poste supprimé ne peut pas être ordonnateur")
        void refuseUnPosteSupprime() {
            posteDirRanch.setStatut(StatutPoste.SUPPRIME);

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerStructureInput(ranch.getId(), posteDirRanch.getId(), safcRanch.getId())));

            assertCode("ORDONNATEUR_INVALIDE", e);
        }

        @Test
        @DisplayName("Le service d'engagement dépend de la structure : le Service du Budget n'est pas celui du ranch")
        void refuseUnServiceHorsDuSousArbre() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerStructureInput(ranch.getId(), posteDirRanch.getId(), serviceBudget.getId())));

            assertCode("SERVICE_ENGAGEMENT_INVALIDE", e);
        }

        @Test
        @DisplayName("Pas de repli : l'unité n'est pas son propre service d'engagement")
        void refuseLUniteCommeService() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerStructureInput(ranch.getId(), posteDirRanch.getId(), ranch.getId())));

            assertCode("SERVICE_ENGAGEMENT_INVALIDE", e);
        }

        @Test
        @DisplayName("Le service d'une sous-structure ne sert pas à la structure qui la contient")
        void refuseLeServiceDUneSousStructure() {
            when(structureRepository.findAllByActifTrue()).thenReturn(List.of(structure(ranch, posteDirRanch, safcRanch, true)));

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerStructureInput(dg.getId(), posteDg.getId(), safcRanch.getId())));

            assertCode("SERVICE_ENGAGEMENT_INVALIDE", e);
        }

        @Test
        @DisplayName("Déclarer la SDFBM ferait passer le Service du Budget de la DG dans son budget : refusé")
        void refuseDEnglober() {
            when(structureRepository.findAllByActifTrue()).thenReturn(List.of(structure(dg, posteDg, serviceBudget, true)));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerStructureInput(sdfbm.getId(), posteSdfbm.getId(), serviceBudget.getId())));

            assertCode("STRUCTURE_ENGLOBE_UN_SERVICE", e);
        }

        @Test
        @DisplayName("Sans habilitation : refus avant toute lecture")
        void refuseSansHabilitation() {
            doThrow(new AccesRefuseException("Habilitation requise"))
                    .when(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_STRUCTURE);

            assertThrows(AccesRefuseException.class, () -> adapter.initCreer(
                    new CreerStructureInput(dg.getId(), posteDg.getId(), serviceBudget.getId())));

            verifyNoInteractions(structureRepository, workflowService);
        }
    }

    // ─── Modification et changement d'état ──────────────────────────────────

    @Nested
    @DisplayName("Modification et changement d'état")
    class Gestion {

        @Test
        @DisplayName("Une modification qui ne change rien est refusée")
        void refuseUneModificationVide() {
            StructureBudgetaireEntity s = structure(dg, posteDg, serviceBudget, true);
            when(structureRepository.findById(s.getId())).thenReturn(Optional.of(s));

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initModifier(
                    new ModifierStructureInput(s.getId(), posteDg.getId(), serviceBudget.getId())));

            assertCode("AUCUNE_MODIFICATION", e);
        }

        @Test
        @DisplayName("Une structure désactivée ne se modifie pas")
        void refuseDeModifierUneStructureInactive() {
            StructureBudgetaireEntity s = structure(ranch, posteDirRanch, safcRanch, false);
            when(structureRepository.findById(s.getId())).thenReturn(Optional.of(s));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initModifier(
                    new ModifierStructureInput(s.getId(), posteDirRanch.getId(), safcRanch.getId())));

            assertCode("STRUCTURE_INACTIVE", e);
        }

        @Test
        @DisplayName("Une structure qui porte des lignes d'un exercice en cours ne se désactive pas")
        void refuseUneStructureUtilisee() {
            StructureBudgetaireEntity s = structure(ranch, posteDirRanch, safcRanch, true);
            when(structureRepository.findById(s.getId())).thenReturn(Optional.of(s));
            when(usageReferentiels.structureUtilisee(s.getId())).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initDesactiver(new ChangerEtatStructureInput(s.getId(), "Cession du ranch")));

            assertCode("STRUCTURE_UTILISEE", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une désactivation sans motif est refusée")
        void refuseUneDesactivationSansMotif() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class,
                    () -> adapter.initDesactiver(new ChangerEtatStructureInput(UUID.randomUUID(), null)));

            assertCode("MOTIF_OBLIGATOIRE", e);
            verifyNoInteractions(structureRepository);
        }

        @Test
        @DisplayName("Une structure dont l'unité a été fermée ne se réactive pas")
        void refuseLaReactivationDUneUniteFermee() {
            StructureBudgetaireEntity s = structure(ranch, posteDirRanch, safcRanch, false);
            when(structureRepository.findById(s.getId())).thenReturn(Optional.of(s));
            ranch.setStatut(StatutUnite.INACTIF);

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initReactiver(new ChangerEtatStructureInput(s.getId(), null)));

            assertCode("UNITE_INACTIVE", e);
        }
    }

    // ─── Décision ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validation ou rejet")
    class Decision {

        @Test
        @DisplayName("Validation d'une déclaration : la structure naît active, avec auteur et validateur")
        void valideUneDeclaration() {
            UUID structureId = StructureBudgetaireAdapter.identifiantPour(ranch.getId());
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, structureId,
                    DemandeStructureEvent.creation(ranch.getId(), posteDirRanch.getId(), safcRanch.getId()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);

            adapter.decider(new DeciderDemandeStructureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

            ArgumentCaptor<StructureBudgetaireEntity> creee = ArgumentCaptor.forClass(StructureBudgetaireEntity.class);
            verify(structureRepository).saveAndFlush(creee.capture());
            StructureBudgetaireEntity s = creee.getValue();
            assertEquals(structureId, s.getId());
            assertEquals(ranch.getId(), s.getUniteId());
            assertEquals(posteDirRanch.getId(), s.getOrdonnateurPosteId());
            assertEquals(safcRanch.getId(), s.getServiceEngagementUniteId());
            assertTrue(s.isActif());
            assertEquals(CHEF_BUDGET, s.getCreePar());
            assertEquals(VALIDATEUR, s.getValidePar());
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.ACCEPTED, null, VALIDATEUR);
        }

        @Test
        @DisplayName("Poste supprimé entre la demande et la validation : validation refusée, demande en attente")
        void refuseSiLePosteAEteSupprime() {
            UUID structureId = StructureBudgetaireAdapter.identifiantPour(ranch.getId());
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, structureId,
                    DemandeStructureEvent.creation(ranch.getId(), posteDirRanch.getId(), safcRanch.getId()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            posteDirRanch.setStatut(StatutPoste.SUPPRIME);

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.decider(
                    new DeciderDemandeStructureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("ORDONNATEUR_INVALIDE", e);
            verify(structureRepository, never()).saveAndFlush(any());
            verify(workflowService, never()).update(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Validation d'une désactivation : la structure devient inactive et garde le motif")
        void valideUneDesactivation() {
            StructureBudgetaireEntity s = structure(ranch, posteDirRanch, safcRanch, true);
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, s.getId(),
                    DemandeStructureEvent.changementEtat(OperationReferentiel.DESACTIVER, "Cession du ranch"));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            when(structureRepository.findById(s.getId())).thenReturn(Optional.of(s));
            connecte(VALIDATEUR);

            adapter.decider(new DeciderDemandeStructureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

            assertEquals(false, s.isActif());
            assertEquals("Cession du ranch", s.getMotifDesactivation());
            assertEquals(CHEF_BUDGET, s.getModifiePar());
            verify(structureRepository).saveAndFlush(s);
        }

        @Test
        @DisplayName("L'auteur ne valide pas sa propre demande")
        void refuseLAuteur() {
            UUID structureId = StructureBudgetaireAdapter.identifiantPour(dg.getId());
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, structureId,
                    DemandeStructureEvent.creation(dg.getId(), posteDg.getId(), serviceBudget.getId()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(CHEF_BUDGET);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeStructureInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("VALIDATEUR_EST_AUTEUR", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Un rejet motivé ne crée rien")
        void rejet() {
            UUID structureId = StructureBudgetaireAdapter.identifiantPour(dg.getId());
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, structureId,
                    DemandeStructureEvent.creation(dg.getId(), posteDg.getId(), serviceBudget.getId()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);

            adapter.decider(new DeciderDemandeStructureInput(demande.getId(), MakerCheckerStatus.REJECTED, "Mauvais service"));

            verifyNoInteractions(structureRepository);
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.REJECTED, "Mauvais service", VALIDATEUR);
        }
    }

    // ─── Lecture ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Fiche : le titulaire de l'ordonnateur est l'affectation active la plus récente ; poste vacant sinon")
    void ficheAvecTitulaire() {
        StructureBudgetaireEntity s = structure(ranch, posteDirRanch, safcRanch, true);
        when(structureRepository.findById(s.getId())).thenReturn(Optional.of(s));
        UtilisateurEntity ancien = UtilisateurEntity.builder().id(UUID.randomUUID()).prenom("Jean").nom("Ancien").actif(true).build();
        UtilisateurEntity actuel = UtilisateurEntity.builder().id(UUID.randomUUID()).prenom("Marie").nom("Actuelle").actif(true).build();
        when(affectationRepository.findByPosteIdAndStatut(posteDirRanch.getId(), StatutAffectation.ACTIF)).thenReturn(List.of(
                AffectationPosteEntity.builder().utilisateur(ancien).poste(posteDirRanch).dateDebut(LocalDate.of(2020, 1, 1)).build(),
                AffectationPosteEntity.builder().utilisateur(actuel).poste(posteDirRanch).dateDebut(LocalDate.of(2025, 6, 1)).build()));

        StructureBudgetaireOutput fiche = adapter.consulter(s.getId());

        assertEquals("RANCH_NDOKAYO", fiche.uniteCode());
        assertEquals("Directeur Ranch Pastoral de Ndokayo", fiche.ordonnateurPosteIntitule());
        assertEquals("Marie Actuelle", fiche.ordonnateurTitulaireNom());
        assertEquals("SAFC_NDOKAYO", fiche.serviceEngagementCode());
        verify(utilsService).hasPermission(Permissions.GET_BUDGET_STRUCTURE_INFO);

        when(affectationRepository.findByPosteIdAndStatut(posteDirRanch.getId(), StatutAffectation.ACTIF)).thenReturn(List.of());
        assertNull(adapter.consulter(s.getId()).ordonnateurTitulaireId());
    }
}
