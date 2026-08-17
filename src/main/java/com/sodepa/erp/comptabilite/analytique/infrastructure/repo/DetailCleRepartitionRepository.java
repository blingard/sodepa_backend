package com.sodepa.erp.comptabilite.analytique.infrastructure.repo;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.DetailCleRepartitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des détails des clés de répartition analytique.
 */
@Repository
public interface DetailCleRepartitionRepository extends JpaRepository<DetailCleRepartitionEntity, UUID> {

    /**
     * Récupère l'ensemble des détails associés à une clé de répartition spécifique.
     * 
     * @param cleId l'identifiant unique de la clé de répartition
     * @return la liste des détails associés
     */
    List<DetailCleRepartitionEntity> findByCleId(UUID cleId);
}
