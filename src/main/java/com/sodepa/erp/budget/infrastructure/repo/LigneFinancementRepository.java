package com.sodepa.erp.budget.infrastructure.repo;

import com.sodepa.erp.budget.infrastructure.entities.LigneFinancementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux lignes de financement (prêts, obligataires, leasings).
 */
@Repository
public interface LigneFinancementRepository extends JpaRepository<LigneFinancementEntity, UUID> {

    /**
     * Recherche les financements contractés auprès d'une banque donnée.
     * 
     * @param banqueId l'identifiant unique de la banque
     * @return la liste des lignes de financement
     */
    List<LigneFinancementEntity> findByBanqueId(UUID banqueId);

    /**
     * Recherche les financements par type (ex: PRET, LEASING, OBLIGATION).
     * 
     * @param type le type de financement recherché
     * @return la liste des lignes de ce type
     */
    List<LigneFinancementEntity> findByType(String type);

    /**
     * Financements, filtrés au besoin sur le prêteur et la nature.
     *
     * <p>
     * Les deux critères sont facultatifs : un paramètre {@code null} ne
     * restreint rien.
     * </p>
     */
    @Query("""
            SELECT f FROM LigneFinancementEntity f
            WHERE (:banqueId IS NULL OR f.banqueId = :banqueId)
              AND (:type IS NULL OR f.type = :type)
            """)
    Page<LigneFinancementEntity> rechercher(
            @Param("banqueId") UUID banqueId,
            @Param("type") String type,
            Pageable pageable);

    /**
     * Financement et son échéancier en une seule requête.
     *
     * <p>
     * Sans la jointure, chaque échéance déclenchait sa propre requête au moment
     * du mappage de la fiche.
     * </p>
     */
    @Query("SELECT f FROM LigneFinancementEntity f LEFT JOIN FETCH f.echeances WHERE f.id = :id")
    Optional<LigneFinancementEntity> findByIdAvecEcheances(@Param("id") UUID id);
}
