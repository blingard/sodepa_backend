package com.sodepa.erp.user.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort;
import com.sodepa.erp.configuration.parametres.application.ParametreConfigurationService;
import com.sodepa.erp.organigramme.infrastructure.entities.*;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.TypeStructureRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.rh.infrastructure.entities.*;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service assurant l'initialisation complète de l'écosystème de démarrage SODEPA :
 * 1. Structures organisationnelles de référence (Types et Unités hiérarchiques).
 * 2. Configurations système et paramètres.
 * 3. Postes budgétés avec leurs permissions associées (dont le Poste DG doté de tous les droits).
 * 4. Affectation du premier utilisateur opérationnel au poste de direction.
 * 5. Création du premier dossier Employé SODEPA lié à ce compte utilisateur.
 * 6. Désactivation irréversible des comptes racines bootstrap 'admin_maker' et 'admin_checker' (en base et dans Keycloak).
 * 7. Traçabilité intégrale de tous les événements d'initialisation dans ClickHouse (audit infalsifiable).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BootstrapInitializationService {

    private final TypeStructureRepository typeStructureRepository;
    private final UniteOrganisationnelleRepository uniteRepository;
    private final PosteRepository posteRepository;
    private final AffectationPosteRepository affectationRepository;
    private final EmployeRepository employeRepository;
    private final UserRepository userRepository;
    private final KeycloakProvisioningPort keycloakProvisioningPort;
    private final ParametreConfigurationService configurationService;
    private final ClickHouseManager clickHouseManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public void ensureBootstrapStructuresAndDeactivateRoots(UtilisateurEntity firstUser) {
        log.info("Vérification et initialisation des structures d'accueil pour le nouvel utilisateur: {}", firstUser.getUsername());

        // 1. Types de structures
        TypeStructureEntity typeDg = ensureTypeStructure("DIRECTION_GENERALE", "Direction Générale", 1, null);
        TypeStructureEntity typeDirection = ensureTypeStructure("DIRECTION", "Direction Centrale", 2, typeDg);
        TypeStructureEntity typeService = ensureTypeStructure("SERVICE", "Service Rattaché", 3, typeDirection);
        TypeStructureEntity typeRanch = ensureTypeStructure("RANCH", "Ranch d'Élevage", 3, typeDirection);
        TypeStructureEntity typeAbattoir = ensureTypeStructure("ABATTOIR", "Abattoir Frigorifique", 3, typeDirection);

        // 2. Unités organisationnelles
        UniteOrganisationnelleEntity uniteDg = ensureUnite("DG", "Direction Générale SODEPA", TypeUniteOrganisationnelle.DIRECTION_GENERALE, typeDg, null, "/DG/", "Siège Yaoundé");
        UniteOrganisationnelleEntity uniteDaf = ensureUnite("DAF", "Direction Administrative et Financière", TypeUniteOrganisationnelle.DIRECTION, typeDirection, uniteDg, "/DG/DAF/", "Siège Yaoundé");
        UniteOrganisationnelleEntity uniteDt = ensureUnite("DT", "Direction Technique", TypeUniteOrganisationnelle.DIRECTION, typeDirection, uniteDg, "/DG/DT/", "Siège Yaoundé");
        UniteOrganisationnelleEntity uniteRanchNdokayo = ensureUnite("RANCH_NDOKAYO", "Ranch de Ndokayo", TypeUniteOrganisationnelle.RANCH, typeRanch, uniteDt, "/DG/DT/RANCH_NDOKAYO/", "Ndokayo");
        UniteOrganisationnelleEntity uniteAbattoirYde = ensureUnite("ABATTOIR_YDE", "Abattoir de Yaoundé", TypeUniteOrganisationnelle.ABATTOIR, typeAbattoir, uniteDt, "/DG/DT/ABATTOIR_YDE/", "Yaoundé");

        // 3. Configurations
        configurationService.rechargerParametres();

        // 4. Postes budgétés
        Set<Permissions> allPermissions = new HashSet<>(Arrays.asList(Permissions.values()));
        PosteEntity posteDg = ensurePoste("POSTE_DG", "Directeur Général", uniteDg, null, CategorieConvention.CADRE_DIRIGEANT, allPermissions);
        ensurePoste("POSTE_DAF", "Directeur Administratif et Financier", uniteDaf, posteDg, CategorieConvention.CADRE_SUPERIEUR, allPermissions);

        // 5. Affectation du premier utilisateur opérationnel au poste de DG
        List<AffectationPosteEntity> existingAffs = affectationRepository.findByUtilisateurIdAndStatut(firstUser.getId(), StatutAffectation.ACTIF);
        if (existingAffs.isEmpty()) {
            AffectationPosteEntity aff = AffectationPosteEntity.builder()
                    .id(UUID.randomUUID())
                    .utilisateur(firstUser)
                    .poste(posteDg)
                    .typeAffectation(TypeAffectation.TITULAIRE)
                    .dateDebut(LocalDate.now())
                    .statut(StatutAffectation.ACTIF)
                    .referenceDecision("DÉCISION D'INSTALLATION N°001/SODEPA/INITIALISATION")
                    .build();
            affectationRepository.save(aff);
            posteDg.setEffectifActuel(1);
            posteRepository.save(posteDg);
            log.info("Premier utilisateur {} affecté comme TITULAIRE au poste {} avec toutes les habilitations.", firstUser.getUsername(), posteDg.getIntitule());

            trackEventInClickHouse("AFFECTATION_POSTE", aff.getId().toString(), Map.of(
                    "action", "AFFECTATION_TITULAIRE_DG",
                    "username", firstUser.getUsername(),
                    "poste", posteDg.getCodePoste()
            ), firstUser.getId().toString());
        }

        // 6. Création du dossier Employé pour le premier utilisateur (Règle: Tout employé est d'abord un utilisateur)
        if (!employeRepository.existsByUtilisateurId(firstUser.getId())) {
            EmployeEntity employe = EmployeEntity.builder()
                    .id(UUID.randomUUID())
                    .matricule("SDP-" + LocalDate.now().getYear() + "-0001")
                    .nom(firstUser.getNom())
                    .prenom(firstUser.getPrenom())
                    .sexe(Sexe.M)
                    .dateNaissance(LocalDate.of(1980, 1, 1))
                    .situationMatrimoniale(SituationMatrimoniale.MARIE)
                    .nombreEnfantsACharge(0)
                    .email(firstUser.getEmail())
                    .telephone(firstUser.getTelephones() != null && !firstUser.getTelephones().isEmpty()
                            ? firstUser.getTelephones().iterator().next() : "+237600000000")
                    .typeContrat(TypeContrat.CDI)
                    .dateEmbauche(LocalDate.now())
                    .statutActivite(StatutActivite.EN_FONCTION)
                    .uniteOrganisationnelle(uniteDg)
                    .poste(posteDg)
                    .utilisateur(firstUser)
                    .build();
            employeRepository.save(employe);
            log.info("Dossier Employé créé pour le Directeur Général: {} ({})", employe.getMatricule(), firstUser.getUsername());

            trackEventInClickHouse("EMPLOYE", employe.getId().toString(), Map.of(
                    "action", "CREATION_EMPLOYE_ROOT_DG",
                    "matricule", employe.getMatricule(),
                    "username", firstUser.getUsername()
            ), firstUser.getId().toString());
        }

        // 7. Désactivation stricte des comptes bootstrap 'admin_maker' et 'admin_checker'
        deactivateSystemBootstrapUser("admin_maker", firstUser.getId().toString());
        deactivateSystemBootstrapUser("admin_checker", firstUser.getId().toString());
    }

    private void deactivateSystemBootstrapUser(String username, String actorId) {
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.isActif()) {
                user.setActif(false);
                userRepository.save(user);
                log.info("Utilisateur système racine '{}' désactivé en base locale suite à la création du premier utilisateur opérationnel.", username);
            }
        });

        try {
            keycloakProvisioningPort.setUserActive(username, false);
            log.info("Utilisateur système racine '{}' désactivé dans Keycloak.", username);
        } catch (Exception e) {
            log.warn("Erreur lors de la désactivation Keycloak de '{}': {}", username, e.getMessage());
        }

        trackEventInClickHouse("USER", username, Map.of(
                "action", "DEACTIVATION_BOOTSTRAP_ROOT",
                "username", username,
                "motif", "Initialisation complète de l'ERP SODEPA terminée"
        ), actorId);
    }

    private void trackEventInClickHouse(String entityName, String entityPk, Object payloadObj, String actorId) {
        try {
            String payloadStr = objectMapper.writeValueAsString(payloadObj);
            clickHouseManager.saveMakerChecker(
                    MakerCheckerMessageInput.builder()
                            .id(UUID.randomUUID())
                            .entityName(entityName)
                            .entityPk(entityPk)
                            .payload(payloadStr)
                            .timestamp(LocalDateTime.now())
                            .maker_id(actorId)
                            .checker_id("SYSTEM")
                            .build()
            );
        } catch (Exception e) {
            log.warn("Journalisation ClickHouse non bloquante ignorée pour {} ({}): {}", entityName, entityPk, e.getMessage());
        }
    }

    private TypeStructureEntity ensureTypeStructure(String code, String libelle, int niveau, TypeStructureEntity parent) {
        return typeStructureRepository.findByCode(code).orElseGet(() -> {
            TypeStructureEntity entity = TypeStructureEntity.builder()
                    .id(UUID.randomUUID())
                    .code(code)
                    .libelle(libelle)
                    .niveauHierarchique(niveau)
                    .typeParent(parent)
                    .description("Type de structure SODEPA " + libelle)
                    .build();
            return typeStructureRepository.save(entity);
        });
    }

    private UniteOrganisationnelleEntity ensureUnite(String code, String intitule, TypeUniteOrganisationnelle typeUnite, TypeStructureEntity typeStructure, UniteOrganisationnelleEntity parent, String matPath, String localisation) {
        return uniteRepository.findByCode(code).orElseGet(() -> {
            UniteOrganisationnelleEntity entity = UniteOrganisationnelleEntity.builder()
                    .id(UUID.randomUUID())
                    .code(code)
                    .intitule(intitule)
                    .typeUnite(typeUnite)
                    .typeStructure(typeStructure)
                    .parent(parent)
                    .materializedPath(matPath)
                    .localisation(localisation)
                    .dateCreation(LocalDate.now())
                    .statut(StatutUnite.ACTIF)
                    .build();
            return uniteRepository.save(entity);
        });
    }

    private PosteEntity ensurePoste(String codePoste, String intitule, UniteOrganisationnelleEntity unite, PosteEntity posteSuperieur, CategorieConvention categorie, Set<Permissions> permissions) {
        return posteRepository.findByCodePoste(codePoste).map(p -> {
            if (p.getPermissions() == null || p.getPermissions().isEmpty()) {
                p.setPermissions(new HashSet<>(permissions));
                return posteRepository.save(p);
            }
            return p;
        }).orElseGet(() -> {
            PosteEntity p = PosteEntity.builder()
                    .id(UUID.randomUUID())
                    .codePoste(codePoste)
                    .intitule(intitule)
                    .uniteOrganisationnelle(unite)
                    .posteSuperieur(posteSuperieur)
                    .effectifAutorise(1)
                    .effectifActuel(0)
                    .categorieConvention(categorie)
                    .statut(StatutPoste.OUVERT)
                    .descriptionMission("Poste budgété de direction SODEPA")
                    .permissions(new HashSet<>(permissions))
                    .build();
            return posteRepository.save(p);
        });
    }
}
