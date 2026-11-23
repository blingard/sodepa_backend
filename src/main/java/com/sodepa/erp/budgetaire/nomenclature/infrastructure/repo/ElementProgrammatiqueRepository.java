package com.sodepa.erp.budgetaire.nomenclature.infrastructure.repo;

import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.ElementProgrammatiqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Accès aux éléments de la nomenclature budgétaire. */
public interface ElementProgrammatiqueRepository extends JpaRepository<ElementProgrammatiqueEntity, UUID> {

    /** Vrai si le code est déjà pris, par un élément actif ou désactivé. */
    boolean existsByCode(String code);

    /** Vrai si l'élément a encore au moins un enfant actif. */
    boolean existsByParentIdAndActifTrue(UUID parentId);
}
