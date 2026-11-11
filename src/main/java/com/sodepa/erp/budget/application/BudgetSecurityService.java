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
 * Service de sécurité ABAC (Attribute-Based Access Control) pour le module Budget.
 * Assure le cloisonnement géographique et organisationnel strict entre le Siège SODEPA
 * et les Unités Opérationnelles déconcentrées (Ranches, Abattoirs, Stations).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetSecurityService {

    private final UtilsService utilsService;
    private final AffectationPosteRepository affectationPosteRepository;
    private final UniteOrganisationnelleRepository uniteOrganisationnelleRepository;

    public void checkPermission(Permissions permission) {
        utilsService.hasPermission(permission);
    }

    public UserData getCurrentUserData() {
        return utilsService.getCurrentUserData();
    }

    public UUID getCurrentUserId() {
        return UUID.fromString(getCurrentUserData().userId());
    }

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

    @Transactional(readOnly = true)
    public Optional<UniteOrganisationnelleEntity> getCurrentUserUnite() {
        return getActiveAffectation()
                .map(AffectationPosteEntity::getPoste)
                .map(PosteEntity::getUniteOrganisationnelle);
    }

    /**
     * Détermine si l'utilisateur a une portée globale (DG, DAF, CSB, Contrôleur DCAI, Admin).
     */
    @Transactional(readOnly = true)
    public boolean isGlobalScope() {
        try {
            UserData user = getCurrentUserData();
            if (user.isAdmin() || "admin_maker".equalsIgnoreCase(user.username()) || "admin_checker".equalsIgnoreCase(user.username())) {
                return true;
            }

            if (user.isDg() || user.isDaf()) {
                return true;
            }

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
     * Retourne le chemin matérialisé de rattachement de l'utilisateur pour le filtrage hiérarchique.
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
     * Valide qu'une unité cible est bien dans le périmètre autorisé de l'utilisateur connecté.
     */
    @Transactional(readOnly = true)
    public void validateAccessToUnite(UUID targetUniteId) {
        if (isGlobalScope() || targetUniteId == null) {
            return;
        }
        String scopePath = getScopeMaterializedPath();
        if (scopePath == null) {
            return;
        }

        UniteOrganisationnelleEntity target = uniteOrganisationnelleRepository.findById(targetUniteId).orElse(null);
        if (target != null && target.getMaterializedPath() != null) {
            if (!target.getMaterializedPath().startsWith(scopePath)) {
                throw new AccesRefuseException("Accès refusé : L'unité " + target.getIntitule() 
                        + " est hors de votre périmètre d'affectation (" + scopePath + ").");
            }
        }
    }
}
