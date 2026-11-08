package com.sodepa.erp.pastoral.infrastructure.repo;

import com.sodepa.erp.pastoral.infrastructure.entities.CatalogueProduitAnimalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA assurant les opérations de persistance et d'interrogation pour catalogue produit animal (PASTORAL).
 */
@Repository
public interface CatalogueProduitAnimalRepository extends JpaRepository<CatalogueProduitAnimalEntity, UUID> {
    List<CatalogueProduitAnimalEntity> findByActifTrueOrderByCodeAsc();
    Optional<CatalogueProduitAnimalEntity> findByCodeIgnoreCase(String code);
    Optional<CatalogueProduitAnimalEntity> findByDesignationIgnoreCase(String designation);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByDesignationIgnoreCase(String designation);
}
