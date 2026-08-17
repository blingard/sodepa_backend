package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import org.springframework.context.ApplicationEvent;

/**
 * Événement publié après la validation et le verrouillage définitif d'une écriture comptable.
 * Permet de notifier d'autres modules (comme le module budget) pour le rapprochement réel/prévisionnel.
 */
public class EcritureValidatedEvent extends ApplicationEvent {

    /**
     * L'écriture comptable validée.
     */
    private final EcritureEntity ecriture;

    /**
     * Constructeur de l'événement.
     * 
     * @param source la source à l'origine de l'événement
     * @param ecriture l'écriture comptable validée
     */
    public EcritureValidatedEvent(Object source, EcritureEntity ecriture) {
        super(source);
        this.ecriture = ecriture;
    }

    /**
     * Récupère l'écriture comptable validée associée à cet événement.
     * 
     * @return l'écriture comptable validée
     */
    public EcritureEntity getEcriture() {
        return ecriture;
    }
}
