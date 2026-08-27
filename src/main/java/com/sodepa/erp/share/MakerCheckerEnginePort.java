package com.sodepa.erp.share;


import com.sodepa.erp.utils.*;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound application port used to communicate with the external
 * Maker-Checker engine.
 *
 * <p>
 * This port abstracts the submission and approval workflow for changes
 * requiring validation by one or more checkers.
 * </p>
 *
 * <p>
 * The payload map supplied to the methods must already conform to the
 * schema expected by the Maker-Checker engine. The conversion from
 * DTO/domain objects into a {@code Map<String, Object>} is performed
 * outside this interface by application-layer mapper components.
 * </p>
 */
public interface MakerCheckerEnginePort {

    /**
     * Submits a persisted entity change to the Maker-Checker engine.
     *
     * <p>
     * This method is typically invoked after a domain entity has been
     * successfully persisted.
     * </p>
     *
     * @param entityName logical name of the domain entity
     *                   (example: {@code "CardRange"})
     * @param entityPk primary key of the persisted entity
     *                 represented as a String UUID
     * @param appliedPatch map describing the change that must be reviewed
     *                     by a checker
     *
     * @return generated request identifier returned by the
     *         Maker-Checker engine
     */
    UUID submitChange(MakerCheckerEntityName entityName, String entityPk, Map<String, Object> appliedPatch,
                      MakerCheckerOperationType checkerOperationType);

    void validateOrReject(UUID requestId, MakerCheckerStatus decision, String notes);

    PageRecord<MakerCheckerSmartOutput> findAllByPage(Pageable pageable);
    PageRecord<MakerCheckerSmartOutput> findAllByStatusAndByPage(Pageable pageable, MakerCheckerStatus status);
    PageRecord<MakerCheckerSmartOutput> findAllByEntityNameAndByPage(Pageable pageable, MakerCheckerEntityName status);

    /**
     * Demandes portant sur un domaine donné et dans un état donné.
     *
     * <p>
     * C'est la requête du checker : « que dois-je trancher, et sur quoi ? ».
     * Sans elle, l'identifiant tiré au hasard à la soumission n'est
     * récupérable nulle part et {@code validate_or_reject} reste inappelable.
     * </p>
     */
    PageRecord<MakerCheckerSmartOutput> findAllByEntityNameAndStatusAndByPage(
            Pageable pageable, MakerCheckerEntityName entityName, MakerCheckerStatus status);

    /**
     * Comme ci-dessus, mais en écartant les demandes soumises par {@code makerId}.
     *
     * <p>
     * C'est la file réellement actionnable par un checker : la séparation
     * maker-checker lui interdit de trancher ses propres soumissions, et
     * {@code validateOrReject} les refuse. Les lister quand même produisait un
     * bouton qui ne pouvait que renvoyer une erreur.
     * </p>
     */
    PageRecord<MakerCheckerSmartOutput> findAllAValiderParAutrui(
            Pageable pageable, MakerCheckerEntityName entityName,
            MakerCheckerStatus status, String makerId);
    MakerCheckerOutput findById(UUID id);
    MakerCheckerOutput findByEntityIdAndEntityName(UUID id, MakerCheckerEntityName entityName);

    void update(UUID id, MakerCheckerStatus status, String note, String checherId);
    void validate(UUID id, Instant createdAt, Instant expireAt);

}
