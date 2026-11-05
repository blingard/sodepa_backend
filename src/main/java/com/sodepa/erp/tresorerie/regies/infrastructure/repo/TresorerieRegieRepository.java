package com.sodepa.erp.tresorerie.regies.infrastructure.repo;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TresorerieRegieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TresorerieRegieRepository extends JpaRepository<TresorerieRegieEntity, UUID> {
    Optional<TresorerieRegieEntity> findByCode(String code);
    List<TresorerieRegieEntity> findByActifTrue();
}
