package com.sodepa.erp.ged.infrastructure.repo;

import com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Dépôt Spring Data JPA pour les documents de la GED SODEPA (REF-07).
 */
@Repository
public interface DocumentGedRepository extends JpaRepository<DocumentGedEntity, UUID> {

    List<DocumentGedEntity> findByTypeEntiteAndEntiteIdOrderByDateUploadDesc(String typeEntite, UUID entiteId);

    List<DocumentGedEntity> findByHashSha256(String hashSha256);

    List<DocumentGedEntity> findByTypeEntiteIgnoreCaseAndEntiteIdAndNomFichierIgnoreCaseOrderByNumeroVersionDesc(
            String typeEntite, UUID entiteId, String nomFichier);

    @Query("SELECT d FROM DocumentGedEntity d WHERE " +
           "(:typeEntite IS NULL OR LOWER(d.typeEntite) = LOWER(:typeEntite)) AND " +
           "(:categorie IS NULL OR LOWER(d.categorieDocumentaire) = LOWER(:categorie)) AND " +
           "(:motCle IS NULL OR LOWER(d.nomFichier) LIKE LOWER(CONCAT('%', :motCle, '%')) " +
           " OR LOWER(d.description) LIKE LOWER(CONCAT('%', :motCle, '%')) " +
           " OR LOWER(d.motsClesIndexation) LIKE LOWER(CONCAT('%', :motCle, '%')))")
    List<DocumentGedEntity> rechercherMulticritere(
            @Param("typeEntite") String typeEntite,
            @Param("categorie") String categorie,
            @Param("motCle") String motCle);
}
