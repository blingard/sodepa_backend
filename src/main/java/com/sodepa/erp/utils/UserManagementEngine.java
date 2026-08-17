package com.sodepa.erp.utils;

import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserManagementEngine implements UserManagementEnginePort{

    private final UserRepository userRepository;


    @Override
    public UserOutput  getUserById(UUID id) {
        UtilisateurEntity entity = userRepository.findById(id).orElseThrow(()->new RuntimeException("User not found"));
        if(!entity.isActif()) throw new RuntimeException("Current user is not active");
        return UserOutput.builder().id(entity.getId()).username(entity.getUsername()).nom(entity.getNom())
                .prenom(entity.getPrenom()).email(entity.getEmail()).photoProfile(entity.getPhotoProfile())
                .actif(entity.isActif()).telephones(entity.getTelephones()).permissions(entity.getPermissions()).build();
    }
}
