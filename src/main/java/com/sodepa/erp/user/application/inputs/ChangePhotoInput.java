package com.sodepa.erp.user.application.inputs;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Données d'entrée pour changer la photo de profil.
 */
public record ChangePhotoInput(
        UUID id,
        MultipartFile photoProfile
) {
}
