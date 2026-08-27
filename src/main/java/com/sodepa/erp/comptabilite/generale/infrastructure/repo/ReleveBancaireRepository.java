package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ReleveBancaireEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReleveBancaireRepository extends JpaRepository<ReleveBancaireEntity, UUID> {

    /**
     * Relevés importés, filtrés au besoin sur la banque et l'état de validation.
     *
     * <p>
     * Sans cette liste, un relevé n'était atteignable que par l'identifiant
     * rendu au moment de sa saisie — que le lettrage et le matching réclament
     * pourtant l'un et l'autre.
     * </p>
     */
    @Query("""
            SELECT r FROM ReleveBancaireEntity r
            WHERE (:banqueId IS NULL OR r.banque.id = :banqueId)
              AND (:valide IS NULL OR r.valide = :valide)
            """)
    Page<ReleveBancaireEntity> rechercher(
            @Param("banqueId") UUID banqueId,
            @Param("valide") Boolean valide,
            Pageable pageable);

    /**
     * Relevé et ses lignes en une seule requête.
     */
    @Query("SELECT r FROM ReleveBancaireEntity r LEFT JOIN FETCH r.lignes WHERE r.id = :id")
    Optional<ReleveBancaireEntity> findByIdAvecLignes(@Param("id") UUID id);
}
