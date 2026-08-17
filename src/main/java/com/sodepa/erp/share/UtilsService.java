package com.sodepa.erp.share;

import com.sodepa.erp.utils.Permissions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UtilsService {

    public CurrentUserAuthenticationToken getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("No authentication found in security context");
        }

        if (!(authentication instanceof CurrentUserAuthenticationToken)) {
            throw new RuntimeException(
                    "Authentication is not of type CurrentUserAuthenticationToken. " +
                            "Please ensure you are authenticated with a valid JWT token. " +
                            "Current type: " + authentication.getClass().getSimpleName()
            );
        }
        CurrentUserAuthenticationToken currentUserAuth = (CurrentUserAuthenticationToken) authentication;
        if (currentUserAuth.getUserData().isEmpty()) {
            throw new RuntimeException("User data is missing in authentication context.");
        }
        if(currentUserAuth.getUserData().isEmpty())
            throw new IllegalArgumentException("User data are empty");
        return currentUserAuth;
    }
    public UserData getCurrentUserData() {
        CurrentUserAuthenticationToken currentUserAuth = getCurrentUser();
        if(currentUserAuth.getUserData().isEmpty())
            throw new RuntimeException("User not found");
        return currentUserAuth.getUserData().get();
    }

    public void hasPermission(Permissions permissions){
        CurrentUserAuthenticationToken currentUserAuthenticationToken = getCurrentUser();
        if(!currentUserAuthenticationToken.getUserData().get().permissions().contains(permissions))
            throw new RuntimeException("You're not authorise to perform this operation");
    }
}