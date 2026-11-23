package com.sodepa.erp.budgetaire.source.infrastructure.repo;

import com.sodepa.erp.budgetaire.source.infrastructure.entities.SourceFinancementEntity;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Accès aux sources de financement. */
public interface SourceFinancementRepository extends JpaRepository<SourceFinancementEntity, UUID> {

    /** Vrai si le code est déjà pris, par une source active ou désactivée. */
    boolean existsByCode(String code);

    /** Sources filtrées ; un filtre absent ne restreint rien. */
    @Query("select s from SourceFinancementEntity s where (:type is null or s.type = :type)"
            + " and (:inclureInactives = true or s.actif = true)")
    Page<SourceFinancementEntity> rechercher(@Param("type") TypeSourceFinancement type,
                                             @Param("inclureInactives") boolean inclureInactives,
                                             Pageable pageable);
}
