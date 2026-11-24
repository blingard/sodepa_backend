package com.sodepa.erp.budgetaire.exercice;

import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.exercice.application.inputs.CreerExerciceInput;
import com.sodepa.erp.budgetaire.exercice.application.inputs.DeciderDemandeExerciceInput;
import com.sodepa.erp.budgetaire.exercice.application.outputs.ExerciceBudgetaireOutput;
import com.sodepa.erp.budgetaire.exercice.infrastructure.adapter.ExerciceBudgetaireAdapter;
import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.EtatExercice;
import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.ExerciceBudgetaireEntity;
import com.sodepa.erp.budgetaire.exercice.infrastructure.event.CreationExerciceEvent;
import com.sodepa.erp.budgetaire.exercice.infrastructure.repo.ExerciceBudgetaireRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.share.erreurs.ApiException;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
import com.sodepa.erp.share.erreurs.RessourceIntrouvableException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Règles de US-BUD-101 « Créer un exercice budgétaire ».
 *
 * <p>
 * Tests sur simulacres : ils fixent les règles de l'adaptateur (droits,
 * doublons, quatre yeux, dates fixées par le serveur). La contrainte
 * d'unicité en base et le circuit réel se vérifient sur l'API démarrée.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ExerciceBudgetaireAdapterTest {

    /** « Aujourd'hui » : 23 novembre 2026, à Douala. */
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-23T09:00:00Z"), ZoneId.of("Africa/Douala"));

    private static final String CHEF_BUDGET = "11111111-1111-1111-1111-111111111111";
    private static final String VALIDATEUR = "22222222-2222-2222-2222-222222222222";

    @Mock private ExerciceBudgetaireRepository exerciceRepository;
    @Mock private MakerCheckerRequestJpaRepo demandeRepository;
    @Mock private WorkflowService workflowService;
    @Mock private UtilsService utilsService;
    @Mock private UserRepository utilisateurRepository;

    private ExerciceBudgetaireAdapter adapter;

    @BeforeEach
    void preparer() {
        adapter = new ExerciceBudgetaireAdapter(exerciceRepository,
                new CircuitReferentielBudgetaire(demandeRepository, workflowService, utilsService, utilisateurRepository, HORLOGE), utilsService, HORLOGE);
    }

    private void connecte(String userId) {
        when(utilsService.getCurrentUserData()).thenReturn(new UserData(
                "u", "Utilisateur", userId, Set.of(), "u@sodepa.cm", Set.of(), "s", "jwt", "iam",
                Set.of(), false, false, false));
    }

    private static void assertCode(String code, ApiException e) {
        assertEquals(code, e.getCode());
    }

    @Nested
    @DisplayName("Demande de création")
    class Soumission {

        @Test
        @DisplayName("Création d'un exercice : la demande part au maker-checker avec la seule année")
        void soumetLaCreation() {
            UUID demandeId = UUID.randomUUID();
            UUID exerciceId = ExerciceBudgetaireAdapter.identifiantPour(2027);
            when(exerciceRepository.existsByAnnee(2027)).thenReturn(false);
            when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                    exerciceId.toString(), MakerCheckerEntityName.BUDGET_EXERCICE, MakerCheckerStatus.PENDING))
                    .thenReturn(List.of());
            when(workflowService.submitChange(MakerCheckerEntityName.BUDGET_EXERCICE, exerciceId.toString(),
                    Map.of("annee", 2027), MakerCheckerOperationType.CREATE)).thenReturn(demandeId);

            SubmissionOutput accuse = adapter.initCreer(new CreerExerciceInput(2027));

            assertEquals(new SubmissionOutput(demandeId, exerciceId), accuse);
            verify(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_EXERCICE);
            verify(exerciceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Exercice en double : « L'exercice 2027 existe déjà »")
        void refuseUnExerciceExistant() {
            when(exerciceRepository.existsByAnnee(2027)).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initCreer(new CreerExerciceInput(2027)));

            assertCode("EXERCICE_EXISTANT", e);
            assertEquals("L'exercice 2027 existe déjà", e.getMessage());
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une demande en attente pour la même année bloque la seconde")
        void refuseUneSecondeDemandeEnAttente() {
            when(exerciceRepository.existsByAnnee(2027)).thenReturn(false);
            when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                    anyString(), eq(MakerCheckerEntityName.BUDGET_EXERCICE), eq(MakerCheckerStatus.PENDING)))
                    .thenReturn(List.of(demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().plus(10, ChronoUnit.DAYS))));

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initCreer(new CreerExerciceInput(2027)));

            assertCode("EXERCICE_DEMANDE_EN_ATTENTE", e);
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une demande expirée mais restée en attente ne bloque pas")
        void ignoreUneDemandeExpiree() {
            when(exerciceRepository.existsByAnnee(2027)).thenReturn(false);
            when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                    anyString(), eq(MakerCheckerEntityName.BUDGET_EXERCICE), eq(MakerCheckerStatus.PENDING)))
                    .thenReturn(List.of(demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().minus(1, ChronoUnit.DAYS))));
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(UUID.randomUUID());

            adapter.initCreer(new CreerExerciceInput(2027));

            verify(workflowService).submitChange(any(), anyString(), any(), eq(MakerCheckerOperationType.CREATE));
        }

        @Test
        @DisplayName("Année absente, ancienne ou au-delà de N+1 : 400")
        void refuseUneAnneeHorsBornes() {
            assertCode("ANNEE_OBLIGATOIRE", assertThrows(RequeteInvalideException.class,
                    () -> adapter.initCreer(new CreerExerciceInput(null))));
            assertCode("ANNEE_HORS_BORNES", assertThrows(RequeteInvalideException.class,
                    () -> adapter.initCreer(new CreerExerciceInput(2028))));
            assertCode("ANNEE_HORS_BORNES", assertThrows(RequeteInvalideException.class,
                    () -> adapter.initCreer(new CreerExerciceInput(1999))));
            verifyNoInteractions(workflowService, exerciceRepository);
        }

        @Test
        @DisplayName("Sans habilitation : refus avant toute lecture")
        void refuseSansHabilitation() {
            doThrow(new AccesRefuseException("Habilitation requise"))
                    .when(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_EXERCICE);

            assertThrows(AccesRefuseException.class, () -> adapter.initCreer(new CreerExerciceInput(2027)));

            verifyNoInteractions(exerciceRepository, demandeRepository, workflowService);
        }
    }

    @Nested
    @DisplayName("Décision sur la demande")
    class Decision {

        @Test
        @DisplayName("Validée par un second utilisateur : exercice en préparation, du 01/01 au 31/12")
        void creeLExerciceALaValidation() {
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().plus(10, ChronoUnit.DAYS));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            when(exerciceRepository.existsByAnnee(2027)).thenReturn(false);

            adapter.decider(new DeciderDemandeExerciceInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

            ArgumentCaptor<ExerciceBudgetaireEntity> cree = ArgumentCaptor.forClass(ExerciceBudgetaireEntity.class);
            verify(exerciceRepository).saveAndFlush(cree.capture());
            ExerciceBudgetaireEntity exercice = cree.getValue();
            assertEquals(2027, exercice.getAnnee());
            assertEquals(EtatExercice.EN_PREPARATION, exercice.getEtat());
            assertEquals(LocalDate.of(2027, 1, 1), exercice.getDateDebut());
            assertEquals(LocalDate.of(2027, 12, 31), exercice.getDateFin());
            assertEquals(UUID.fromString(demande.getEntityPk()), exercice.getId());
            assertEquals(CHEF_BUDGET, exercice.getCreePar());
            assertEquals(VALIDATEUR, exercice.getValidePar());
            assertEquals(demande.getId(), exercice.getDemandeCreationId());
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.ACCEPTED, null, VALIDATEUR);
        }

        @Test
        @DisplayName("L'auteur ne valide pas sa propre demande")
        void refuseLAuteurCommeValidateur() {
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().plus(10, ChronoUnit.DAYS));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(CHEF_BUDGET);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeExerciceInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("VALIDATEUR_EST_AUTEUR", e);
            verify(exerciceRepository, never()).saveAndFlush(any());
            verify(workflowService, never()).update(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Demande déjà tranchée : 409")
        void refuseUneDemandeDejaTranchee() {
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.ACCEPTED, HORLOGE.instant().plus(10, ChronoUnit.DAYS));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));

            assertCode("DEMANDE_DEJA_TRANCHEE", assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeExerciceInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null))));
        }

        @Test
        @DisplayName("Demande inconnue ou d'un autre domaine : 404")
        void refuseUneDemandeEtrangere() {
            MakerCheckerRequestEntity autre = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().plus(10, ChronoUnit.DAYS));
            autre.setEntityName(MakerCheckerEntityName.JOURNAL);
            when(demandeRepository.findById(autre.getId())).thenReturn(Optional.of(autre));
            UUID inconnue = UUID.randomUUID();
            when(demandeRepository.findById(inconnue)).thenReturn(Optional.empty());

            assertThrows(RessourceIntrouvableException.class, () -> adapter.decider(
                    new DeciderDemandeExerciceInput(autre.getId(), MakerCheckerStatus.ACCEPTED, null)));
            assertThrows(RessourceIntrouvableException.class, () -> adapter.decider(
                    new DeciderDemandeExerciceInput(inconnue, MakerCheckerStatus.ACCEPTED, null)));
        }

        @Test
        @DisplayName("Rejet : motif obligatoire, aucun exercice créé")
        void rejetteAvecMotif() {
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().plus(10, ChronoUnit.DAYS));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));

            assertCode("MOTIF_OBLIGATOIRE", assertThrows(RequeteInvalideException.class, () -> adapter.decider(
                    new DeciderDemandeExerciceInput(demande.getId(), MakerCheckerStatus.REJECTED, " "))));

            connecte(VALIDATEUR);
            adapter.decider(new DeciderDemandeExerciceInput(demande.getId(), MakerCheckerStatus.REJECTED, "Année erronée"));

            verify(exerciceRepository, never()).saveAndFlush(any());
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.REJECTED, "Année erronée", VALIDATEUR);
        }

        @Test
        @DisplayName("Validation concurrente : l'exercice créé entre-temps donne 409, pas 500")
        void refuseUnDoublonALaValidation() {
            MakerCheckerRequestEntity demande = demande(CHEF_BUDGET, MakerCheckerStatus.PENDING, HORLOGE.instant().plus(10, ChronoUnit.DAYS));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);
            when(exerciceRepository.existsByAnnee(2027)).thenReturn(false);
            when(exerciceRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_budget_exercice_annee"));

            assertCode("EXERCICE_EXISTANT", assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeExerciceInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null))));
            verify(workflowService, never()).update(any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("Liste : du plus récent au plus ancien, 100 lignes au plus")
    void listeParAnneeDecroissante() {
        Page<ExerciceBudgetaireEntity> vide = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(exerciceRepository.findAll(any(Pageable.class))).thenReturn(vide);

        adapter.lister(PageRequest.of(0, 5000));

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(exerciceRepository).findAll(page.capture());
        assertEquals(100, page.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "annee"), page.getValue().getSort());
        verify(utilsService).hasPermission(Permissions.GET_BUDGET_EXERCICE_INFO);
    }

    private static MakerCheckerRequestEntity demande(String auteur, MakerCheckerStatus statut, Instant expiration) {
        return MakerCheckerRequestEntity.builder()
                .id(UUID.randomUUID())
                .entityName(MakerCheckerEntityName.BUDGET_EXERCICE)
                .entityPk(ExerciceBudgetaireAdapter.identifiantPour(2027).toString())
                .makerId(auteur)
                .status(statut)
                .payload(Map.of("annee", 2027))
                .checkerOperationType(MakerCheckerOperationType.CREATE)
                .createdAt(HORLOGE.instant().minus(1, ChronoUnit.HOURS))
                .expiredAt(expiration)
                .build();
    }

    @Nested
    @DisplayName("Demandes en attente")
    class DemandesEnAttente {

        @Test
        @DisplayName("Création : l'exercice qui naîtra, en préparation, du 1er janvier au 31 décembre")
        void montreLExercicePropose() {
            UUID exerciceId = ExerciceBudgetaireAdapter.identifiantPour(2027);
            MakerCheckerRequestEntity demande = MakerCheckerRequestEntity.builder()
                    .id(UUID.randomUUID()).entityName(MakerCheckerEntityName.BUDGET_EXERCICE)
                    .entityPk(exerciceId.toString()).makerId(CHEF_BUDGET).status(MakerCheckerStatus.PENDING)
                    .payload(new CreationExerciceEvent(2027).versCharge())
                    .checkerOperationType(MakerCheckerOperationType.CREATE)
                    .createdAt(HORLOGE.instant()).expiredAt(HORLOGE.instant().plusSeconds(86_400)).build();
            connecte(VALIDATEUR);
            when(demandeRepository.findAllByEntityNameAndStatusAndMakerIdNot(eq(MakerCheckerEntityName.BUDGET_EXERCICE),
                    eq(MakerCheckerStatus.PENDING), eq(VALIDATEUR), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(demande), PageRequest.of(0, 20), 1));

            DemandeBudgetaireOutput<ExerciceBudgetaireOutput> d =
                    adapter.demandesEnAttente(PageRequest.of(0, 20)).content().getFirst();

            assertEquals(OperationReferentiel.CREER, d.operation());
            assertNull(d.actuel());
            assertEquals(exerciceId, d.proposition().id());
            assertEquals(2027, d.proposition().annee());
            assertEquals(EtatExercice.EN_PREPARATION, d.proposition().etat());
            assertEquals(LocalDate.of(2027, 1, 1), d.proposition().dateDebut());
            assertEquals(LocalDate.of(2027, 12, 31), d.proposition().dateFin());
            assertEquals(CHEF_BUDGET, d.proposition().creePar());
            assertNull(d.proposition().validePar());
        }
    }
}
