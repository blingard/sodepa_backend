package com.sodepa.erp.achats.application;

import com.sodepa.erp.achats.infrastructure.entities.ExpressionBesoinEntity;
import com.sodepa.erp.achats.infrastructure.repo.ExpressionBesoinRepository;
import com.sodepa.erp.achats.infrastructure.repo.MarcheDossierRepository;
import com.sodepa.erp.achats.infrastructure.repo.MarcheReceptionPvRepository;
import com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.workflow.application.WorkflowService;
import com.sodepa.erp.share.MakerCheckerOutput;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service de sécurité pour le module Achats & Marchés Publics.
 * Gère le filtrage hiérarchique selon la structure d'affectation de l'utilisateur connecté (materializedPath)
 * et le contrôle strict des habilitations par poste.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchatsSecurityService {

    private final UtilsService utilsService;
    private final AffectationPosteRepository affectationPosteRepository;
    private final MarcheDossierRepository marcheDossierRepository;
    private final MarcheReceptionPvRepository marcheReceptionPvRepository;
    private final ExpressionBesoinRepository expressionBesoinRepository;
    private final UniteOrganisationnelleRepository uniteOrganisationnelleRepository;
    @org.springframework.context.annotation.Lazy
    private final WorkflowService workflowService;

    public UtilsService getUtilsService() {
        return utilsService;
    }

    public void checkPermission(Permissions permission) {
        utilsService.hasPermission(permission);
    }

    public boolean canValidate(Permissions permission) {
        try {
            utilsService.hasPermission(permission);
            return true;
        } catch (AccesRefuseException e) {
            return false;
        }
    }

    public UserData getCurrentUserData() {
        return utilsService.getCurrentUserData();
    }

    public UUID getCurrentUserId() {
        return UUID.fromString(getCurrentUserData().userId());
    }

    @Transactional(readOnly = true)
    public Optional<AffectationPosteEntity> getActiveAffectation() {
        UUID userId = getCurrentUserId();
        LocalDate today = LocalDate.now();
        List<AffectationPosteEntity> list = affectationPosteRepository.findByUtilisateurIdAndStatut(userId, StatutAffectation.ACTIF);
        return list.stream()
                .filter(a -> (a.getDateDebut() == null || !a.getDateDebut().isAfter(today)) &&
                             (a.getDateFin() == null || !a.getDateFin().isBefore(today)))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<UniteOrganisationnelleEntity> getCurrentUserUnite() {
        return getActiveAffectation()
                .map(AffectationPosteEntity::getPoste)
                .map(PosteEntity::getUniteOrganisationnelle);
    }

    @Transactional(readOnly = true)
    public boolean isGlobalScope() {
        UserData user = getCurrentUserData();
        if ("admin_maker".equalsIgnoreCase(user.username()) || "admin_checker".equalsIgnoreCase(user.username())) {
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

        // Le Directeur Général (POSTE_DG) a une portée globale sur toute la SODEPA
        if ("POSTE_DG".equalsIgnoreCase(poste.getCodePoste())) {
            return true;
        }

        UniteOrganisationnelleEntity unite = poste.getUniteOrganisationnelle();
        if (unite == null) {
            return false;
        }

        String path = unite.getMaterializedPath();
        return path == null || "/DG/".equals(path) || "/".equals(path);
    }

    @Transactional(readOnly = true)
    public String getScopeMaterializedPath() {
        if (isGlobalScope()) {
            return null; // Null signifie portée illimitée (pas de filtrage WHERE LIKE)
        }
        return getCurrentUserUnite()
                .map(UniteOrganisationnelleEntity::getMaterializedPath)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasAccessToUnite(UniteOrganisationnelleEntity targetUnite) {
        if (isGlobalScope() || targetUnite == null) {
            return true;
        }
        String scopePath = getScopeMaterializedPath();
        if (scopePath == null) {
            return true;
        }
        String targetPath = targetUnite.getMaterializedPath();
        return targetPath != null && targetPath.startsWith(scopePath);
    }

    @Transactional(readOnly = true)
    public void validateAccessToUnite(UniteOrganisationnelleEntity targetUnite, String messageErreur) {
        if (!hasAccessToUnite(targetUnite)) {
            throw new AccesRefuseException(messageErreur != null ? messageErreur :
                    "Accès refusé : cette ressource appartient à une structure hors de votre périmètre hiérarchique.");
        }
    }

    @Transactional(readOnly = true)
    public void validateAccessToUnite(UUID requestId, MakerCheckerEntityName entityName, String messageErreur) {
        if (isGlobalScope() || requestId == null) {
            return;
        }

        try {
            MakerCheckerOutput checkerOutput = workflowService.findById(requestId);
            if (checkerOutput == null) {
                return;
            }

            UniteOrganisationnelleEntity targetUnite = resolveTargetUnite(checkerOutput, entityName);
            if (targetUnite != null) {
                validateAccessToUnite(targetUnite, messageErreur);
            }
        } catch (AccesRefuseException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Impossible de résoudre l'unité organisationnelle pour la demande Maker-Checker {}: {}", requestId, e.getMessage());
        }
    }

    private UniteOrganisationnelleEntity resolveTargetUnite(MakerCheckerOutput request, MakerCheckerEntityName entityName) {
        if (request == null) return null;

        Map<String, Object> payload = request.payload();
        if (entityName == MakerCheckerEntityName.MARCHE_RECEPTION_PV) {
            Object dossierIdObj = payload != null ? payload.get("dossierId") : null;
            if (dossierIdObj != null) {
                UUID dossierId = UUID.fromString(dossierIdObj.toString());
                return marcheDossierRepository.findById(dossierId)
                        .map(d -> d.getExpressionBesoin() != null ? d.getExpressionBesoin().getUniteOrganisationnelle() : null)
                        .orElse(null);
            }
        } else if (entityName == MakerCheckerEntityName.LIQUIDATION_FACTURE) {
            if (request.entityPk() != null) {
                try {
                    UUID pvId = UUID.fromString(request.entityPk());
                    return marcheReceptionPvRepository.findById(pvId)
                            .map(pv -> pv.getDossier() != null && pv.getDossier().getExpressionBesoin() != null
                                    ? pv.getDossier().getExpressionBesoin().getUniteOrganisationnelle()
                                    : null)
                            .orElse(null);
                } catch (Exception ignored) {
                }
            }
        } else if (entityName == MakerCheckerEntityName.EXPRESSION_BESOIN) {
            Object uniteIdObj = payload != null ? payload.get("uniteOrganisationnelleId") : null;
            if (uniteIdObj != null) {
                try {
                    UUID uId = UUID.fromString(uniteIdObj.toString());
                    return uniteOrganisationnelleRepository.findById(uId).orElse(null);
                } catch (Exception ignored) {
                }
            }
            if (request.entityPk() != null) {
                try {
                    UUID ebId = UUID.fromString(request.entityPk());
                    return expressionBesoinRepository.findById(ebId)
                            .map(ExpressionBesoinEntity::getUniteOrganisationnelle)
                            .orElse(null);
                } catch (Exception ignored) {
                }
            }
        } else if (entityName == MakerCheckerEntityName.MARCHE_DOSSIER) {
            Object ebIdObj = payload != null ? payload.get("expressionBesoinId") : null;
            if (ebIdObj != null) {
                try {
                    UUID ebId = UUID.fromString(ebIdObj.toString());
                    return expressionBesoinRepository.findById(ebId)
                            .map(ExpressionBesoinEntity::getUniteOrganisationnelle)
                            .orElse(null);
                } catch (Exception ignored) {
                }
            }
            if (request.entityPk() != null) {
                try {
                    UUID dossierId = UUID.fromString(request.entityPk());
                    return marcheDossierRepository.findById(dossierId)
                            .map(d -> d.getExpressionBesoin() != null ? d.getExpressionBesoin().getUniteOrganisationnelle() : null)
                            .orElse(null);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
