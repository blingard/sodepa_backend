package com.sodepa.erp.tresorerie.regies.infrastructure.repo;

import com.sodepa.erp.tresorerie.regies.infrastructure.entities.TresorerieRegieDepenseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TresorerieRegieDepenseRepository extends JpaRepository<TresorerieRegieDepenseEntity, UUID> {
    Optional<TresorerieRegieDepenseEntity> findByNumeroPiece(String numeroPiece);
    List<TresorerieRegieDepenseEntity> findByRegieIdAndJustifieFalse(UUID regieId);
    Page<TresorerieRegieDepenseEntity> findByRegieId(UUID regieId, Pageable pageable);
}
