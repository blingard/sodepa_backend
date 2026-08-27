package com.sodepa.erp.share;

import com.sodepa.erp.share.erreurs.AccesRefuseException;
import com.sodepa.erp.utils.Permissions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UtilsService {

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
