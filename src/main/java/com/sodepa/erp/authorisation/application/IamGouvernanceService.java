package com.sodepa.erp.authorisation.application;

import com.sodepa.erp.authorisation.infrastructure.entities.AffectationProfilUtilisateurEntity;
import com.sodepa.erp.authorisation.infrastructure.entities.ProfilFonctionnelEntity;
import com.sodepa.erp.authorisation.infrastructure.entities.RegleSodEntity;
import com.sodepa.erp.authorisation.infrastructure.repo.AffectationProfilUtilisateurRepository;
import com.sodepa.erp.authorisation.infrastructure.repo.ProfilFonctionnelRepository;
import com.sodepa.erp.authorisation.infrastructure.repo.RegleSodRepository;
import com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort;
import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.rh.infrastructure.entities.ContratTravailEntity;
import com.sodepa.erp.rh.infrastructure.entities.StatutContrat;
import com.sodepa.erp.rh.infrastructure.repo.ContratTravailRepository;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.UserManagementEnginePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de Gouvernance IAM, Profils Fonctionnels, Permissions Multidimensionnelles,
 * Matrice de Séparation des Tâches (SoD) et Désactivation Automatique sur Sortie RH
 * (REF-06 / F-REF-024 à F-REF-028 / RG-REF-018, RG-REF-021, RG-REF-022, RG-REF-024, RG-REF-027).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IamGouvernanceService {

    private final ProfilFonctionnelRepository profilRepository;
    private final AffectationProfilUtilisateurRepository affectationProfilRepository;
    private final RegleSodRepository regleSodRepository;
    private final UserRepository userRepository;
    private final ContratTravailRepository contratTravailRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private UtilsService utilsService;

    @Autowired(required = false)
    private KeycloakProvisioningPort keycloakProvisioningPort;

    @Autowired(required = false)
    private UserManagementEnginePort userManagementEnginePort;

    private void requireGovernancePermission(boolean writeOperation) {
        if (utilsService == null || SecurityContextHolder.getContext().getAuthentication() == null) {
            return;
        }
        if (writeOperation) {
            utilsService.hasAnyPermission(Permissions.INIT_CREATE_USER, Permissions.INIT_UPDATE_USER, Permissions.VALIDATE_OR_REJECT_USER);
        } else {
            utilsService.hasAnyPermission(Permissions.GET_FULL_USER_INFO, Permissions.INIT_CREATE_USER, Permissions.INIT_UPDATE_USER);
        }
    }

    private void evictCacheSafe(UUID userId) {
        if (userManagementEnginePort != null) {
            userManagementEnginePort.evictAuthCache(userId);
        }
    }

    // ==========================================
    // PROFILS FONCTIONNELS & PROPAGATION (CA-REF-025-03 / RG-REF-018)
    // ==========================================

    /**
     * Liste tous les profils fonctionnels actifs du référentiel IAM SODEPA.
     *
     * @return Liste triée par code des entités {@link ProfilFonctionnelEntity}.
     */
    @Transactional(readOnly = true)
    public List<ProfilFonctionnelEntity> listerProfilsFonctionnels() {
        return profilRepository.findByActifTrueOrderByCodeAsc();
    }

    /**
     * Crée ou met à jour un profil fonctionnel après contrôle d'absence de conflit SoD interne,
     * puis propage automatiquement ses permissions à tous les utilisateurs porteurs ({@code CA-REF-025-03}).
     *
     * @param profil Entité du profil fonctionnel à créer ou modifiéer.
     * @return Le profil {@link ProfilFonctionnelEntity} sauvegardé et propagé.
     * @throws IllegalStateException Si le profil viole en interne une règle de la matrice SoD.
     */
    @Transactional
    public ProfilFonctionnelEntity creerOuMettreAJourProfil(ProfilFonctionnelEntity input) {
        requireGovernancePermission(true);
        if (input == null || input.getCode() == null || input.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code du profil fonctionnel est obligatoire.");
        }
        String normalizedCode = input.getCode().trim().toUpperCase(Locale.ROOT);
        ProfilFonctionnelEntity target = (input.getId() != null ? profilRepository.findById(input.getId()) : profilRepository.findByCodeIgnoreCase(normalizedCode))
                .orElseGet(ProfilFonctionnelEntity::new);

        target.setCode(normalizedCode);
        target.setLibelle(input.getLibelle());
        target.setDescription(input.getDescription());
        target.setPlafondEngagementFcfa(input.getPlafondEngagementFcfa());
        target.setMfaRequis(input.isMfaRequis());
        target.setPermissions(input.getPermissions() != null ? new HashSet<>(input.getPermissions()) : new HashSet<>());
        target.setActif(true);

        verifierAbsenceConflitSodInterneProfil(target);
        ProfilFonctionnelEntity saved = profilRepository.save(target);

        // Synchronisation MFA et invalidation de cache sans recopier définitivement les permissions du profil dans user.getPermissions()
        List<AffectationProfilUtilisateurEntity> porteurs = affectationProfilRepository.findByProfilIdAndActifTrue(saved.getId());
        for (AffectationProfilUtilisateurEntity aff : porteurs) {
            userRepository.findById(aff.getUtilisateurId()).ifPresent(u -> {
                if (saved.isMfaRequis() && !u.isMfaActive()) {
                    u.setMfaActive(true);
                    userRepository.save(u);
                }
                evictCacheSafe(u.getId());
            });
        }
        log.info("Profil fonctionnel {} sauvegardé (résolution dynamique active pour {} porteurs).",
                saved.getCode(), porteurs.size());
        return saved;
    }

    // ==========================================
    // AFFECTATION DE PROFIL MULTIDIMENSIONNELLE & CONTRÔLE SOD (CA-REF-025-01 & CA-REF-027-01)
    // ==========================================

    /**
     * Attribue un profil fonctionnel à un utilisateur sur un périmètre multidimensionnel
     * (unité organisationnelle, magasin/site, exercice budgétaire/comptable) après vérification bloquante
     * de la matrice de Séparation des Tâches (SoD — {@code CA-REF-025-01} &amp; {@code CA-REF-027-01}).
     *
     * @param utilisateurId            Identifiant UUID de l'utilisateur cible.
     * @param codeProfil               Code du profil fonctionnel à attribuer.
     * @param uniteOrganisationnelleId Périmètre d'unité organisationnelle (optionnel).
     * @param codeMagasinOuSite        Périmètre de site ou magasin (optionnel).
     * @param exerciceAutorise         Millésime d'exercice autorisé (optionnel).
     * @param dateFin                  Date d'expiration de l'habilitation (optionnelle).
     * @return L'affectation {@link AffectationProfilUtilisateurEntity} crée.
     * @throws IllegalStateException Si le cumul avec les profils/permissions existants enfreint la matrice SoD.
     */
    @Transactional
    public AffectationProfilUtilisateurEntity affecterProfilAUtilisateur(
            UUID utilisateurId,
            String codeProfil,
            UUID uniteOrganisationnelleId,
            String codeMagasinOuSite,
            Integer exerciceAutorise,
            LocalDate dateFin) {
        requireGovernancePermission(true);

        UUID actorId = utilsService != null ? utilsService.getCurrentUserIdOrNull() : null;
        if (actorId != null && actorId.equals(utilisateurId) && !utilsService.isAdmin()) {
            throw new AccesRefuseException("Auto-attribution de profil interdite (Principe des 4 yeux).");
        }

        UtilisateurEntity user = userRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));
        ProfilFonctionnelEntity nouveauProfil = profilRepository.findByCodeIgnoreCase(codeProfil)
                .orElseThrow(() -> new IllegalArgumentException("Profil fonctionnel introuvable : " + codeProfil));

        // 1. Vérification de la Matrice SoD avant attribution (CA-REF-027-01 / RG-REF-021, 022, 027)
        LocalDate today = LocalDate.now();
        List<AffectationProfilUtilisateurEntity> affectationsActuelles =
                affectationProfilRepository.findByUtilisateurIdAndActifTrue(utilisateurId).stream()
                        .filter(a -> (a.getDateFin() == null || !a.getDateFin().isBefore(today)))
                        .toList();

        Set<String> codesProfilsCumules = affectationsActuelles.stream()
                .map(a -> a.getProfil().getCode().toUpperCase())
                .collect(Collectors.toSet());
        codesProfilsCumules.add(nouveauProfil.getCode().toUpperCase());

        Set<String> permissionsCumulees = new HashSet<>();
        if (userManagementEnginePort != null) {
            userManagementEnginePort.resolveEffectivePermissions(utilisateurId)
                    .forEach(p -> permissionsCumulees.add(p.name()));
        } else if (user.getPermissions() != null) {
            user.getPermissions().forEach(p -> permissionsCumulees.add(p.name()));
        }
        affectationsActuelles.forEach(a -> a.getProfil().getPermissions().forEach(p -> permissionsCumulees.add(p.name())));
        nouveauProfil.getPermissions().forEach(p -> permissionsCumulees.add(p.name()));

        verifierConformiteMatriceSod(user.getUsername(), codesProfilsCumules, permissionsCumulees);

        // 2. Enregistrement de l'affectation multidimensionnelle (unité, magasin/site, exercice)
        AffectationProfilUtilisateurEntity affectation = AffectationProfilUtilisateurEntity.builder()
                .utilisateurId(utilisateurId)
                .profil(nouveauProfil)
                .uniteOrganisationnelleId(uniteOrganisationnelleId != null ? uniteOrganisationnelleId : user.getUniteOrganisationnelleId())
                .codeMagasinOuSite(codeMagasinOuSite)
                .exerciceAutorise(exerciceAutorise)
                .plafondSpecifiqueFcfa(nouveauProfil.getPlafondEngagementFcfa())
                .dateDebut(LocalDate.now())
                .dateFin(dateFin)
                .actif(true)
                .build();
        affectation = affectationProfilRepository.save(affectation);

        // 3. Synchronisation du MFA sans figer les droits du profil dans user.getPermissions() (résolution dynamique dans UserManagementEngine)
        if (nouveauProfil.isMfaRequis() && !user.isMfaActive()) {
            user.setMfaActive(true);
            userRepository.save(user);
        }
        evictCacheSafe(utilisateurId);

        return affectation;
    }

    /**
     * Liste les affectations de profils fonctionnels actives pour un utilisateur donné.
     *
     * @param utilisateurId Identifiant UUID de l'utilisateur.
     * @return Liste des affectations actives de l'utilisateur.
     */
    @Transactional(readOnly = true)
    public List<AffectationProfilUtilisateurEntity> listerAffectationsUtilisateur(UUID utilisateurId) {
        return affectationProfilRepository.findByUtilisateurIdAndActifTrue(utilisateurId);
    }

    // ==========================================
    // MATRICE DE SÉPARATION DES TÂCHES (SOD — CA-REF-027-01 & 02)
    // ==========================================

    /**
     * Liste toutes les règles actives de la matrice de Séparation des Tâches (SoD).
     *
     * @return Liste triée par code des règles {@link RegleSodEntity}.
     */
    @Transactional(readOnly = true)
    public List<RegleSodEntity> listerReglesSod() {
        return regleSodRepository.findByActifTrueOrderByCodeRegleAsc();
    }

    /**
     * Ajoute ou met à jour une règle d'incompatibilité dans la matrice SoD.
     *
     * @param regle Entité décrivant la règle SoD.
     * @return La règle {@link RegleSodEntity} enregistrée.
     */
    @Transactional
    public RegleSodEntity creerRegleSod(RegleSodEntity input) {
        requireGovernancePermission(true);
        if (input == null || input.getCodeRegle() == null || input.getCodeRegle().isBlank()) {
            throw new IllegalArgumentException("Le code de règle SoD est obligatoire.");
        }
        String code = input.getCodeRegle().trim().toUpperCase(Locale.ROOT);
        RegleSodEntity target = input.getId() != null
                ? regleSodRepository.findById(input.getId()).orElseGet(RegleSodEntity::new)
                : new RegleSodEntity();
        target.setCodeRegle(code);
        target.setLibelle(input.getLibelle());
        target.setTypeIncompatibilite(input.getTypeIncompatibilite() != null ? input.getTypeIncompatibilite().trim().toUpperCase(Locale.ROOT) : "PERMISSION_VS_PERMISSION");
        target.setElementA(input.getElementA() != null ? input.getElementA().trim().toUpperCase(Locale.ROOT) : null);
        target.setElementB(input.getElementB() != null ? input.getElementB().trim().toUpperCase(Locale.ROOT) : null);
        target.setRegleGestionRef(input.getRegleGestionRef());
        target.setRisqueMetier(input.getRisqueMetier());
        target.setBloquant(input.isBloquant());
        target.setActif(true);
        return regleSodRepository.save(target);
    }

    /**
     * Vérifie qu'un profil fonctionnel ne contient pas en lui-même deux permissions mutuellement incompatibles.
     *
     * @param profil Profil fonctionnel à contrôler.
     */
    private void verifierAbsenceConflitSodInterneProfil(ProfilFonctionnelEntity profil) {
        if (profil.getPermissions() == null || profil.getPermissions().isEmpty()) {
            return;
        }
        Set<String> perms = profil.getPermissions().stream().map(Enum::name).collect(Collectors.toSet());
        verifierConformiteMatriceSod("PROFIL:" + profil.getCode(), Set.of(profil.getCode().toUpperCase()), perms);
    }

    /**
     * Contrôle qu'un ensemble de profils et de permissions respecte l'intégralité des règles bloquantes
     * de la matrice de Séparation des Tâches (SoD — {@code CA-REF-027-01}).
     *
     * @param cible        Identifiant ou nom d'utilisateur contrôlé.
     * @param codesProfils Ensemble des codes de profils cumulés.
     * @param permissions  Ensemble des permissions cumulées.
     * @throws IllegalStateException Si une incompatibilité bloquante est détectée.
     */
    public void verifierConformiteMatriceSod(String cible, Set<String> codesProfils, Set<String> permissions) {
        List<RegleSodEntity> regles = regleSodRepository.findByActifTrueOrderByCodeRegleAsc();
        for (RegleSodEntity r : regles) {
            String a = r.getElementA().toUpperCase();
            String b = r.getElementB().toUpperCase();
            boolean conflit = false;
            if ("PROFIL_VS_PROFIL".equalsIgnoreCase(r.getTypeIncompatibilite())) {
                conflit = codesProfils.contains(a) && codesProfils.contains(b);
            } else {
                conflit = permissions.contains(a) && permissions.contains(b);
            }
            if (conflit && r.isBloquant()) {
                throw new IllegalStateException(
                        String.format("Conflit de Séparation des Tâches (SoD — %s / %s / CA-REF-027-01) détecté sur [%s] : "
                                + "impossible de cumuler [%s] et [%s]. Risque : %s",
                                r.getCodeRegle(), r.getRegleGestionRef(), cible, a, b, r.getRisqueMetier()));
            }
        }
    }

    /**
     * Génère l'état de contrôle des conflits de Séparation des Tâches (SoD) sur l'ensemble des comptes actifs ({@code CA-REF-027-02}).
     *
     * @return Liste détaillée des anomalies SoD détectées.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> auditerConflitsSodUtilisateurs() {
        requireGovernancePermission(false);
        List<RegleSodEntity> regles = regleSodRepository.findByActifTrueOrderByCodeRegleAsc();
        List<UtilisateurEntity> users = userRepository.findAll();
        List<Map<String, Object>> anomalies = new ArrayList<>();

        for (UtilisateurEntity u : users) {
            if (!u.isActif()) continue;
            Set<String> perms = userManagementEnginePort != null
                    ? userManagementEnginePort.resolveEffectivePermissions(u.getId()).stream().map(Enum::name).collect(Collectors.toSet())
                    : (u.getPermissions() != null
                            ? u.getPermissions().stream().map(Enum::name).collect(Collectors.toSet())
                            : Set.of());
            Set<String> profils = affectationProfilRepository.findByUtilisateurIdAndActifTrue(u.getId()).stream()
                    .map(a -> a.getProfil().getCode().toUpperCase())
                    .collect(Collectors.toSet());

            for (RegleSodEntity r : regles) {
                String a = r.getElementA().toUpperCase();
                String b = r.getElementB().toUpperCase();
                boolean enConflit = "PROFIL_VS_PROFIL".equalsIgnoreCase(r.getTypeIncompatibilite())
                        ? (profils.contains(a) && profils.contains(b))
                        : (perms.contains(a) && perms.contains(b));
                if (enConflit) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("utilisateurId", u.getId());
                    item.put("username", u.getUsername());
                    item.put("nomComplet", u.getPrenom() + " " + u.getNom());
                    item.put("codeRegleSod", r.getCodeRegle());
                    item.put("regleGestionRef", r.getRegleGestionRef());
                    item.put("elementsIncompatibles", a + " <-> " + b);
                    item.put("risqueMetier", r.getRisqueMetier());
                    anomalies.add(item);
                }
            }
        }
        return anomalies;
    }

    // ==========================================
    // DÉSACTIVATION AUTOMATIQUE SUR SORTIE RH OU EXPIRATION (CA-REF-024-02 & 03 / RG-REF-024)
    // ==========================================

    /**
     * Exécute la revue quotidienne automatique (00h05) et à la demandée :
     * <ol>
     *   <li>Désactive immédiatement les comptes dont la {@code dateExpiration} est dépassée ({@code CA-REF-024-03}) ;</li>
     *   <li>Désactive immédiatement les comptes des agents dont le contrat RH est arrivé à échéance ou résilié ({@code CA-REF-024-02} / {@code RG-REF-024}) ;</li>
     *   <li>Envoie une notification de sécurité par Email et SMS pour chaque compte révoqué.</li>
     * </ol>
     *
     * @return Rapport synthétique des comptes désactivés lors de l'exécution.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 5 0 * * *")
    @net.javacrumbs.shedlock.spring.annotation.SchedulerLock(name = "iamRevueAutomatiqueSortiesRh", lockAtMostFor = "PT10M", lockAtLeastFor = "PT2S")
    @Transactional
    public Map<String, Object> executerRevueAutomatiqueComptesEtSortiesRh() {
        requireGovernancePermission(true);
        LocalDate today = LocalDate.now();
        List<String> comptesDesactives = new ArrayList<>();

        // 1. Comptes temporaires / prestataires arrivés à expiration (CA-REF-024-03)
        for (UtilisateurEntity u : userRepository.findByActifTrueAndDateExpirationBefore(today)) {
            desactiverCompteEtKeycloak(u, "EXPIRE", "Expiration de la période d'accès temporaire au " + u.getDateExpiration());
            comptesDesactives.add(u.getUsername() + " (EXPIRATION_ACCES_TEMPORAIRE : " + u.getDateExpiration() + ")");
        }

        // 2. Agents RH en fin de contrat / sortie des effectifs (CA-REF-024-02 / RG-REF-024)
        for (ContratTravailEntity contrat : contratTravailRepository.findContratsEchusPourUtilisateursActifs(
                today, List.of(StatutContrat.RESILIE, StatutContrat.CLOTURE))) {
            UtilisateurEntity u = contrat.getEmploye().getUtilisateur();
            if (u != null && u.isActif()) {
                desactiverCompteEtKeycloak(u, "DESACTIVE_SORTIE_RH", "Sortie RH / Fin du contrat " + contrat.getNumeroContrat());
                comptesDesactives.add(u.getUsername() + " (SORTIE_RH_CONTRAT : " + contrat.getNumeroContrat() + ")");
            }
        }

        Map<String, Object> rapport = new LinkedHashMap<>();
        rapport.put("dateExecution", today);
        rapport.put("nombreComptesDesactives", comptesDesactives.size());
        rapport.put("comptesDesactives", comptesDesactives);
        return rapport;
    }

    private void desactiverCompteEtKeycloak(UtilisateurEntity u, String statutCompte, String motif) {
        u.setActif(false);
        u.setStatutCompte(statutCompte);
        userRepository.save(u);
        revoquerAffectationsProfils(u.getId());
        evictCacheSafe(u.getId());
        if (keycloakProvisioningPort != null) {
            try {
                keycloakProvisioningPort.setUserActive(u.getUsername(), false);
            } catch (Exception e) {
                log.warn("Impossible de désactiver le compte Keycloak '{}' lors de la revue RH : {}", u.getUsername(), e.getMessage());
            }
        }
        notifierSecuriteDesactivationCompte(u, motif);
    }

    /**
     * Révoque toutes les affectations de profils fonctionnels actives d'un utilisateur désactivé.
     *
     * @param utilisateurId Identifiant UUID de l'utilisateur.
     */
    private void revoquerAffectationsProfils(UUID utilisateurId) {
        List<AffectationProfilUtilisateurEntity> actives =
                affectationProfilRepository.findByUtilisateurIdAndActifTrue(utilisateurId);
        for (AffectationProfilUtilisateurEntity aff : actives) {
            aff.setActif(false);
            aff.setDateFin(LocalDate.now());
            affectationProfilRepository.save(aff);
        }
    }

    /**
     * Envoie une notification Email et SMS lors de la désactivation automatique d'un compte utilisateur.
     *
     * @param utilisateur Compte utilisateur désactivé.
     * @param motif       Motif de la révocation automatique.
     */
    private void notifierSecuriteDesactivationCompte(UtilisateurEntity utilisateur, String motif) {
        try {
            notificationService.notifierEmailEtSms(
                    utilisateur,
                    null,
                    "[ERP SODEPA - Sécurité IAM] Désactivation automatique du compte " + utilisateur.getUsername(),
                    "Le compte utilisateur " + utilisateur.getUsername() + " a été automatiquement désactivé conformément à la politique de sécurité IAM (RG-REF-024). Motif : " + motif,
                    "IAM_DESACTIVATION_AUTOMATIQUE",
                    "UTILISATEUR",
                    utilisateur.getId().toString()
            );
        } catch (Exception e) {
            log.warn("Notification de désactivation IAM non envoyée : {}", e.getMessage());
        }
    }
}
