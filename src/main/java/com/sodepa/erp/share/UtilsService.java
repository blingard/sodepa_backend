package com.sodepa.erp.share;

import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.utils.Permissions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `UtilsService` au sein de la SODEPA.
 */
@Component
public class UtilsService {

    public boolean isAdmin() {
        UserData currentUser = getCurrentUser().getUserData().orElseThrow(() -> new NonAuthentifieException("Profil utilisateur introuvable."));
        if(Objects.equals(currentUser.username(), "admin_maker") || Objects.equals(currentUser.username(), "admin_checker")) {
            return true;
        }
        return false;
    }

    public boolean isSuperviseurOuAdmin(EmployeEntity employe, UserData userDate) {

        if (userDate.permissions().size() == Permissions.values().length) {
            return true;
        }
        // 1. Contrôle sur le compte utilisateur technique (Rôles ADMIN / SUPER_ADMIN)
        if (userDate.isAdmin() || userDate.isDaf() || userDate.isDg()) {
            return true;
        }

        // 2. Contrôle sur la fiche de poste et l'organigramme de l'employé
        if (employe != null && employe.getPoste() != null) {
            String codePoste = (employe.getPoste().getCodePoste() != null ? employe.getPoste().getCodePoste() : "").toUpperCase();
            String intitulePoste = (employe.getPoste().getIntitule() != null ? employe.getPoste().getIntitule() : "").toUpperCase();
            String signaturePoste = codePoste + " " + intitulePoste;

            if (signaturePoste.contains("DIRECTEUR_GENERAL") || signaturePoste.contains("DIRECTEUR GENERAL") || signaturePoste.contains("DG")
                    || signaturePoste.contains("DIRECTEUR_DAF") || signaturePoste.contains("DAF") || signaturePoste.contains("FINANCIER")
                    || signaturePoste.contains("PCA") || signaturePoste.contains("CONSEIL")
                    || signaturePoste.contains("AUDIT_INTERNE") || signaturePoste.contains("AUDITEUR")
                    || signaturePoste.contains("CONTROLEUR_GESTION") || signaturePoste.contains("CONTROLE DE GESTION")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Utilisateur courant, tel que le filtre JWT l'a posé dans le contexte.
     *
     * <p>
     * Les échecs sont des refus d'authentification, non des pannes : ils
     * remontent en 401 pour que le client sache qu'il doit se reconnecter, au
     * lieu du 500 indéchiffrable rendu jusqu'ici.
     * </p>
     */
    public CurrentUserAuthenticationToken getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new NonAuthentifieException("Aucune authentification dans le contexte de sécurité.");
        }

        if (!(authentication instanceof CurrentUserAuthenticationToken currentUserAuth)) {
            throw new NonAuthentifieException(
                    "Jeton d'authentification inattendu : " + authentication.getClass().getSimpleName());
        }

        if (currentUserAuth.getUserData().isEmpty()) {
            throw new NonAuthentifieException("Profil utilisateur absent du contexte d'authentification.");
        }

        return currentUserAuth;
    }

    public UserData getCurrentUserData() {
        return getCurrentUser().getUserData().orElseThrow(
                () -> new NonAuthentifieException("Profil utilisateur introuvable."));
    }

    /**
     * Exige une habilitation, et rend un refus lisible si elle manque.
     *
     * <p>
     * Le message nomme l'habilitation attendue : sans elle, le diagnostic d'un
     * 403 obligeait à relire le code pour deviner laquelle était en cause.
     * </p>
     */
    public void hasPermission(Permissions permissions) {
        CurrentUserAuthenticationToken currentUserAuthenticationToken = getCurrentUser();
        if (!currentUserAuthenticationToken.getUserData().get().permissions().contains(permissions)) {
            throw new AccesRefuseException(
                    "Habilitation requise pour cette opération : " + permissions.name());
        }
    }

    /** Absence ou invalidité du contexte d'authentification. */
    private static class NonAuthentifieException
            extends com.sodepa.erp.share.erreurs.ApiException {
        NonAuthentifieException(String message) {
            super(org.springframework.http.HttpStatus.UNAUTHORIZED, "NON_AUTHENTIFIE", message);
        }
    }
}
