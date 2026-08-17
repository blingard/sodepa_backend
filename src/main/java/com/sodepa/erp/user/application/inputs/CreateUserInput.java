package com.sodepa.erp.user.application.inputs;

import com.sodepa.erp.utils.Permissions;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Données d'entrée pour créer un utilisateur.
 */
public record CreateUserInput(
        String username,
        String nom,
        String prenom,
        String email,
        Set<String> telephones,
        Set<Permissions> permissions,
        MultipartFile photoProfile
) {
}
