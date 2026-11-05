package com.sodepa.erp.organigramme.application.ports;

import com.sodepa.erp.organigramme.application.outputs.ChaineHierarchiqueOutput;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeDelegation;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de résolution hiérarchique et de délégation managériale transverse.
 * Utilisé par les modules Budget (Maker-Checker, dérogations), RH (congés), Achats, etc.
 */
public interface HierarchieResolutionPort {

    Optional<UtilisateurEntity> getManagerDirect(UUID utilisateurId);

    List<UtilisateurEntity> getChaineHierarchique(UUID utilisateurId, int maxNiveaux);

    ChaineHierarchiqueOutput getChaineHierarchiqueDetaillee(UUID utilisateurId, int maxNiveaux);

    Optional<UtilisateurEntity> getResponsableUnite(UUID uniteId);

    boolean isSuperieurHierarchique(UUID managerId, UUID collaborateurId);

    boolean hasAccessToUnite(UUID utilisateurId, UUID uniteId);

    Optional<UtilisateurEntity> resoudreApprobateurActuel(UUID demandeurId, BigDecimal montant, TypeDelegation type);
}
