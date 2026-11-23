package com.sodepa.erp.budgetaire.nature;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.commun.CircuitReferentielBudgetaire;
import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.commun.UsageReferentiels;
import com.sodepa.erp.budgetaire.nature.application.inputs.CreerNatureInput;
import com.sodepa.erp.budgetaire.nature.application.inputs.ModifierNatureInput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.NatureBudgetaireEntity;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.RegleCompteNature;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.event.DemandeNatureEvent;
import com.sodepa.erp.budgetaire.nature.infrastructure.repo.NatureBudgetaireRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.share.erreurs.ApiException;
import com.sodepa.erp.share.erreurs.ConflitMetierException;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire.FONCTIONNEMENT;
import static com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire.INVESTISSEMENT;
import static com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire.PRODUITS;
import static com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire.DEPENSE;
import static com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire.RECETTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Règles de US-BUD-108 pour les natures : compte existant au plan comptable,
 * cohérence section, sens et classe de compte, cycle de vie maker-checker.
 */
@ExtendWith(MockitoExtension.class)
class NatureBudgetaireAdapterTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-23T09:00:00Z"), ZoneId.of("Africa/Douala"));
    private static final String CHEF_BUDGET = "11111111-1111-1111-1111-111111111111";
    private static final String VALIDATEUR = "22222222-2222-2222-2222-222222222222";

    @Mock private NatureBudgetaireRepository natureRepository;
    @Mock private CompteRepository compteRepository;
    @Mock private MakerCheckerRequestJpaRepo demandeRepository;
    @Mock private WorkflowService workflowService;
    @Mock private UtilsService utilsService;
    @Mock private UsageReferentiels usageReferentiels;

    private NatureBudgetaireAdapter adapter;
    private CompteEntity ventes;
    private CompteEntity achats;

    @BeforeEach
    void preparer() {
        adapter = new NatureBudgetaireAdapter(natureRepository, compteRepository,
                new CircuitReferentielBudgetaire(demandeRepository, workflowService, utilsService, HORLOGE),
                utilsService, usageReferentiels, HORLOGE);
        ventes = compte("701", "Ventes de marchandises");
        achats = compte("601", "Achats de marchandises");
        lenient().when(compteRepository.findByCode(anyString())).thenReturn(Optional.empty());
        lenient().when(compteRepository.findByCode("701")).thenReturn(Optional.of(ventes));
        lenient().when(compteRepository.findByCode("601")).thenReturn(Optional.of(achats));
        lenient().when(compteRepository.findById(ventes.getId())).thenReturn(Optional.of(ventes));
        lenient().when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(List.of());
    }

    private static CompteEntity compte(String code, String intitule) {
        return CompteEntity.builder().id(UUID.nameUUIDFromBytes(code.getBytes())).code(code).intitule(intitule)
                .isAuxiliaire(false).build();
    }

    private NatureBudgetaireEntity nature(String code, boolean actif) {
        return NatureBudgetaireEntity.builder().id(NatureBudgetaireAdapter.identifiantPour(code)).code(code)
                .libelle("Ventes de bovins").section(PRODUITS).sens(RECETTE).compteId(ventes.getId()).actif(actif)
                .creePar(CHEF_BUDGET).creeLe(HORLOGE.instant()).validePar(VALIDATEUR).valideLe(HORLOGE.instant())
                .version(0L).build();
    }

    private void connecte(String userId) {
        when(utilsService.getCurrentUserData()).thenReturn(new UserData(
                "u", "Utilisateur", userId, Set.of(), "u@sodepa.cm", Set.of(), "s", "jwt", "iam",
                Set.of(), false, false, false));
    }

    private static MakerCheckerRequestEntity demande(UUID natureId, DemandeNatureEvent charge) {
        return MakerCheckerRequestEntity.builder()
                .id(UUID.randomUUID()).entityName(MakerCheckerEntityName.BUDGET_NATURE).entityPk(natureId.toString())
                .makerId(CHEF_BUDGET).status(MakerCheckerStatus.PENDING).payload(charge.versCharge())
                .checkerOperationType(charge.operation().typeMakerChecker())
                .createdAt(HORLOGE.instant().minus(1, ChronoUnit.HOURS))
                .expiredAt(HORLOGE.instant().plus(10, ChronoUnit.DAYS)).build();
    }

    private static void assertCode(String code, ApiException e) {
        assertEquals(code, e.getCode());
    }

    @Nested
    @DisplayName("Demande de création")
    class Creation {

        @Test
        @DisplayName("Nature de produits : « Ventes de bovins », produits, compte 701, recette")
        void soumetUneNatureDeProduits() {
            UUID demandeId = UUID.randomUUID();
            UUID natureId = NatureBudgetaireAdapter.identifiantPour("VENTES-BOVINS");
            when(workflowService.submitChange(any(), anyString(), any(), any())).thenReturn(demandeId);

            SubmissionOutput accuse = adapter.initCreer(
                    new CreerNatureInput("ventes-bovins", "Ventes de bovins", PRODUITS, RECETTE, " 701 "));

            assertEquals(new SubmissionOutput(demandeId, natureId), accuse);
            verify(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_NATURE);
            verify(workflowService).submitChange(MakerCheckerEntityName.BUDGET_NATURE, natureId.toString(),
                    Map.of("operation", "CREER", "code", "VENTES-BOVINS", "libelle", "Ventes de bovins",
                            "section", "PRODUITS", "sens", "RECETTE", "compteId", ventes.getId().toString()),
                    MakerCheckerOperationType.CREATE);
        }

        @Test
        @DisplayName("Compte inconnu : un compte absent du plan comptable est refusé")
        void refuseUnCompteInconnu() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerNatureInput("VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, "7011")));

            assertCode("COMPTE_INCONNU", e);
            assertEquals("Le compte 7011 ne figure pas au plan comptable.", e.getMessage());
            verifyNoInteractions(workflowService);
        }

        @Test
        @DisplayName("Une nature de produits sur un compte de charges est refusée")
        void refuseUnCompteIncoherent() {
            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerNatureInput("VENTES", "Ventes", PRODUITS, RECETTE, "601")));

            assertCode("NATURE_INCOHERENTE", e);
        }

        @Test
        @DisplayName("Un compte auxiliaire de tiers ne porte pas de nature")
        void refuseUnCompteAuxiliaire() {
            CompteEntity client = compte("4111001", "Client Dupont");
            client.setIsAuxiliaire(true);
            when(compteRepository.findByCode("4111001")).thenReturn(Optional.of(client));

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.initCreer(
                    new CreerNatureInput("X", "X", PRODUITS, RECETTE, "4111001")));

            assertCode("NATURE_INCOHERENTE", e);
        }

        @Test
        @DisplayName("Un code déjà pris est refusé")
        void refuseUnCodeExistant() {
            when(natureRepository.existsByCode("VENTES-BOVINS")).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerNatureInput("VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, "701")));

            assertCode("CODE_NATURE_EXISTANT", e);
        }

        @Test
        @DisplayName("Une seconde demande sur le même code est refusée tant que la première attend")
        void refuseUneSecondeDemande() {
            UUID natureId = NatureBudgetaireAdapter.identifiantPour("VENTES-BOVINS");
            when(demandeRepository.findByEntityPkAndEntityNameAndStatusOrderByCreatedAtDesc(
                    natureId.toString(), MakerCheckerEntityName.BUDGET_NATURE, MakerCheckerStatus.PENDING))
                    .thenReturn(List.of(demande(natureId, DemandeNatureEvent.creation(
                            "VENTES-BOVINS", "x", PRODUITS, RECETTE, ventes.getId()))));

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.initCreer(
                    new CreerNatureInput("VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, "701")));

            assertCode("NATURE_DEMANDE_EN_ATTENTE", e);
        }

        @Test
        @DisplayName("Sans habilitation : refus avant toute lecture")
        void refuseSansHabilitation() {
            doThrow(new AccesRefuseException("Habilitation requise"))
                    .when(utilsService).hasPermission(Permissions.INIT_CREATE_BUDGET_NATURE);

            assertThrows(AccesRefuseException.class, () -> adapter.initCreer(
                    new CreerNatureInput("VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, "701")));

            verifyNoInteractions(natureRepository, workflowService);
        }
    }

    @Test
    @DisplayName("Cohérence section, sens et classe de compte")
    void regleDeCoherence() {
        assertTrue(RegleCompteNature.admis(FONCTIONNEMENT, DEPENSE, "601"));
        assertTrue(RegleCompteNature.admis(FONCTIONNEMENT, DEPENSE, "83"));
        assertTrue(RegleCompteNature.admis(PRODUITS, RECETTE, "701"));
        assertTrue(RegleCompteNature.admis(PRODUITS, RECETTE, "88"));
        assertTrue(RegleCompteNature.admis(INVESTISSEMENT, DEPENSE, "244"));
        assertTrue(RegleCompteNature.admis(INVESTISSEMENT, RECETTE, "16"));
        assertFalse(RegleCompteNature.admis(FONCTIONNEMENT, RECETTE, "701"));
        assertFalse(RegleCompteNature.admis(PRODUITS, DEPENSE, "701"));
        assertFalse(RegleCompteNature.admis(PRODUITS, RECETTE, "601"));
        assertFalse(RegleCompteNature.admis(FONCTIONNEMENT, DEPENSE, "82"));
        assertFalse(RegleCompteNature.admis(INVESTISSEMENT, DEPENSE, "16"));
        assertFalse(RegleCompteNature.admis(FONCTIONNEMENT, DEPENSE, "8"));
    }

    @Nested
    @DisplayName("Gestion")
    class Gestion {

        @Test
        @DisplayName("Une modification qui ne change pas le libellé est refusée")
        void refuseUneModificationVide() {
            NatureBudgetaireEntity n = nature("VENTES-BOVINS", true);
            when(natureRepository.findById(n.getId())).thenReturn(Optional.of(n));

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class,
                    () -> adapter.initModifier(new ModifierNatureInput(n.getId(), " Ventes de bovins ")));

            assertCode("AUCUNE_MODIFICATION", e);
        }

        @Test
        @DisplayName("Une nature qui qualifie des lignes d'un exercice en cours ne se désactive pas")
        void refuseUneNatureUtilisee() {
            NatureBudgetaireEntity n = nature("VENTES-BOVINS", true);
            when(natureRepository.findById(n.getId())).thenReturn(Optional.of(n));
            when(usageReferentiels.natureUtilisee(n.getId())).thenReturn(true);

            ConflitMetierException e = assertThrows(ConflitMetierException.class,
                    () -> adapter.initDesactiver(new ChangerEtatInput(n.getId(), "Fusion")));

            assertCode("NATURE_UTILISEE", e);
            verifyNoInteractions(workflowService);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Decision {

        @Test
        @DisplayName("Validation d'une création : la nature naît active, sur le compte résolu à la demande")
        void valideUneCreation() {
            UUID natureId = NatureBudgetaireAdapter.identifiantPour("VENTES-BOVINS");
            MakerCheckerRequestEntity demande = demande(natureId, DemandeNatureEvent.creation(
                    "VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, ventes.getId()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);

            adapter.decider(new DeciderDemandeInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null));

            ArgumentCaptor<NatureBudgetaireEntity> creee = ArgumentCaptor.forClass(NatureBudgetaireEntity.class);
            verify(natureRepository).saveAndFlush(creee.capture());
            NatureBudgetaireEntity n = creee.getValue();
            assertEquals("VENTES-BOVINS", n.getCode());
            assertEquals(ventes.getId(), n.getCompteId());
            assertEquals(PRODUITS, n.getSection());
            assertTrue(n.isActif());
            assertEquals(CHEF_BUDGET, n.getCreePar());
            assertEquals(VALIDATEUR, n.getValidePar());
            verify(workflowService).update(demande.getId(), MakerCheckerStatus.ACCEPTED, null, VALIDATEUR);
        }

        @Test
        @DisplayName("Compte retiré du plan entre la demande et la validation : validation refusée")
        void refuseSiLeCompteADisparu() {
            UUID natureId = NatureBudgetaireAdapter.identifiantPour("VENTES-BOVINS");
            MakerCheckerRequestEntity demande = demande(natureId, DemandeNatureEvent.creation(
                    "VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, UUID.randomUUID()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(VALIDATEUR);

            RequeteInvalideException e = assertThrows(RequeteInvalideException.class, () -> adapter.decider(
                    new DeciderDemandeInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("COMPTE_INCONNU", e);
            verify(natureRepository, never()).saveAndFlush(any());
            verify(workflowService, never()).update(any(), any(), any(), any());
        }

        @Test
        @DisplayName("L'auteur ne valide pas sa propre demande")
        void refuseLAuteur() {
            UUID natureId = NatureBudgetaireAdapter.identifiantPour("VENTES-BOVINS");
            MakerCheckerRequestEntity demande = demande(natureId, DemandeNatureEvent.creation(
                    "VENTES-BOVINS", "Ventes de bovins", PRODUITS, RECETTE, ventes.getId()));
            when(demandeRepository.findById(demande.getId())).thenReturn(Optional.of(demande));
            connecte(CHEF_BUDGET);

            ConflitMetierException e = assertThrows(ConflitMetierException.class, () -> adapter.decider(
                    new DeciderDemandeInput(demande.getId(), MakerCheckerStatus.ACCEPTED, null)));

            assertCode("VALIDATEUR_EST_AUTEUR", e);
        }
    }
}
