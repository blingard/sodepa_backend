package com.sodepa.erp.configuration.parametres.infrastructure.repo;

import com.sodepa.erp.configuration.parametres.infrastructure.entities.ParametreSystemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `ParametreSystemeRepository`.
 */
@Repository
public interface ParametreSystemeRepository extends JpaRepository<ParametreSystemeEntity, String> {

    List<ParametreSystemeEntity> findByCategorie(String categorie);
}
