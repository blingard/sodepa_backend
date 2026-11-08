package com.sodepa.erp.notification.infrastructure.repo;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import com.sodepa.erp.notification.infrastructure.entities.NotificationEntity;
import com.sodepa.erp.notification.infrastructure.entities.StatutNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Référentiel d'accès aux données pour l'historique et le suivi des notifications Email, SMS et internes.
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * Recherche les notifications adressées à un utilisateur donné, triées de la plus récente à la plus ancienne.
     *
     * @param destinataireUtilisateurId Identifiant UUID de l'utilisateur destinataire.
     * @return Liste chronologique inversée des notifications de l'utilisateur.
     */
    List<NotificationEntity> findByDestinataireUtilisateurIdOrderByDateEnvoiDesc(UUID destinataireUtilisateurId);

    /**
     * Recherche les notifications filtrées par canal de diffusion (EMAIL, SMS, EMAIL_ET_SMS).
     *
     * @param canal Canal de notification recherché.
     * @return Liste des notifications correspondantes.
     */
    List<NotificationEntity> findByCanalOrderByDateEnvoiDesc(CanalNotification canal);

    /**
     * Recherche les notifications selon leur statut d'acheminement ou de lecture.
     *
     * @param statut Statut recherché (EN_ATTENTE, ENVOYEE, ECHEC, LUE).
     * @return Liste des notifications correspondantes.
     */
    List<NotificationEntity> findByStatutOrderByDateEnvoiDesc(StatutNotification statut);
}
