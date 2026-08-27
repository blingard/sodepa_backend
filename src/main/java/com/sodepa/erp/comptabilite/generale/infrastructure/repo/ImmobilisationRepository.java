package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ImmobilisationEntity;
import com.sodepa.erp.utils.StatutImmobilisation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImmobilisationRepository extends JpaRepository<ImmobilisationEntity, UUID> {
    Optional<ImmobilisationEntity> findByCode(String code);

    /**
     * Registre des immobilisations, filtré au besoin.
     *
     * <p>
     * Les deux critères sont facultatifs et se combinent : un paramètre
     * {@code null} ne restreint rien. Écrit en JPQL plutôt qu'en méthode
     * dérivée, dont il aurait fallu quatre variantes pour couvrir les mêmes
     * combinaisons.
     * </p>
     *
     * @param recherche fragment cherché dans le code ou la désignation, déjà en minuscules
     * @param statut état du bien, ou {@code null} pour tous
     */
    @Query("""
            SELECT i FROM ImmobilisationEntity i
            WHERE (:recherche IS NULL
                   OR LOWER(i.code) LIKE %:recherche%
                   OR LOWER(i.designation) LIKE %:recherche%)
              AND (:statut IS NULL OR i.statut = :statut)
            """)
    Page<ImmobilisationEntity> rechercher(
            @Param("recherche") String recherche,
            @Param("statut") StatutImmobilisation statut,
            Pageable pageable);
}
