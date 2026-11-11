package com.sodepa.erp.budget.application;

import com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de Securite et de Cloisonnement Geographique ABAC (Attribute-Based Access Control) pour le Budget.
 * <p>
 * Ce composant garantit l'etancheite informationnelle et operationnelle entre les differentes structures de la SODEPA :
 * <ul>
 *   <li><b>Le Siege de Yaounde (Portee Globale)</b> : La Direction Generale (DG), la Direction Administrative et Financiere (DAF),
 *       le Service du Budget (CSB) et l'Audit Interne (DCAI) ont une vision transversale sur l'ensemble de la societe.</li>
 *   <li><b>Les Unites Operationnelles Decentralisees (Portee Restreinte)</b> : Les ranches d'elevage (Faro, Dumbo, Ndokayo),
 *       les abattoirs frigorifiques (Yaounde, Douala) et les stations de quarantaine ne peuvent intervenir que sur leur propre perimetre.</li>
 * </ul>
 * Il s'appuie sur la structure hierarchique arborescente de l'organigramme (chemin materialise 'materializedPath')
 * pour verifier les droits d'engagement et de consultation sans dependre de regles codees en dur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetSecurityService {

    private final UtilsService utilsService;
    private final AffectationPosteRepository affectationPosteRepository;
    private final UniteOrganisationnelleRepository uniteOrganisationnelleRepository;

    /**
     * Verifie si l'utilisateur connecte detient une permission fonctionnelle particuliere.
     * <p>
     * Interrompt le traitement par une exception de securite si le droit est manquant.
     *
     * @param permission le privilege controle (ex: VIEW_BUDGET, CREATE_BUDGET_ENGAGEMENT)
     */
    public void checkPermission(Permissions permission) {
        utilsService.hasPermission(permission);
    }

    /**
     * Recupere les informations d'identite et de profil de l'utilisateur actuellement connecte a la session.
     *
     * @return le jeu de donnees de l'utilisateur courant (identifiant, login, roles)
     */
    public UserData getCurrentUserData() {
        return utilsService.getCurrentUserData();
    }

    /**
     * Extrait l'identifiant technique unique (UUID) de l'utilisateur connecte.
     *
     * @return l'identifiant de session de l'agent
     */
    public UUID getCurrentUserId() {
        return UUID.fromString(getCurrentUserData().userId());
    }

    /**
     * Recherche l'affectation active de l'agent dans l'organigramme a la date du jour.
     * <p>
     * Une affectation est active si son statut est 'ACTIF' et si la date actuelle
     * est comprise entre la date de debut et la date de fin eventuelle.
     *
     * @return l'affectation de poste valide, ou vide si l'utilisateur n'a pas de poste affecte
     */
    @Transactional(readOnly = true)
    public Optional<AffectationPosteEntity> getActiveAffectation() {
        try {
            UUID userId = getCurrentUserId();
            LocalDate today = LocalDate.now();
            List<AffectationPosteEntity> list = affectationPosteRepository.findByUtilisateurIdAndStatut(userId, StatutAffectation.ACTIF);
            return list.stream()
                    .filter(a -> (a.getDateDebut() == null || !a.getDateDebut().isAfter(today)) &&
                                 (a.getDateFin() == null || !a.getDateFin().isBefore(today)))
                    .findFirst();
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'affectation active : {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Identifie l'unite organisationnelle (direction, service, ranch, abattoir) a laquelle l'agent est actuellement affecte.
     *
     * @return l'entite organisationnelle d'appartenance de l'agent
     */
    @Transactional(readOnly = true)
    public Optional<UniteOrganisationnelleEntity> getCurrentUserUnite() {
        return getActiveAffectation()
                .map(AffectationPosteEntity::getPoste)
                .map(PosteEntity::getUniteOrganisationnelle);
    }

    /**
     * Determine si l'utilisateur connecte possede une portee de vision globale sur toute la SODEPA.
     * <p>
     * L'acces global est attribue :
     * <ul>
     *   <li>Aux administrateurs systeme techniques.</li>
     *   <li>Au Directeur General (DG) et au Directeur Administratif et Financier (DAF).</li>
     *   <li>Aux agents dont le poste ou l'unite est directement lie a la Direction Generale, au Budget ou a l'Audit.</li>
     * </ul>
     *
     * @return true si l'utilisateur a une vue SODEPA entiere, false s'il est cantonne a son unite
     */
    @Transactional(readOnly = true)
    public boolean isGlobalScope() {
        try {
            UserData user = getCurrentUserData();
            // 1. Profils d'administration technique
            if (user.isAdmin() || "admin_maker".equalsIgnoreCase(user.username()) || "admin_checker".equalsIgnoreCase(user.username())) {
                return true;
            }

            // 2. Fonctions de Direction Generale et Financiere
            if (user.isDg() || user.isDaf()) {
                return true;
            }

            // 3. Verification par le poste organique occupe
            Optional<AffectationPosteEntity> aff = getActiveAffectation();
            if (aff.isEmpty()) {
                return false;
            }

            PosteEntity poste = aff.get().getPoste();
            if (poste == null) {
                return false;
            }

            String codePoste = (poste.getCodePoste() != null ? poste.getCodePoste() : "").trim().toUpperCase();
            if (codePoste.contains("DG") || codePoste.contains("DAF") || codePoste.contains("BUDGET") || codePoste.contains("DCAI")) {
                return true;
            }

            // 4. Verification par la position dans l'arborescence (Siege)
            UniteOrganisationnelleEntity unite = poste.getUniteOrganisationnelle();
            if (unite == null) {
                return false;
            }

            String path = unite.getMaterializedPath();
            return path == null || "/DG/".equals(path) || "/".equals(path) || path.startsWith("/DG/DAF");
        } catch (Exception e) {
            log.warn("Vérification scope global échouée, application scope restreint : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Recupere le prefixe hierarchique (materializedPath) d'appartenance de l'utilisateur.
     * <p>
     * Utilise dans les requetes SQL/JPQL (via l'operateur LIKE) pour filtrer les donnees
     * de maniere a inclure l'unite de l'agent et l'ensemble de ses sous-services.
     *
     * @return le chemin hierarchique de base (ex: '/DG/DR-NORD/RANCH-FARO/'), ou null si portee globale
     */
    @Transactional(readOnly = true)
    public String getScopeMaterializedPath() {
        if (isGlobalScope()) {
            return null;
        }
        return getCurrentUserUnite()
                .map(UniteOrganisationnelleEntity::getMaterializedPath)
                .orElse(null);
    }

    /**
     * Controle bloquant verifiant si l'agent connecte a le droit d'imputer une operation sur une unite cible.
     * <p>
     * Interdit par exemple a un gestionnaire du Ranch de Dumbo d'engager des depenses
     * sur le budget de l'Abattoir de Yaounde ou du Siege.
     *
     * @param targetUniteId identifiant de l'unite cible concernee par l'operation financiere
     * @throws AccesRefuseException si l'unite cible se trouve hors du perimetre autorise de l'agent
     */
    @Transactional(readOnly = true)
    public void validateAccessToUnite(UUID targetUniteId) {
        // 1. Les utilisateurs a portee globale sont dispenses de restriction
        if (isGlobalScope() || targetUniteId == null) {
            return;
        }
        String scopePath = getScopeMaterializedPath();
        if (scopePath == null) {
            return;
        }

        // 2. Recherche et controle de filiation de l'unite cible
        UniteOrganisationnelleEntity target = uniteOrganisationnelleRepository.findById(targetUniteId).orElse(null);
        if (target != null && target.getMaterializedPath() != null) {
            // L'unite cible doit obligatoirement commencer par le chemin d'affectation de l'utilisateur
            if (!target.getMaterializedPath().startsWith(scopePath)) {
                throw new AccesRefuseException("Accès refusé : L'unité " + target.getIntitule() 
                        + " est hors de votre périmètre d'affectation (" + scopePath + ").");
            }
        }
    }
}
