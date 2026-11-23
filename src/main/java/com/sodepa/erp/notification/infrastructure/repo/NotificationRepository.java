package com.sodepa.erp.notification.infrastructure.repo;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Référentiel d'accès aux données pour l'entité {@link NotificationEntity}.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * Recherche les entités selon le critère {@code findByDestinataireUtilisateurIdOrderByDateEnvoiDesc}.
     */
    List<NotificationEntity> findByDestinataireUtilisateurIdOrderByDateEnvoiDesc(UUID destinataireUtilisateurId);

    /**
     * Recherche paginée des notifications d'un utilisateur destinataire.
     */
    Page<NotificationEntity> findByDestinataireUtilisateurIdOrderByDateEnvoiDesc(UUID destinataireUtilisateurId, Pageable pageable);

    /**
     * Recherche les notifications d'un utilisateur filtrées par liste de statuts.
     */
    List<NotificationEntity> findByDestinataireUtilisateurIdAndStatutInOrderByDateEnvoiDesc(
            UUID destinataireUtilisateurId, Collection<StatutNotification> statuts);

    /**
     * Recherche les notifications d'un utilisateur filtrées strictement par canal et statuts (cloche UI In-App).
     */
    List<NotificationEntity> findByDestinataireUtilisateurIdAndCanalAndStatutInOrderByDateEnvoiDesc(
            UUID destinataireUtilisateurId, CanalNotification canal, Collection<StatutNotification> statuts);

    /**
     * Recherche une notification via sa clé d'idempotence unique.
     */
    Optional<NotificationEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Retrouve toutes les notifications canaux appartenant au même groupe d'émission.
     */
    List<NotificationEntity> findByGroupIdOrderByDateEnvoiAsc(UUID groupId);

    /**
     * Recherche les entités selon le critère {@code findByCanalOrderByDateEnvoiDesc}.
     */
    List<NotificationEntity> findByCanalOrderByDateEnvoiDesc(CanalNotification canal);

    /**
     * Recherche les entités selon le critère {@code findByStatutOrderByDateEnvoiDesc}.
     */
    List<NotificationEntity> findByStatutOrderByDateEnvoiDesc(StatutNotification statut);
}
