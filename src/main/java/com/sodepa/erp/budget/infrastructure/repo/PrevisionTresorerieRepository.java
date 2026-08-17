package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.PrevisionTresorerieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux prévisions de trésorerie.
 */
@Repository
public interface PrevisionTresorerieRepository extends JpaRepository<PrevisionTresorerieEntity, UUID> {

    /**
     * Recherche les prévisions de trésorerie sur un intervalle de dates d'échéance.
     * 
     * @param debut la date de début de l'intervalle
     * @param fin la date de fin de l'intervalle
     * @return la liste des prévisions de trésorerie correspondantes
     */
    List<PrevisionTresorerieEntity> findByDateEcheanceBetween(LocalDate debut, LocalDate fin);
}
