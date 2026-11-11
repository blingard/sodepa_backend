package com.sodepa.erp.budget;

import com.sodepa.erp.budget.application.inputs.EngagementInput;
import com.sodepa.erp.budget.application.inputs.ReallocationInput;
import com.sodepa.erp.budget.infrastructure.adapter.BudgetPlanAdapter;
import com.sodepa.erp.budget.infrastructure.entities.*;
import com.sodepa.erp.budget.infrastructure.repo.*;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetModuleRulesTest {

    @Mock
    private BudgetPlanRepository budgetPlanRepository;

    @Mock
    private BudgetItemRepository budgetItemRepository;

    @Mock
    private BudgetEngagementRepository budgetEngagementRepository;

    @Mock
    private BudgetReallocationRepository budgetReallocationRepository;

    @Mock
    private AuditTrailRepository auditTrailRepository;

    @Mock
    private UtilsService utilsService;

    @Mock
    private WorkflowService workflowService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BudgetPlanAdapter budgetPlanAdapter;

    private BudgetPlanEntity planExecutoire;
    private BudgetItemEntity itemSource;
    private BudgetItemEntity itemDest;

    @BeforeEach
    void setUp() {
        planExecutoire = BudgetPlanEntity.builder()
                .id(UUID.randomUUID())
                .annee(2026)
                .intitule("Budget SODEPA 2026")
                .version(1)
                .statut(StatutBudget.EXECUTOIRE)
                .totalBudget(new BigDecimal("100000000"))
                .creeLe(LocalDateTime.now())
                .build();

        itemSource = BudgetItemEntity.builder()
                .id(UUID.randomUUID())
                .budgetPlan(planExecutoire)
                .sectionId(UUID.randomUUID())
                .compteCode("601100")
                .montantAnnuel(new BigDecimal("10000000")) // 10 millions FCFA dotation initiale
                .montantPlanned(new BigDecimal("10000000"))
                .montantEngage(BigDecimal.ZERO)
                .montantReal(BigDecimal.ZERO)
                .periodes(new ArrayList<>())
                .build();

        // 12 périodes mensuelles
        for (int i = 1; i <= 12; i++) {
            itemSource.getPeriodes().add(BudgetItemPeriodeEntity.builder()
                    .id(UUID.randomUUID())
                    .budgetItem(itemSource)
                    .periodeNum(i)
                    .montantPlanned(new BigDecimal("833333.33"))
                    .montantReal(BigDecimal.ZERO)
                    .build());
        }

        itemDest = BudgetItemEntity.builder()
                .id(UUID.randomUUID())
                .budgetPlan(planExecutoire)
                .sectionId(itemSource.getSectionId()) // même section
                .compteCode("601200")
                .montantAnnuel(new BigDecimal("5000000"))
                .montantPlanned(new BigDecimal("5000000"))
                .montantEngage(BigDecimal.ZERO)
                .montantReal(BigDecimal.ZERO)
                .periodes(new ArrayList<>())
                .build();

        for (int i = 1; i <= 12; i++) {
            itemDest.getPeriodes().add(BudgetItemPeriodeEntity.builder()
                    .id(UUID.randomUUID())
                    .budgetItem(itemDest)
                    .periodeNum(i)
                    .montantPlanned(new BigDecimal("416666.66"))
                    .montantReal(BigDecimal.ZERO)
                    .build());
        }
    }

    @Test
    @DisplayName("RG-BUD-001 & RG-BUD-002 : Statuts budgétaires et règles d'exécutabilité")
    void testStatutBudgetTransitionsAndFlags() {
        assertTrue(StatutBudget.EXECUTOIRE.isExecutoire());
        assertTrue(StatutBudget.PUBLISHED.isExecutoire());
        assertTrue(StatutBudget.RECTIFIE.isExecutoire());

        assertFalse(StatutBudget.DRAFT.isExecutoire());
        assertFalse(StatutBudget.EN_PREPARATION.isExecutoire());
        assertFalse(StatutBudget.SUBMITTED.isExecutoire());
        assertFalse(StatutBudget.SOUMIS.isExecutoire());
        assertFalse(StatutBudget.REJECTED.isExecutoire());
        assertFalse(StatutBudget.CLOTURE.isExecutoire());

        assertTrue(StatutBudget.DRAFT.isModifiable());
        assertTrue(StatutBudget.EN_PREPARATION.isModifiable());
        assertTrue(StatutBudget.REJECTED.isModifiable());
        assertFalse(StatutBudget.EXECUTOIRE.isModifiable());
    }

    @Test
    @DisplayName("RG-BUD-018 & CA-BUD-022-01 : Réallocation simple <= 5% de la dotation initiale même section")
    void testReallocationSimple5PourcentOuMoins() {
        // Montant de 500 000 FCFA sur dotation initiale de 10 000 000 FCFA = 5.00% pile
        BigDecimal montant = new BigDecimal("500000");
        UUID reallocId = UUID.randomUUID();

        when(budgetItemRepository.findById(itemSource.getId())).thenReturn(Optional.of(itemSource));
        when(budgetItemRepository.findById(itemDest.getId())).thenReturn(Optional.of(itemDest));

        Map<String, Object> payload = Map.of(
                "sourceItemId", itemSource.getId().toString(),
                "destItemId", itemDest.getId().toString(),
                "montant", montant,
                "responsableId", UUID.randomUUID().toString(),
                "raison", "Ajustement vaccins bétail"
        );

        ReflectionTestUtils.invokeMethod(budgetPlanAdapter, "creationReallocation", payload, reallocId);

        verify(budgetReallocationRepository).save(argThat(r -> {
            assertEquals(TypeReallocation.SIMPLE, r.getTypeReallocation());
            assertEquals(StatutReallocation.EXECUTEE, r.getStatut());
            assertEquals(0, r.getTauxReallocation().compareTo(new BigDecimal("5.000000")));
            assertFalse(r.isMesureConservatoire());
            return true;
        }));
    }

    @Test
    @DisplayName("RG-BUD-018 & CA-BUD-022-02 : Réallocation significative > 5% de la dotation initiale")
    void testReallocationSignificativePlusDe5Pourcent() {
        // Montant de 600 000 FCFA sur 10 000 000 FCFA = 6.00% (> 5%)
        BigDecimal montant = new BigDecimal("600000");
        UUID reallocId = UUID.randomUUID();

        when(budgetItemRepository.findById(itemSource.getId())).thenReturn(Optional.of(itemSource));
        when(budgetItemRepository.findById(itemDest.getId())).thenReturn(Optional.of(itemDest));

        Map<String, Object> payload = Map.of(
                "sourceItemId", itemSource.getId().toString(),
                "destItemId", itemDest.getId().toString(),
                "montant", montant,
                "responsableId", UUID.randomUUID().toString(),
                "raison", "Renforcement clôture ranch"
        );

        ReflectionTestUtils.invokeMethod(budgetPlanAdapter, "creationReallocation", payload, reallocId);

        verify(budgetReallocationRepository).save(argThat(r -> {
            assertEquals(TypeReallocation.SIGNIFICATIVE, r.getTypeReallocation());
            return true;
        }));
    }

    @Test
    @DisplayName("RG-BUD-017 & CA-BUD-023-03 : Mesure conservatoire d'extrême urgence autorisée par le DG (statut À régulariser CA)")
    void testReallocationMesureConservatoireUrgence() {
        BigDecimal montant = new BigDecimal("1200000"); // 12% > 5%
        UUID reallocId = UUID.randomUUID();

        when(budgetItemRepository.findById(itemSource.getId())).thenReturn(Optional.of(itemSource));
        when(budgetItemRepository.findById(itemDest.getId())).thenReturn(Optional.of(itemDest));

        Map<String, Object> payload = Map.of(
                "sourceItemId", itemSource.getId().toString(),
                "destItemId", itemDest.getId().toString(),
                "montant", montant,
                "responsableId", UUID.randomUUID().toString(),
                "raison", "Urgence vétérinaire épizootie",
                "mesureConservatoire", true,
                "justificationUrgence", "Suspicion foyer fièvre aphteuse au Ranch de Faro"
        );

        ReflectionTestUtils.invokeMethod(budgetPlanAdapter, "creationReallocation", payload, reallocId);

        verify(budgetReallocationRepository).save(argThat(r -> {
            assertTrue(r.isMesureConservatoire());
            assertEquals(StatutReallocation.A_REGULARISER_CA, r.getStatut());
            assertEquals(TypeReallocation.SIGNIFICATIVE, r.getTypeReallocation());
            assertEquals("Suspicion foyer fièvre aphteuse au Ranch de Faro", r.getJustificationUrgence());
            return true;
        }));
    }

    @Test
    @DisplayName("RG-BUD-006 & RG-BUD-007 : Détermination automatique du circuit (Menue vs Grosse dépense seuil 500 000 FCFA)")
    void testSeuilMenueDepenseVsGrosseDepense() {
        when(budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(any(), any(), any()))
                .thenReturn(Optional.of(itemSource));
        when(budgetEngagementRepository.save(any(BudgetEngagementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Cas 1 : 450 000 FCFA < 500 000 FCFA -> MENUE_DEPENSE
        Map<String, Object> payloadMenue = Map.of(
                "planId", planExecutoire.getId().toString(),
                "compteCode", itemSource.getCompteCode(),
                "sectionId", itemSource.getSectionId().toString(),
                "numeroEngagement", "BE-2026-HQ-001",
                "description", "Achat petit matériel",
                "montant", new BigDecimal("450000"),
                "utilisateurId", UUID.randomUUID().toString()
        );

        BudgetEngagementEntity beMenue = ReflectionTestUtils.invokeMethod(
                budgetPlanAdapter, "creationEngagement", payloadMenue, UUID.randomUUID());
        assertNotNull(beMenue);
        assertEquals("MENUE_DEPENSE", beMenue.getCircuit());

        // Cas 2 : 500 000 FCFA pile >= 500 000 FCFA -> GROSSE_DEPENSE
        Map<String, Object> payloadGrosse = Map.of(
                "planId", planExecutoire.getId().toString(),
                "compteCode", itemSource.getCompteCode(),
                "sectionId", itemSource.getSectionId().toString(),
                "numeroEngagement", "BE-2026-HQ-002",
                "description", "Pièces de rechange chaîne froid",
                "montant", new BigDecimal("500000"),
                "utilisateurId", UUID.randomUUID().toString()
        );

        BudgetEngagementEntity beGrosse = ReflectionTestUtils.invokeMethod(
                budgetPlanAdapter, "creationEngagement", payloadGrosse, UUID.randomUUID());
        assertNotNull(beGrosse);
        assertEquals("GROSSE_DEPENSE", beGrosse.getCircuit());
    }

    @Test
    @DisplayName("RG-BUD-004 & CA-BUD-011-02 : Rejet bloquant si crédit budgétaire insuffisant")
    void testControleEngagementBloquantSiDepassement() {
        when(budgetItemRepository.findByBudgetPlanIdAndCompteCodeAndSectionId(any(), any(), any()))
                .thenReturn(Optional.of(itemSource));

        // Disponible sur itemSource = 10 000 000 FCFA. Demande de 10 000 001 FCFA
        Map<String, Object> payloadExces = Map.of(
                "planId", planExecutoire.getId().toString(),
                "compteCode", itemSource.getCompteCode(),
                "sectionId", itemSource.getSectionId().toString(),
                "numeroEngagement", "BE-2026-HQ-999",
                "description", "Achat tracteur",
                "montant", new BigDecimal("10000001"),
                "utilisateurId", UUID.randomUUID().toString()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ReflectionTestUtils.invokeMethod(budgetPlanAdapter, "creationEngagement", payloadExces, UUID.randomUUID()));

        assertTrue(ex.getMessage().contains("Crédit budgétaire insuffisant"));
    }
}
