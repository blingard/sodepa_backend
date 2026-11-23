package com.sodepa.erp.workflow.application.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.organigramme.infrastructure.entities.PosteEntity;
import com.sodepa.erp.organigramme.infrastructure.entities.UniteOrganisationnelleEntity;
import com.sodepa.erp.organigramme.infrastructure.repo.PosteRepository;
import com.sodepa.erp.organigramme.infrastructure.repo.UniteOrganisationnelleRepository;
import com.sodepa.erp.rh.infrastructure.entities.EmployeEntity;
import com.sodepa.erp.rh.infrastructure.entities.GrilleSalarialeEntity;
import com.sodepa.erp.rh.infrastructure.entities.StatutActivite;
import com.sodepa.erp.rh.infrastructure.repo.EmployeRepository;
import com.sodepa.erp.rh.infrastructure.repo.GrilleSalarialeRepository;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.CodeGenerator;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowInstanceEntity;
import com.sodepa.erp.workflow.infrastructure.entities.WorkflowTaskEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Handler spécialisé de gestion du cycle de vie du workflow pour l'entité {@link EmployeEntity}.
 * <p>
 * Prend en charge les circuits de validation RH relatifs aux collaborateurs SODEPA :
 * <ul>
 *   <li>Recrutement et embauche d'un nouvel agent (validation hiérarchique, DAF, DG)</li>
 *   <li>Confirmation ou fin de période d'essai</li>
 *   <li>Mutation, changement de poste ou reclassement indiciaire</li>
 *   <li>Suspension ou radiation administrative</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class EmployeWorkflowEntityHandler implements WorkflowEntityHandler {

    private final EmployeRepository employeRepository;
    private final UserRepository userRepository;
    private final PosteRepository posteRepository;
    private final UniteOrganisationnelleRepository uniteOrganisationnelleRepository;
    private final GrilleSalarialeRepository grilleSalarialeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Noms d'entités supportés pour le routage du handler Employé.
     */
    private static final String ENTITY_KEY_SHORT = "EMPLOYE";
    private static final String ENTITY_KEY_RH = "RH_EMPLOYE";
    private static final String ENTITY_KEY_CLASS = EmployeEntity.class.getName();
    private static final String ENTITY_KEY_SIMPLE = EmployeEntity.class.getSimpleName();

    @Override
    public boolean supports(String entityName) {
        if (entityName == null || entityName.isBlank()) {
            return false;
        }
        String clean = entityName.trim();
        return clean.equalsIgnoreCase(ENTITY_KEY_SHORT)
                || clean.equalsIgnoreCase(ENTITY_KEY_RH)
                || clean.equalsIgnoreCase(ENTITY_KEY_SIMPLE)
                || clean.equals(ENTITY_KEY_CLASS);
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private com.sodepa.erp.rh.application.RhService rhService;

    @Override
    @Transactional
    public void onStepApproved(WorkflowTaskEntity task, Map<String, Object> modifiedFields) {
        if (task == null || task.getWorkflowInstance() == null) {
            return;
        }
        // Aucune écriture ni mutation en table métier PostgreSQL pendant les étapes intermédiaires :
        // les champs modifiés (champsModifiesJson) et pièces jointes vivent dans ClickHouse / Outbox jusqu'à l'approbation finale.
        WorkflowInstanceEntity instance = task.getWorkflowInstance();
        String codeEtape = task.getEtapeDefinition() != null ? task.getEtapeDefinition().getCodeEtape() : "N/A";
        log.debug("[WORKFLOW-EMPLOYE] Étape '{}' approuvée pour instance {} ({}) — état historisé dans ClickHouse.",
                codeEtape, instance.getId(), instance.getEntityId());
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance) {
        onWorkflowCompleted(instance, null);
    }

    @Override
    @Transactional
    public void onWorkflowCompleted(WorkflowInstanceEntity instance, Map<String, Object> finalData) {
        if (instance == null) {
            return;
        }

        UUID employeId = parseEntityId(instance.getEntityId());
        if (employeId == null) {
            log.error("[WORKFLOW-EMPLOYE] Impossible de finaliser le workflow : identifiant d'employé nul");
            return;
        }

        EmployeEntity employe = employeRepository.findById(employeId).orElse(null);
        if (employe == null && finalData != null && !finalData.isEmpty()) {
            rhService.finaliserApprobationRecrutementDg(employeId, null, null, finalData);
            return;
        }
        if (employe == null) {
            log.error("[WORKFLOW-EMPLOYE] Employé introuvable pour la finalisation (ID: {})", employeId);
            return;
        }

        log.info("[WORKFLOW-EMPLOYE] Finalisation du circuit de validation pour l'employé {} ({})",
                employe.getNom() + " " + employe.getPrenom(), employe.getId());

        if (employe.getStatutActivite() == null || employe.getStatutActivite() == StatutActivite.SUSPENDU) {
            employe.setStatutActivite(StatutActivite.EN_FONCTION);
        }

        if (employe.getMatricule() == null || employe.getMatricule().isBlank() || employe.getMatricule().startsWith("TEMP-")) {
            long sequence = employeRepository.count();
            String prefix = employe.getCodePrefix() != null ? employe.getCodePrefix() : "EMP";
            String matriculeGenere = CodeGenerator.generate(prefix, sequence);
            employe.setMatricule(matriculeGenere);
            log.info("[WORKFLOW-EMPLOYE] Matricule généré pour l'employé {} : {}", employe.getId(), matriculeGenere);
        }

        if (employe.getUtilisateur() != null) {
            UtilisateurEntity user = employe.getUtilisateur();
            if (!user.isActif()) {
                user.setActif(true);
                userRepository.save(user);
                log.info("[WORKFLOW-EMPLOYE] Compte utilisateur associé activé : {}", user.getUsername());
            }
        }

        employeRepository.save(employe);
        log.info("[WORKFLOW-EMPLOYE] Employé {} validé et activé avec succès.", employe.getMatricule());
    }

    @Override
    @Transactional
    public void onWorkflowRejected(WorkflowInstanceEntity instance, String motif) {
        if (instance == null) {
            return;
        }
        // Zéro création et zéro altération/suspension d'un employé existant en PostgreSQL lors d'un rejet workflow
        log.warn("[WORKFLOW-EMPLOYE] Instance {} ({}) REJETEE sans mutation PostgreSQL. Motif: {}",
                instance.getId(), instance.getEntityId(), motif);
    }

    /**
     * Applique les mutations d'attributs autorisés sur l'entité Employé.
     */
    private void appliquerChampsModifies(EmployeEntity employe, Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) continue;

            try {
                switch (key) {
                    case "telephone" -> employe.setTelephone(String.valueOf(value));
                    case "email" -> employe.setEmail(String.valueOf(value));
                    case "adresse" -> employe.setAdresse(String.valueOf(value));
                    case "numeroCnps" -> employe.setNumeroCnps(String.valueOf(value));
                    case "compteBancaireRib" -> employe.setCompteBancaireRib(String.valueOf(value));
                    case "banqueNom" -> employe.setBanqueNom(String.valueOf(value));
                    case "nombreEnfantsACharge" -> {
                        if (value instanceof Number n) {
                            employe.setNombreEnfantsACharge(n.intValue());
                        } else {
                            employe.setNombreEnfantsACharge(Integer.parseInt(String.valueOf(value)));
                        }
                    }
                    case "dateEmbauche" -> employe.setDateEmbauche(LocalDate.parse(String.valueOf(value)));
                    case "dateFinEssai" -> employe.setDateFinEssai(LocalDate.parse(String.valueOf(value)));
                    case "dateFinContrat" -> employe.setDateFinContrat(LocalDate.parse(String.valueOf(value)));
                    case "statutActivite" -> employe.setStatutActivite(StatutActivite.valueOf(String.valueOf(value)));
                    case "posteId", "poste" -> {
                        UUID posteId = UUID.fromString(String.valueOf(value));
                        PosteEntity poste = posteRepository.findById(posteId).orElse(null);
                        if (poste != null) {
                            employe.setPoste(poste);
                        }
                    }
                    case "uniteOrganisationnelleId", "uniteOrganisationnelle" -> {
                        UUID uoId = UUID.fromString(String.valueOf(value));
                        UniteOrganisationnelleEntity uo = uniteOrganisationnelleRepository.findById(uoId).orElse(null);
                        if (uo != null) {
                            employe.setUniteOrganisationnelle(uo);
                        }
                    }
                    case "grilleSalarialeId", "grilleSalariale" -> {
                        UUID gsId = UUID.fromString(String.valueOf(value));
                        GrilleSalarialeEntity gs = grilleSalarialeRepository.findById(gsId).orElse(null);
                        if (gs != null) {
                            employe.setGrilleSalariale(gs);
                        }
                    }
                    default -> log.debug("[WORKFLOW-EMPLOYE] Champ ignoré pour mutation directe : {}", key);
                }
            } catch (Exception e) {
                log.warn("[WORKFLOW-EMPLOYE] Erreur lors de l'application du champ {} (valeur: {}) : {}", key, value, e.getMessage());
            }
        }
    }

    private UUID parseEntityId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(entityId.trim());
        } catch (IllegalArgumentException e) {
            log.error("[WORKFLOW-EMPLOYE] Échec de conversion de l'identifiant entité en UUID : {}", entityId);
            return null;
        }
    }
}
