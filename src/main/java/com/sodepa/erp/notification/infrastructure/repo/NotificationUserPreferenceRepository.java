package com.sodepa.erp.notification.infrastructure.repo;

import com.sodepa.erp.notification.infrastructure.entities.NotificationUserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Référentiel des préférences individuelles de notification par utilisateur.
 */
@Repository
public interface NotificationUserPreferenceRepository extends JpaRepository<NotificationUserPreferenceEntity, UUID> {

    Optional<NotificationUserPreferenceEntity> findByUtilisateurId(UUID utilisateurId);
}
