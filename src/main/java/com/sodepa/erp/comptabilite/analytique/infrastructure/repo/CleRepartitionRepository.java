package com.sodepa.erp.comptabilite.analytique.infrastructure.repo;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.CleRepartitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des clés de répartition analytique.
 */
@Repository
public interface CleRepartitionRepository extends JpaRepository<CleRepartitionEntity, UUID> {

    /**
     * Recherche une clé de répartition par son code unique.
     * 
     * @param code le code de la clé
     * @return un Optional contenant la clé s'il y a correspondance
     */
    Optional<CleRepartitionEntity> findByCode(String code);

    /**
     * Vérifie si une clé de répartition existe avec le code spécifié.
     * 
     * @param code le code à tester
     * @return true si elle existe, sinon false
     */
    boolean existsByCode(String code);
}
