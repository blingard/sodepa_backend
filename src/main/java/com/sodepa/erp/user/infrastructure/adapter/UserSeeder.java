package com.sodepa.erp.user.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.infrastructure.repo.ClickHouseManager;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sodepa.erp.authentication.application.ports.KeycloakProvisioningPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Seeder officiel pour garantir la présence et l'activation des super-administrateurs système (bootstrap roots).
 * Initialise ou réactive les comptes 'admin_maker' et 'admin_checker' avec l'ensemble des permissions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final KeycloakProvisioningPort keycloakProvisioningPort;
    private final ClickHouseManager clickHouseManager;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        UUID origin = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID makerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID checkerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Set<Permissions> allPermissions = new HashSet<>(Arrays.asList(Permissions.values()));

        // 1. Initialiser ou réactiver admin_maker
        Optional<UtilisateurEntity> optMaker = userRepository.findById(makerId);
        if (optMaker.isEmpty()) {
            try {
                UUID iamId = createKeycloakUserSafe(makerId, "admin_maker", "Admin", "Maker", "admin.maker@sodepa.com");
                UtilisateurEntity adminMaker = UtilisateurEntity.builder()
                        .id(makerId)
                        .username("admin_maker")
                        .nom("Maker")
                        .prenom("Admin")
                        .email("admin.maker@sodepa.com")
                        .photoProfile("")
                        .actif(true)
                        .telephones(Set.of("+237600000001"))
                        .permissions(allPermissions)
                        .iam(iamId)
                        .build();
                userRepository.save(adminMaker);
                log.info("Super-administrateur 'admin_maker' créé avec succès en base locale.");
                auditClickHouse(adminMaker, makerId, origin);
            } catch (Exception ex) {
                log.error("Erreur lors de la création de admin_maker : {}", ex.getMessage());
            }
        } else {
            UtilisateurEntity maker = optMaker.get();
            if (!maker.isActif()) {
                maker.setActif(true);
                userRepository.save(maker);
                log.info("Compte 'admin_maker' réactivé.");
            }
        }

        // 2. Initialiser ou réactiver admin_checker
        Optional<UtilisateurEntity> optChecker = userRepository.findById(checkerId);
        if (optChecker.isEmpty()) {
            try {
                UUID iam = createKeycloakUserSafe(checkerId, "admin_checker", "Admin", "Checker", "admin.checker@sodepa.com");
                UtilisateurEntity adminChecker = UtilisateurEntity.builder()
                        .id(checkerId)
                        .username("admin_checker")
                        .nom("Checker")
                        .prenom("Admin")
                        .email("admin.checker@sodepa.com")
                        .photoProfile("")
                        .actif(true)
                        .telephones(Set.of("+237600000002"))
                        .permissions(allPermissions)
                        .iam(iam)
                        .build();

                userRepository.save(adminChecker);
                log.info("Super-administrateur 'admin_checker' créé avec succès en base locale.");
                auditClickHouse(adminChecker, checkerId, origin);
            } catch (Exception ex) {
                log.error("Erreur lors de la création de admin_checker : {}", ex.getMessage());
            }
        } else {
            UtilisateurEntity checker = optChecker.get();
            if (!checker.isActif()) {
                checker.setActif(true);
                userRepository.save(checker);
                log.info("Compte 'admin_checker' réactivé.");
            }
        }
    }

    private void auditClickHouse(UtilisateurEntity user, UUID userPk, UUID actorId) {
        try {
            clickHouseManager.saveMakerChecker(
                    MakerCheckerMessageInput.builder()
                            .id(UUID.randomUUID())
                            .entityName(MakerCheckerEntityName.USER.name())
                            .entityPk(userPk.toString())
                            .payload(objectMapper.writeValueAsString(user))
                            .timestamp(LocalDateTime.now())
                            .maker_id(actorId.toString())
                            .checker_id(actorId.toString())
                            .build()
            );
        } catch (Exception e) {
            log.debug("Audit ClickHouse ignoré pour {} : {}", user.getUsername(), e.getMessage());
        }
    }

    private UUID createKeycloakUserSafe(UUID id, String username, String firstName, String lastName, String email) {
        try {
            log.info("Provisionnement de l'administrateur '{}' dans Keycloak...", username);
            return keycloakProvisioningPort.createKeycloakUser(id, username, email, firstName, lastName, true);
        } catch (Exception e) {
            log.warn("Keycloak non joignable pour '{}' (repli sur ID local) : {}", username, e.getMessage());
            return id;
        }
    }
}
