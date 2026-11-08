package com.sodepa.erp.utils;

import com.sodepa.erp.organigramme.infrastructure.entities.AffectationPosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.DelegationPouvoirEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.DerogationPermissionEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutAffectation;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDelegation;
import com.sodepa.erp.organigramme.infrastructure.entities.StatutDerogation;
import com.sodepa.erp.organigramme.infrastructure.repo.AffectationPosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.DelegationPouvoirRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.DerogationPermissionRepository;
import com.sodepa.erp.share.FileStorageService;
import com.sodepa.erp.share.PosteDto;
import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Composant applicatif `UserManagementEngine` du système ERP SODEPA.
 */
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class UserManagementEngine implements UserManagementEnginePort {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AffectationPosteRepository affectationPosteRepository;
    private final DelegationPouvoirRepository delegationPouvoirRepository;
    private final DerogationPermissionRepository derogationPermissionRepository;

    private UtilisateurEntity getUserEntityById(UUID id) {
        UtilisateurEntity entity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (!entity.isActif()) {
            throw new RuntimeException("Current user is not active");
        }
        return entity;
    }
    @Override
    public UserOutput getUserById(UUID id) {
        UtilisateurEntity entity = getUserEntityById(id);

        Set<Permissions> effectivePermissions = resolveEffectivePermissions(entity);

        return UserOutput.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .email(entity.getEmail())
                .photoProfile(fileStorageService.getPresignedUrlFromFullUrl(entity.getPhotoProfile(), 1))
                .actif(entity.isActif())
                .telephones(entity.getTelephones())
                .permissions(effectivePermissions)
                .build();
    }



    @Override
    public Set<PosteDto> getUserAllActivePosteByUserId(UUID id) {
        UtilisateurEntity entity = getUserEntityById(id);
        return getAllActiveAffectations(entity)
                .stream()
                .map(aff -> PosteDto.fromEntity(aff.getPoste()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<PosteDto> getUserAllPostByUserId(UUID id) {
        UtilisateurEntity entity = getUserEntityById(id);
        return getAllAffectations(entity)
                .stream()
                .map(aff -> PosteDto.fromEntity(aff.getPoste()))
                .collect(Collectors.toSet());
    }

    private Set<AffectationPosteEntity> getAllAffectations(UtilisateurEntity entity) {
        return affectationPosteRepository.findByUtilisateurId(entity.getId())
                .stream().collect(Collectors.toSet());
    }

    private Set<AffectationPosteEntity> getAllActiveAffectations(UtilisateurEntity entity){
        final LocalDate today = LocalDate.now();
        return getAllAffectations(entity)
                .stream()
                .filter(affectationPosteEntity -> Objects.equals(affectationPosteEntity.getStatut(), StatutAffectation.ACTIF))
                .filter(aff -> {
                    boolean dateDebutOk = aff.getDateDebut() == null || !aff.getDateDebut().isAfter(today);
                    boolean dateFinOk = aff.getDateFin() == null || !aff.getDateFin().isBefore(today);
                    if (dateDebutOk && dateFinOk && aff.getPoste() != null && aff.getPoste().getPermissions() != null) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Résout dynamiquement les permissions d'un utilisateur SODEPA :
     * 1. Exceptions racines : admin_maker et admin_checker possèdent des permissions directes et ne sont pas des employés.
     * 2. Collaborateurs opérationnels :
     *    - Permissions du poste budgété actif (AffectationPosteEntity).
     *    - Permissions des délégations actives (DelegationPouvoirEntity).
     *    - Permissions dérogées exceptionnelles actives accordées par la DG (DerogationPermissionEntity).
     */
    private Set<Permissions> resolveEffectivePermissions(UtilisateurEntity entity) {
        if ("admin_maker".equalsIgnoreCase(entity.getUsername()) || "admin_checker".equalsIgnoreCase(entity.getUsername())) {
            return entity.getPermissions() != null ? entity.getPermissions() : Collections.emptySet();
        }

        Set<Permissions> permissions = new HashSet<>();
        Instant now = Instant.now();

        // 1. Affectations de postes actives
        Set<AffectationPosteEntity> affectations = getAllActiveAffectations(entity);
        for (AffectationPosteEntity aff : affectations) {
            permissions.addAll(aff.getPoste().getPermissions());
        }

        // 2. Délégations actives (en tant que délégataire)
        List<DelegationPouvoirEntity> delegations = delegationPouvoirRepository.findByDelegataireIdAndStatut(
                entity.getId(), StatutDelegation.ACTIVE);
        for (DelegationPouvoirEntity del : delegations) {
            boolean dateDebutOk = del.getDateDebut() == null || !del.getDateDebut().isAfter(now);
            boolean dateFinOk = del.getDateFin() == null || !del.getDateFin().isBefore(now);
            if (dateDebutOk && dateFinOk && del.getDelegant() != null) {
                List<AffectationPosteEntity> delegantAffs = affectationPosteRepository.findByUtilisateurIdAndStatut(
                        del.getDelegant().getId(), StatutAffectation.ACTIF);
                for (AffectationPosteEntity aff : delegantAffs) {
                    if (aff.getPoste() != null && aff.getPoste().getPermissions() != null) {
                        permissions.addAll(aff.getPoste().getPermissions());
                    }
                }
            }
        }

        // 3. Dérogations nominatives d'habilitations exceptionnelles actives
        List<DerogationPermissionEntity> derogations = derogationPermissionRepository.findByUtilisateurIdAndStatut(
                entity.getId(), StatutDerogation.ACTIVE);
        for (DerogationPermissionEntity der : derogations) {
            boolean dateDebutOk = der.getDateDebut() == null || !der.getDateDebut().isAfter(now);
            boolean dateFinOk = der.getDateFin() == null || !der.getDateFin().isBefore(now);
            if (dateDebutOk && dateFinOk && der.getPermission() != null) {
                permissions.add(der.getPermission());
            }
        }

        return permissions;
    }
}
