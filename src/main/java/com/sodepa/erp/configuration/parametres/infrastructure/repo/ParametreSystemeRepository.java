package com.sodepa.erp.configuration.parametres.infrastructure.repo;

import com.sodepa.erp.configuration.parametres.infrastructure.entities.ParametreSystemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParametreSystemeRepository extends JpaRepository<ParametreSystemeEntity, String> {

    List<ParametreSystemeEntity> findByCategorie(String categorie);
}
