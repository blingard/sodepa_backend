package com.sodepa.erp.notification.infrastructure.repo;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Référentiel des règles événement × canal du moteur de notifications.
 */
@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, UUID> {

    List<NotificationRuleEntity> findByTypeEvenementOrderByOrdrePrioriteAsc(String typeEvenement);

    Optional<NotificationRuleEntity> findByTypeEvenementAndCanal(String typeEvenement, CanalNotification canal);

    List<NotificationRuleEntity> findAllByOrderByTypeEvenementAscOrdrePrioriteAsc();
}
