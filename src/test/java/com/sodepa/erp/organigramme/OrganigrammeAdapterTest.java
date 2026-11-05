package com.sodepa.erp.organigramme;

import com.sodepa.erp.organigramme.application.exceptions.BudgetedHeadcountExceededException;
import com.sodepa.erp.organigramme.application.exceptions.ConflictAffectationException;
import com.sodepa.erp.organigramme.application.exceptions.CyclicDependencyException;
import com.sodepa.erp.organigramme.application.inputs.*;
import com.sodepa.erp.organigramme.application.outputs.*;
import com.sodepa.erp.organigramme.infrastructure.adapter.OrganigrammeAdapter;
import com.sodepa.erp.organigramme.infrastructure.entities.*;
import com.sodepa.erp.organigramme.infrastructure.repo.*;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganigrammeAdapterTest {

    @Mock
    private UniteOrganisationnelleRepository uniteRepository;

    @Mock
    private PosteRepository posteRepository;

    @Mock
    private AffectationPosteRepository affectationRepository;

    @Mock
    private OrganigrammeVersionRepository versionRepository;

    @Mock
    private DelegationPouvoirRepository delegationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganigrammeAdapter adapter;

    private UniteOrganisationnelleEntity directionGenerale;
    private UniteOrganisationnelleEntity ranchJakiri;
    private PosteEntity posteDG;
    private PosteEntity posteChefRanch;
    private UtilisateurEntity userDG;
    private UtilisateurEntity userChefRanch;

    @BeforeEach
    void setUp() {
        directionGenerale = UniteOrganisationnelleEntity.builder()
                .id(UUID.randomUUID())
                .code("DG")
                .intitule("Direction Générale SODEPA")
                .typeUnite(TypeUniteOrganisationnelle.DIRECTION_GENERALE)
                .materializedPath("/DG/")
                .localisation("Yaoundé")
                .statut(StatutUnite.ACTIF)
                .dateCreation(LocalDate.of(2025, 1, 1))
                .build();

        ranchJakiri = UniteOrganisationnelleEntity.builder()
                .id(UUID.randomUUID())
                .code("RNCH-JAK")
                .intitule("Ranch de Jakiri")
                .typeUnite(TypeUniteOrganisationnelle.RANCH)
                .parent(directionGenerale)
                .materializedPath("/DG/RNCH-JAK/")
                .localisation("Jakiri, Nord-Ouest")
                .statut(StatutUnite.ACTIF)
                .dateCreation(LocalDate.of(2025, 1, 1))
                .build();

        posteDG = PosteEntity.builder()
                .id(UUID.randomUUID())
                .codePoste("PST-DG")
                .intitule("Directeur Général")
                .uniteOrganisationnelle(directionGenerale)
                .posteSuperieur(null)
                .effectifAutorise(1)
                .effectifActuel(1)
                .categorieConvention(CategorieConvention.CADRE_DIRIGEANT)
                .statut(StatutPoste.OUVERT)
                .build();

        posteChefRanch = PosteEntity.builder()
                .id(UUID.randomUUID())
                .codePoste("PST-CHEF-RNCH-JAK")
                .intitule("Chef de Ranch de Jakiri")
                .uniteOrganisationnelle(ranchJakiri)
                .posteSuperieur(posteDG)
                .effectifAutorise(1)
                .effectifActuel(1)
                .categorieConvention(CategorieConvention.CADRE_SUPERIEUR)
                .statut(StatutPoste.OUVERT)
                .build();

        userDG = UtilisateurEntity.builder()
                .id(UUID.randomUUID())
                .username("dg.sodepa")
                .nom("Directeur")
                .prenom("Général")
                .email("dg@sodepa.cm")
                .actif(true)
                .build();

        userChefRanch = UtilisateurEntity.builder()
                .id(UUID.randomUUID())
                .username("chef.jakiri")
                .nom("Tchinda")
                .prenom("Paul")
                .email("tchinda@sodepa.cm")
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("Création d'une unité racine : chemin matérialisé '/CODE/'")
    void testCreerUniteRacine() {
        when(uniteRepository.existsByCode("DG")).thenReturn(false);
        when(uniteRepository.save(any(UniteOrganisationnelleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreerUniteInput input = CreerUniteInput.builder()
                .code("DG")
                .intitule("Direction Générale SODEPA")
                .typeUnite(TypeUniteOrganisationnelle.DIRECTION_GENERALE)
                .parentId(null)
                .localisation("Yaoundé")
                .build();

        UniteOrganisationnelleOutput output = adapter.creerUnite(input);

        assertNotNull(output);
        assertEquals("/DG/", output.materializedPath());
        assertEquals("DG", output.code());
    }

    @Test
    @DisplayName("Création d'une unité enfant : chemin matérialisé concaténé depuis le parent")
    void testCreerUniteEnfant() {
        when(uniteRepository.existsByCode("RNCH-FARO")).thenReturn(false);
        when(uniteRepository.findById(directionGenerale.getId())).thenReturn(Optional.of(directionGenerale));
        when(uniteRepository.save(any(UniteOrganisationnelleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreerUniteInput input = CreerUniteInput.builder()
                .code("RNCH-FARO")
                .intitule("Ranch de Faro")
                .typeUnite(TypeUniteOrganisationnelle.RANCH)
                .parentId(directionGenerale.getId())
                .localisation("Faro")
                .build();

        UniteOrganisationnelleOutput output = adapter.creerUnite(input);

        assertNotNull(output);
        assertEquals("/DG/RNCH-FARO/", output.materializedPath());
        assertEquals("RNCH-FARO", output.code());
    }

    @Test
    @DisplayName("Détection de cycle lors du déplacement d'une unité")
    void testDeplacerUniteCycleRejete() {
        // Tenter de déplacer Direction Générale (/DG/) comme enfant du Ranch de Jakiri (/DG/RNCH-JAK/)
        when(uniteRepository.findById(directionGenerale.getId())).thenReturn(Optional.of(directionGenerale));
        when(uniteRepository.findById(ranchJakiri.getId())).thenReturn(Optional.of(ranchJakiri));

        DeplacerUniteInput input = DeplacerUniteInput.builder()
                .uniteId(directionGenerale.getId())
                .nouveauParentId(ranchJakiri.getId())
                .build();

        assertThrows(CyclicDependencyException.class, () -> adapter.deplacerUnite(input));
    }

    @Test
    @DisplayName("Rejet du dépassement de l'effectif budgété autorisé")
    void testDepassementEffectifAutoriseRejete() {
        when(userRepository.findById(userChefRanch.getId())).thenReturn(Optional.of(userChefRanch));
        when(posteRepository.findById(posteChefRanch.getId())).thenReturn(Optional.of(posteChefRanch));
        when(affectationRepository.findFirstByUtilisateurIdAndTypeAffectationAndStatut(
                userChefRanch.getId(), TypeAffectation.TITULAIRE, StatutAffectation.ACTIF))
                .thenReturn(Optional.empty());

        // Effectif autorisé = 1, et il y a déjà 1 actif
        when(affectationRepository.countByPosteIdAndStatut(posteChefRanch.getId(), StatutAffectation.ACTIF))
                .thenReturn(1L);

        AffecterCollaborateurInput input = AffecterCollaborateurInput.builder()
                .utilisateurId(userChefRanch.getId())
                .posteId(posteChefRanch.getId())
                .typeAffectation(TypeAffectation.TITULAIRE)
                .dateDebut(LocalDate.now())
                .build();

        assertThrows(BudgetedHeadcountExceededException.class, () -> adapter.affecterCollaborateur(input));
    }

    @Test
    @DisplayName("Rejet de double affectation titulaire active")
    void testDoubleAffectationTitulaireRejetee() {
        when(userRepository.findById(userChefRanch.getId())).thenReturn(Optional.of(userChefRanch));
        when(posteRepository.findById(posteChefRanch.getId())).thenReturn(Optional.of(posteChefRanch));

        AffectationPosteEntity existante = AffectationPosteEntity.builder()
                .id(UUID.randomUUID())
                .utilisateur(userChefRanch)
                .poste(posteDG)
                .typeAffectation(TypeAffectation.TITULAIRE)
                .statut(StatutAffectation.ACTIF)
                .build();

        when(affectationRepository.findFirstByUtilisateurIdAndTypeAffectationAndStatut(
                userChefRanch.getId(), TypeAffectation.TITULAIRE, StatutAffectation.ACTIF))
                .thenReturn(Optional.of(existante));

        AffecterCollaborateurInput input = AffecterCollaborateurInput.builder()
                .utilisateurId(userChefRanch.getId())
                .posteId(posteChefRanch.getId())
                .typeAffectation(TypeAffectation.TITULAIRE)
                .dateDebut(LocalDate.now())
                .build();

        assertThrows(ConflictAffectationException.class, () -> adapter.affecterCollaborateur(input));
    }

    @Test
    @DisplayName("Résolution managériale N+1 directe")
    void testResolutionManagerDirect() {
        AffectationPosteEntity affChefRanch = AffectationPosteEntity.builder()
                .id(UUID.randomUUID())
                .utilisateur(userChefRanch)
                .poste(posteChefRanch)
                .typeAffectation(TypeAffectation.TITULAIRE)
                .statut(StatutAffectation.ACTIF)
                .build();

        AffectationPosteEntity affDG = AffectationPosteEntity.builder()
                .id(UUID.randomUUID())
                .utilisateur(userDG)
                .poste(posteDG)
                .typeAffectation(TypeAffectation.TITULAIRE)
                .statut(StatutAffectation.ACTIF)
                .build();

        when(affectationRepository.findByUtilisateurIdAndStatut(userChefRanch.getId(), StatutAffectation.ACTIF))
                .thenReturn(List.of(affChefRanch));

        when(affectationRepository.findByPosteIdAndStatut(posteDG.getId(), StatutAffectation.ACTIF))
                .thenReturn(List.of(affDG));

        Optional<UtilisateurEntity> manager = adapter.getManagerDirect(userChefRanch.getId());

        assertTrue(manager.isPresent());
        assertEquals(userDG.getId(), manager.get().getId());
        assertEquals("Directeur", manager.get().getNom());
    }

    @Test
    @DisplayName("Routage vers le délégataire en cas de délégation active du manager")
    void testRoutageDelegationActuelle() {
        UtilisateurEntity delegataire = UtilisateurEntity.builder()
                .id(UUID.randomUUID())
                .username("adjoint.dg")
                .nom("Adjoint")
                .prenom("DG")
                .email("adjoint@sodepa.cm")
                .actif(true)
                .build();

        AffectationPosteEntity affChefRanch = AffectationPosteEntity.builder()
                .id(UUID.randomUUID())
                .utilisateur(userChefRanch)
                .poste(posteChefRanch)
                .typeAffectation(TypeAffectation.TITULAIRE)
                .statut(StatutAffectation.ACTIF)
                .build();

        AffectationPosteEntity affDG = AffectationPosteEntity.builder()
                .id(UUID.randomUUID())
                .utilisateur(userDG)
                .poste(posteDG)
                .typeAffectation(TypeAffectation.TITULAIRE)
                .statut(StatutAffectation.ACTIF)
                .build();

        when(affectationRepository.findByUtilisateurIdAndStatut(userChefRanch.getId(), StatutAffectation.ACTIF))
                .thenReturn(List.of(affChefRanch));

        when(affectationRepository.findByPosteIdAndStatut(posteDG.getId(), StatutAffectation.ACTIF))
                .thenReturn(List.of(affDG));

        DelegationPouvoirEntity delegation = DelegationPouvoirEntity.builder()
                .id(UUID.randomUUID())
                .delegant(userDG)
                .delegataire(delegataire)
                .typeDelegation(TypeDelegation.TOTALE)
                .plafondMontant(BigDecimal.valueOf(10_000_000))
                .dateDebut(Instant.now().minus(1, ChronoUnit.DAYS))
                .dateFin(Instant.now().plus(5, ChronoUnit.DAYS))
                .statut(StatutDelegation.ACTIVE)
                .build();

        when(delegationRepository.findActiveDelegations(eq(userDG.getId()), any(Instant.class), eq(TypeDelegation.BUDGETAIRE_SEULE)))
                .thenReturn(List.of(delegation));

        Optional<UtilisateurEntity> approbateur = adapter.resoudreApprobateurActuel(
                userChefRanch.getId(), BigDecimal.valueOf(5_000_000), TypeDelegation.BUDGETAIRE_SEULE);

        assertTrue(approbateur.isPresent());
        assertEquals(delegataire.getId(), approbateur.get().getId());
        assertEquals("Adjoint", approbateur.get().getNom());
    }

    @Test
    @DisplayName("Contrôle d'accès basé sur la structure (ABAC) : visibilité selon le chemin matérialisé")
    void testControleAccesUniteABAC() {
        AffectationPosteEntity affChefRanch = AffectationPosteEntity.builder()
                .id(UUID.randomUUID())
                .utilisateur(userChefRanch)
                .poste(posteChefRanch)
                .typeAffectation(TypeAffectation.TITULAIRE)
                .statut(StatutAffectation.ACTIF)
                .build();

        when(affectationRepository.findByUtilisateurIdAndStatut(userChefRanch.getId(), StatutAffectation.ACTIF))
                .thenReturn(List.of(affChefRanch));

        when(uniteRepository.findById(ranchJakiri.getId())).thenReturn(Optional.of(ranchJakiri));
        when(uniteRepository.findById(directionGenerale.getId())).thenReturn(Optional.of(directionGenerale));

        // Le chef de ranch (/DG/RNCH-JAK/) a accès à sa propre unité
        assertTrue(adapter.hasAccessToUnite(userChefRanch.getId(), ranchJakiri.getId()));

        // Le chef de ranch n'a pas accès à la Direction Générale (/DG/)
        assertFalse(adapter.hasAccessToUnite(userChefRanch.getId(), directionGenerale.getId()));
    }
}
