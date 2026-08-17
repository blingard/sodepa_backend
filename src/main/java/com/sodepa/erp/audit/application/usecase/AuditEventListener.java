package com.sodepa.erp.audit.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.usecase.EcritureValidatedEvent;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneEcritureEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Écouteur transactionnel Spring qui intercepte la validation réussie d'écritures comptables.
 * Ne s'exécute qu'après le commit de la transaction PostgreSQL pour éviter d'insérer des données annulées dans ClickHouse.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditEventPublisher auditEventPublisher;

    /**
     * Gère l'événement de validation d'écriture comptable après commit réussi de la transaction principale.
     * 
     * @param event l'événement contenant l'écriture validée
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEcritureValidated(EcritureValidatedEvent event) {
        EcritureEntity ecriture = event.getEcriture();
        if (ecriture == null || ecriture.getLignes() == null) {
            return;
        }

        // Pour chaque ligne de l'écriture comptable, on envoie un message individuel vers RabbitMQ
        for (LigneEcritureEntity ligne : ecriture.getLignes()) {
            UUID sectionId = null;
            if (ligne.getVentilations() != null && !ligne.getVentilations().isEmpty()) {
                if (ligne.getVentilations().get(0).getSection() != null) {
                    sectionId = ligne.getVentilations().get(0).getSection().getId();
                }
            }

            AuditEventPublisher.TransactionMessage msg = new AuditEventPublisher.TransactionMessage(
                    ligne.getId(),
                    ligne.getCompteCode(),
                    ligne.getDebit(),
                    ligne.getCredit(),
                    ligne.getLibelleLigne() != null ? ligne.getLibelleLigne() : ecriture.getLibelle(),
                    ecriture.getDateComptable(),
                    sectionId
            );

            auditEventPublisher.publishTransaction(msg);
        }
    }
}
