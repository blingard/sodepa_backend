package com.sodepa.erp.comptabilite.analytique.infrastructure.repo;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.SectionAnalytiqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des sections analytiques.
 */
@Repository
public interface SectionAnalytiqueRepository extends JpaRepository<SectionAnalytiqueEntity, UUID> {
    
    /**
     * Récupère l'ensemble des sections analytiques rattachées à un axe analytique parent.
     * 
     * @param axeId l'identifiant unique de l'axe parent
     * @return la liste des sections associées
     */
    List<SectionAnalytiqueEntity> findByAxeId(UUID axeId);

    /**
     * Recherche une section analytique par son code unique et l'ID de son axe parent.
     * 
     * @param axeId l'identifiant unique de l'axe parent
     * @param code le code abrégé de la section
     * @return un Optional contenant la section analytique si trouvée
     */
    Optional<SectionAnalytiqueEntity> findByAxeIdAndCode(UUID axeId, String code);
}
