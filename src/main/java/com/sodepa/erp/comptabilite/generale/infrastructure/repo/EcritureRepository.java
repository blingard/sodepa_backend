package com.sodepa.erp.comptabilite.generale.infrastructure.repo;

import com.sodepa.erp.comptabilite.generale.infrastructure.entities.EcritureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EcritureRepository extends JpaRepository<EcritureEntity, UUID> {
    List<EcritureEntity> findByDateComptableBetween(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT e FROM EcritureEntity e JOIN FETCH e.lignes WHERE e.dateComptable BETWEEN :start AND :end")
    List<EcritureEntity> findByDateComptableBetweenWithLignes(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
