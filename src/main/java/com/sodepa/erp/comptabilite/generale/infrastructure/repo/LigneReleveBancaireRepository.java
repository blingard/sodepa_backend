package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneReleveBancaireEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneReleveBancaireRepository extends JpaRepository<LigneReleveBancaireEntity, UUID> {
    List<LigneReleveBancaireEntity> findByReleveId(UUID releveId);
    List<LigneReleveBancaireEntity> findByReleveIdAndRapproche(UUID releveId, Boolean rapproche);
}
