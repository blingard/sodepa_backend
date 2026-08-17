package com.sodepa.erp.comptabilite.analytique.infrastructure.repo;

import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface de persistance pour l'accès aux données des budgets prévisionnels.
 */
@Repository
public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {

    /**
     * Recherche une ligne budgétaire pour un exercice, une section et un compte général.
     * 
     * @param annee l'exercice budgétaire
     * @param sectionId l'identifiant unique de la section
     * @param compteCode le code du compte général
     * @return un Optional contenant le budget s'il existe
     */
    Optional<BudgetEntity> findByAnneeAndSectionIdAndCompteCode(int annee, UUID sectionId, String compteCode);

    /**
     * Récupère l'ensemble des lignes budgétaires pour un exercice donné.
     * 
     * @param annee l'exercice comptable (ex: 2026)
     * @return la liste des budgets de l'année
     */
    List<BudgetEntity> findByAnnee(int annee);

    /**
     * Récupère les lignes budgétaires d'une section analytique spécifique pour un exercice donné.
     * 
     * @param annee l'exercice comptable
     * @param sectionId l'identifiant de la section
     * @return la liste des budgets associés
     */
    List<BudgetEntity> findByAnneeAndSectionId(int annee, UUID sectionId);
}
