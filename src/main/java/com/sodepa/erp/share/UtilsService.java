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
        if (userDate == null) {
            return false;
        }
        if (userDate.permissions().contains(Permissions.WORKFLOW_SUPERVISION)
                || userDate.permissions().size() == Permissions.values().length) {
            return true;
        }
        // 1. Contrôle sur le compte utilisateur technique (Rôles ADMIN / SUPER_ADMIN / DAF / DG)
        if (userDate.isAdmin() || userDate.isDaf() || userDate.isDg()) {
            return true;
        }

        // 2. Contrôle sur la fiche de poste de l'organigramme (stricte validation des postes de direction)
        if (employe != null && employe.getPoste() != null) {
            String codePoste = (employe.getPoste().getCodePoste() != null ? employe.getPoste().getCodePoste() : "").trim().toUpperCase();
            String intitulePoste = (employe.getPoste().getIntitule() != null ? employe.getPoste().getIntitule() : "").trim().toUpperCase();

            if (codePoste.equals("POSTE_DG") || codePoste.equals("DG")
                    || codePoste.equals("POSTE_DIRECTEUR_DAF") || codePoste.equals("DAF")
                    || intitulePoste.equals("DIRECTEUR GENERAL") || intitulePoste.equals("DIRECTEUR GÉNÉRAL")
                    || intitulePoste.equals("DIRECTEUR ADMINISTRATIF ET FINANCIER")) {
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
        UserData ud = currentUserAuthenticationToken.getUserData().orElseThrow(
                () -> new NonAuthentifieException("Profil utilisateur introuvable."));
        if (ud.isAdmin() || ud.permissions().size() == Permissions.values().length || ud.permissions().contains(permissions)) {
            return;
        }
        throw new AccesRefuseException(
                "Habilitation requise pour cette opération : " + permissions.name());
    }

    /** Absence ou invalidité du contexte d'authentification. */
    private static class NonAuthentifieException
            extends com.sodepa.erp.share.erreurs.ApiException {
        NonAuthentifieException(String message) {
            super(org.springframework.http.HttpStatus.UNAUTHORIZED, "NON_AUTHENTIFIE", message);
        }
    }
}
