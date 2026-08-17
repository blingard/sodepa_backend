package com.sodepa.erp.authentication.application.ports;

import java.util.Map;
import java.util.UUID;

/**
 * Port interface for Keycloak user provisioning and administration operations.
 */
public interface KeycloakProvisioningPort {

    /**
     * Creates a new user in Keycloak with the default password.
     * 
     * @param id        the unique identifier of the user (from ERP)
     * @param username  the username
     * @param email     the email address
     * @param firstName the first name
     * @param lastName  the last name
     * @param active    whether the user is enabled
     */
    UUID createKeycloakUser(UUID id, String username, String email, String firstName, String lastName, boolean active);

    /**
     * Changes/resets the password of a user in Keycloak.
     * 
     * @param userId      the user unique identifier (Keycloak sub / ID)
     * @param newPassword the new password
     */
    void changePassword(String userId, String newPassword);

}
