package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.BanqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface d'accès aux données JPA pour l'entité BanqueEntity.
 */
@Repository
public interface BanqueRepository extends JpaRepository<BanqueEntity, UUID> {
    
    /**
     * Recherche une banque par son code unique.
     * @param code le code abrégé unique de la banque.
     * @return un optionnel contenant la banque trouvée.
     */
    Optional<BanqueEntity> findByCode(String code);

    /**
     * Recherche une banque par son nom de banque.
     * @param nom le nom de la banque.
     * @return un optionnel contenant la banque trouvée.
     */
    Optional<BanqueEntity> findByNom(String nom);

    /**
     * Recherche une banque active par son nom de banque.
     * @param id le nom de la banque.
     * @return un optionnel contenant la banque trouvée.
     */
    Optional<BanqueEntity> findByIdAndStatusIsTrue(UUID id);

    /**
     * Recherche toutes les banques active.
     * @return une liste contenant les banques trouvées.
     */
    List<BanqueEntity> findAllByStatusIsTrueOrderByNomAsc();


}
