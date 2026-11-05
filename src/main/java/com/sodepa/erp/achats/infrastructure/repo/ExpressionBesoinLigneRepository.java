package com.sodepa.erp.achats.infrastructure.repo;

import com.sodepa.erp.achats.infrastructure.entities.ExpressionBesoinLigneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpressionBesoinLigneRepository extends JpaRepository<ExpressionBesoinLigneEntity, UUID> {
    List<ExpressionBesoinLigneEntity> findByExpressionBesoinIdOrderByOrdreAsc(UUID ebId);
}
