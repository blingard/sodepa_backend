package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.comptabilite.generale.application.inputs.CreateCompteInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.CompteRepository;
import com.sodepa.erp.share.CurrentUserAuthenticationToken;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

/**
 * Seeder pour le plan comptable SYSCOHADA.
 * Initialise le plan comptable en passant par les adaptateurs et le processus Maker-Checker.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class CompteSeeder implements CommandLineRunner {

    private final CompteRepository compteRepository;
    private final CompteAdapter compteAdapter;
    private final MakerCheckerRequestJpaRepo makerCheckerRequestJpaRepo;

    @Override
    public void run(String... args) {
        if (compteRepository.count() > 0) {
            log.info("Le plan comptable est déjà initialisé dans la base de données.");
            return;
        }
        UUID originSession = UUID.fromString("00000000-0000-0000-0000-000000000000");


        log.info("Initialisation du plan comptable SYSCOHADA depuis le fichier JSON via les endpoints (use cases)...");
        ClassPathResource resource = new ClassPathResource("planComptableOHADA.json");
        if (!resource.exists()) {
            log.warn("Fichier planComptableOHADA.json introuvable dans le classpath. Initialisation annulée.");
            return;
        }

        // 1. Authentifier en tant que 'admin_maker'
        UUID makerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Set<Permissions> allPermissions = new HashSet<>(Arrays.asList(Permissions.values()));
        UserData makerUserData = UserData.builder()
                .username("admin_maker")
                .userId(makerId.toString())
                .permissions(allPermissions)
                .sessionId(originSession.toString())
                .build();
        CurrentUserAuthenticationToken makerAuth = new CurrentUserAuthenticationToken(
                "admin_maker", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), makerUserData
        );
        SecurityContextHolder.getContext().setAuthentication(makerAuth);

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode rootNode = new ObjectMapper().readTree(inputStream);
            List<CreateCompteInput> inputsToSubmit = new ArrayList<>();

            if (rootNode.isArray()) {
                for (JsonNode classNode : rootNode) {
                    // Niveau 1 : Classe
                    String classCode = classNode.get("code").asText();
                    String classIntitule = classNode.get("intitule").asText();
                    String nature = determineNatureByClass(classCode);

                    inputsToSubmit.add(CreateCompteInput.builder()
                            .code(classCode)
                            .intitule(classIntitule)
                            .parentCode(null)
                            .niveau(1)
                            .nature(nature)
                            .isAuxiliaire(false)
                            .build());

                    // Niveau 2 : Comptes principaux
                    JsonNode accountsNode = classNode.get("comptes");
                    if (accountsNode != null && accountsNode.isArray()) {
                        for (JsonNode accountNode : accountsNode) {
                            String accountCode = accountNode.get("code").asText();
                            String accountIntitule = accountNode.get("intitule").asText();

                            inputsToSubmit.add(CreateCompteInput.builder()
                                    .code(accountCode)
                                    .intitule(accountIntitule)
                                    .parentCode(classCode)
                                    .niveau(2)
                                    .nature(nature)
                                    .isAuxiliaire(false)
                                    .build());

                            // Niveau 3 : Sous-comptes
                            JsonNode subAccountsNode = accountNode.get("sousComptes");
                            if (subAccountsNode != null && subAccountsNode.isArray()) {
                                for (JsonNode subAccountNode : subAccountsNode) {
                                    String subAccountCode = subAccountNode.get("code").asText();
                                    String subAccountIntitule = subAccountNode.get("intitule").asText();

                                    inputsToSubmit.add(CreateCompteInput.builder()
                                            .code(subAccountCode)
                                            .intitule(subAccountIntitule)
                                            .parentCode(accountCode)
                                            .niveau(3)
                                            .nature(nature)
                                            .isAuxiliaire(false)
                                            .build());
                                }
                            }
                        }
                    }
                }
            }

            // Soumettre toutes les demandes de création
            for (CreateCompteInput input : inputsToSubmit) {
                compteAdapter.initCreateCompte(input);
            }

            log.info("{} demandes de création de comptes soumises par admin_maker.", inputsToSubmit.size());

            // 2. Authentifier en tant que 'admin_checker'
            UUID checkerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            UserData checkerUserData = UserData.builder()
                    .username("admin_checker")
                    .userId(checkerId.toString())
                    .permissions(allPermissions)
                    .sessionId(originSession.toString())
                    .build();
            CurrentUserAuthenticationToken checkerAuth = new CurrentUserAuthenticationToken(
                    "admin_checker", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), checkerUserData
            );
            SecurityContextHolder.getContext().setAuthentication(checkerAuth);

            // Valider toutes les demandes de création de comptes en attente
            List<MakerCheckerRequestEntity> pendingRequests = makerCheckerRequestJpaRepo.findAll().stream()
                    .filter(r -> r.getEntityName() == MakerCheckerEntityName.COMPTE && r.getStatus() == MakerCheckerStatus.PENDING)
                    .toList();

            for (MakerCheckerRequestEntity req : pendingRequests) {
                ValidateOrRejectSubmissionInput validationInput = new ValidateOrRejectSubmissionInput(
                        req.getId(), MakerCheckerStatus.ACCEPTED, "Approbation automatique lors de l'initialisation du plan comptable.", req.getCheckerOperationType()
                );
                compteAdapter.validateOrReject(validationInput);
            }

            log.info("Plan comptable SYSCOHADA initialisé avec succès : {} comptes validés et enregistrés.", pendingRequests.size());
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du plan comptable : ", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String determineNatureByClass(String classCode) {
        return switch (classCode) {
            case "1" -> "CAPITAUX_PROPRES_RESSOURCES_DURABLES";
            case "2" -> "ACTIF_IMMOBILISE";
            case "3" -> "STOCKS";
            case "4" -> "TIERS";
            case "5" -> "TRESORERIE";
            case "6" -> "CHARGE";
            case "7" -> "PRODUIT";
            case "8" -> "HAO";
            case "9" -> "ANALYTIQUE_ENGAGEMENTS";
            default -> "INCONNUE";
        };
    }
}
