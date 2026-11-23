package com.sodepa.erp.budgetaire.nature.infrastructure.repo;

import com.sodepa.erp.budgetaire.nature.infrastructure.entities.NatureBudgetaireEntity;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Accès aux natures budgétaires. */
public interface NatureBudgetaireRepository extends JpaRepository<NatureBudgetaireEntity, UUID> {

    /** Vrai si le code est déjà pris, par une nature active ou désactivée. */
    boolean existsByCode(String code);

    /** Natures filtrées ; un filtre absent ne restreint rien. */
    @Query("select n from NatureBudgetaireEntity n where (:section is null or n.section = :section)"
            + " and (:sens is null or n.sens = :sens) and (:inclureInactives = true or n.actif = true)")
    Page<NatureBudgetaireEntity> rechercher(@Param("section") SectionBudgetaire section,
                                            @Param("sens") SensBudgetaire sens,
                                            @Param("inclureInactives") boolean inclureInactives,
                                            Pageable pageable);
}
