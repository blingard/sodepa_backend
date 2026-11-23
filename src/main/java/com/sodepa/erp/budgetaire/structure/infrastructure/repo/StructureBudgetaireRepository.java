package com.sodepa.erp.budgetaire.structure.infrastructure.repo;

import com.sodepa.erp.budgetaire.structure.infrastructure.entities.StructureBudgetaireEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Accès aux structures budgétaires. */
public interface StructureBudgetaireRepository extends JpaRepository<StructureBudgetaireEntity, UUID> {

    /** Vrai si l'unité est déjà déclarée, structure active ou désactivée. */
    boolean existsByUniteId(UUID uniteId);

    Page<StructureBudgetaireEntity> findAllByActifTrue(Pageable pageable);

    /** Structures actives : il y en a quelques dizaines au plus, lues d'un bloc pour les règles d'englobement. */
    List<StructureBudgetaireEntity> findAllByActifTrue();
}
