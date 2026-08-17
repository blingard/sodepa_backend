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
import java.util.Set;
import java.util.UUID;

/**
 * Seeder pour créer les utilisateurs super administrateurs par défaut au démarrage de l'application.
 * Crée les comptes 'admin_maker' et 'admin_checker' avec tous les droits associés, tant en base locale que dans Keycloak.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
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

        if (userRepository.findById(makerId).isEmpty()) {
            try {
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
                        .build();
                UUID iamId = createKeycloakUser(makerId, "admin_maker", "Admin", "Maker", "admin.maker@sodepa.com");
                adminMaker.setIam(iamId);
                userRepository.save(adminMaker);
                log.info("Utilisateur par défaut 'admin_maker' (super administrateur) créé avec succès en base de données.");
                clickHouseManager.saveMakerChecker(
                        MakerCheckerMessageInput.builder()
                                .id(UUID.randomUUID())
                                .entityName(MakerCheckerEntityName.USER.name())
                                .entityPk(makerId.toString())
                                .payload(objectMapper.writeValueAsString(adminMaker))
                                .timestamp(LocalDateTime.now())
                                .maker_id(origin.toString())
                                .checker_id(origin.toString())
                                .build()
                );
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        if (userRepository.findById(checkerId).isEmpty()) {
            try {
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
                        .build();

                UUID iam = createKeycloakUser(checkerId, "admin_checker", "Admin", "Checker", "admin.checker@sodepa.com");
                adminChecker.setIam(iam);
                userRepository.save(adminChecker);
                log.info("Utilisateur par défaut 'admin_checker' (super administrateur) créé avec succès en base de données.");
                clickHouseManager.saveMakerChecker(
                        MakerCheckerMessageInput.builder()
                                .id(UUID.randomUUID())
                                .entityName(MakerCheckerEntityName.USER.name())
                                .entityPk(makerId.toString())
                                .payload(objectMapper.writeValueAsString(adminChecker))
                                .timestamp(LocalDateTime.now())
                                .maker_id(origin.toString())
                                .checker_id(origin.toString())
                                .build()
                );
            }catch (Exception ex){
                ex.printStackTrace();
            }

        }
    }

    private UUID createKeycloakUser(UUID id, String username, String firstName, String lastName, String email) {
        try {
            log.info("Administrateur par défaut '{}' enregistré avec succès dans Keycloak.", username);
            return keycloakProvisioningPort.createKeycloakUser(id, username, email, firstName, lastName, true);
        } catch (Exception e) {
            log.error("Exception lors du provisionnement Keycloak pour '{}': ", username, e);
            throw new RuntimeException("Exception lors du provisionnement Keycloak pour : "+username);
        }
    }
}
