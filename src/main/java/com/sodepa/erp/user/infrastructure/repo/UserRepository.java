package com.sodepa.erp.user.infrastructure.repo;

import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité UtilisateurEntity.
 */
public interface UserRepository extends JpaRepository<UtilisateurEntity, UUID> {

    Optional<UtilisateurEntity> findByUsername(String username);

    Optional<UtilisateurEntity> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM UtilisateurEntity u LEFT JOIN u.telephones t WHERE " +
           "(:nom IS NULL OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
           "(:prenom IS NULL OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :prenom, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:telephone IS NULL OR t LIKE CONCAT('%', :telephone, '%'))")
    Page<UtilisateurEntity> searchUsers(
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("email") String email,
            @Param("telephone") String telephone,
            Pageable pageable
    );
}
