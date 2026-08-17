package com.sodepa.erp.comptabilite.analytique.infrastructure.repo;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.VentilationAnalytiqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des ventilations analytiques des lignes d'écriture.
 */
@Repository
public interface VentilationAnalytiqueRepository extends JpaRepository<VentilationAnalytiqueEntity, UUID> {
    
    /**
     * Récupère toutes les ventilations analytiques associées à une ligne d'écriture comptable spécifique.
     * 
     * @param ligneEcritureId l'identifiant unique de la ligne d'écriture d'origine
     * @return la liste des ventilations de cette ligne
     */
    List<VentilationAnalytiqueEntity> findByLigneEcritureId(UUID ligneEcritureId);
}
