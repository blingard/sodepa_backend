package com.sodepa.erp.comptabilite.analytique.infrastructure.repo;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.AxeAnalytiqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des axes analytiques.
 */
@Repository
public interface AxeAnalytiqueRepository extends JpaRepository<AxeAnalytiqueEntity, UUID> {
    
    /**
     * Recherche un axe analytique par son code unique.
     * 
     * @param code le code abrégé de l'axe analytique
     * @return un Optional contenant l'axe analytique s'il est trouvé
     */
    Optional<AxeAnalytiqueEntity> findByCode(String code);

    /**
     * Vérifie si un axe analytique existe avec le code spécifié.
     * 
     * @param code le code abrégé à tester
     * @return true si l'axe existe, sinon false
     */
    boolean existsByCode(String code);
}
