package com.sodepa.erp.notification.infrastructure.repo;

import com.sodepa.erp.notification.infrastructure.entities.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Référentiel des modèles (templates) de notifications stockés en base de données.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByCode(String code);

    Optional<NotificationTemplateEntity> findByTypeEvenementAndLangueAndActifTrue(String typeEvenement, String langue);

    Optional<NotificationTemplateEntity> findByTypeEvenementAndLangue(String typeEvenement, String langue);

    List<NotificationTemplateEntity> findAllByOrderByTypeEvenementAscLangueAsc();
}
