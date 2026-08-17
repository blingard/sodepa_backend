package com.sodepa.erp.share;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodepa.erp.audit.application.inputs.MakerCheckerMessageInput;
import com.sodepa.erp.audit.application.usecase.AuditEventPublisher;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user.UserAdapterInterface;
import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import com.sodepa.erp.utils.*;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MakerCheckerEngineAdapter implements MakerCheckerEnginePort{

    private final MakerCheckerRequestJpaRepo requestRepo;
    private final UtilsService utilsService;
    private final MakerCheckerMapper mapper;
    private final AuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private UserAdapterInterface userAdapterInterface;

    @Value("${app.duration}")
    private Long days;

    @Override
    public void submitChange(MakerCheckerEntityName entityName, String entityPk, Map<String, Object> appliedPatch, MakerCheckerOperationType checkerOperationType) {
        UUID requestId = UUID.randomUUID();

        String makerId = utilsService.getCurrentUser().getUserData().get().userId();

        MakerCheckerRequestEntity request = MakerCheckerRequestEntity.builder()
                .id(requestId)
                .entityName(entityName)
                .entityPk(entityPk)
                .makerId(makerId)
                .status(MakerCheckerStatus.PENDING)
                .payload(appliedPatch)
                .checkerOperationType(checkerOperationType)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plus(days, ChronoUnit.DAYS))
                .build();

        request = requestRepo.save(request);
        // Publier l'événement de soumission vers ClickHouse via RabbitMQ
        publishMakerCheckerEvent(request, makerId, null);
    }

    @Override
    public void validateOrReject(UUID requestId, MakerCheckerStatus decision, String notes) {

    }

    @Override
    public PageRecord<MakerCheckerSmartOutput> findAllByPage(Pageable pageable) {
        return null;
    }

    @Override
    public PageRecord<MakerCheckerSmartOutput> findAllByStatusAndByPage(Pageable pageable, MakerCheckerStatus status) {
        return null;
    }

    @Override
    public PageRecord<MakerCheckerSmartOutput> findAllByEntityNameAndByPage(Pageable pageable, MakerCheckerEntityName status) {
        return null;
    }

    @Override
    public MakerCheckerOutput findById(UUID id) {
        MakerCheckerRequestEntity entity = requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Data not found."));
        return this.mapping(entity);
    }

    @Override
    public MakerCheckerOutput findByEntityIdAndEntityName(UUID id, MakerCheckerEntityName entityName) {
        return null;
    }

    @Override
    @Transactional
    public void update(UUID id, MakerCheckerStatus status, String note, String checherId) {
        MakerCheckerRequestEntity entity = requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Data not found."));
        entity.setStatus(status);
        entity.setNotes(note);
        entity.setDecidedAt(Instant.now());
        entity.setCheckerId(checherId);
        entity = requestRepo.save(entity);

        // Publier l'événement de décision vers ClickHouse via RabbitMQ
        publishMakerCheckerEvent(entity, entity.getMakerId(), checherId);
    }

    @Override
    public void validate(UUID id, Instant createdAt, Instant expireAt) {
        Instant now = Instant.now();
        try {
            if (createdAt == null || expireAt == null) {
                throw new RuntimeException("Request creation or expiration date is missing");
            }

            if (now.isBefore(createdAt)) {
                throw new RuntimeException("Request is not yet valid: current date is before the creation date");
            }

            if (now.isAfter(expireAt)) {
                throw new RuntimeException("Request has expired: current date is after the expiration date");
            }
        }catch (Exception ex){
            update(id, MakerCheckerStatus.EXPIRED, ex.getMessage(), "SYSTEM");
            throw new RuntimeException(ex.getMessage());
        }
    }

    private MakerCheckerOutput mapping(MakerCheckerRequestEntity entity){
        UserRecordSmartOutput maker = userAdapterInterface.getUserById(UUID.fromString(entity.getMakerId()));
        if(maker == null && entity.getMakerId() != "SYSTEM")
            throw new RuntimeException("Maker cannot be null");
        if(entity.getMakerId() == "SYSTEM"){
            maker = UserRecordSmartOutput.builder()
                    .id(UUID.randomUUID())
                    .name("SYSTEM")
                    .image(null)
                    .status(true)
                    .build();
        }
        UserRecordSmartOutput checker = userAdapterInterface.getUserById(UUID.fromString(entity.getMakerId()));

        if(entity.getCheckerId() == "SYSTEM"){
            checker = UserRecordSmartOutput.builder()
                    .id(UUID.randomUUID())
                    .name("SYSTEM")
                    .image(null)
                    .status(true)
                    .build();
        }
        return MakerCheckerOutput.builder()
                .id(entity.getId())
                .entityName(entity.getEntityName())
                .entityPk(entity.getEntityPk())
                .maker(maker)
                .checker(checker)
                .status(entity.getStatus())
                .payload(entity.getPayload())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .decidedAt(entity.getDecidedAt())
                .expiredAt(entity.getExpiredAt())
                .checkerOperationType(entity.getCheckerOperationType())
                .build();
    }

    private void publishMakerCheckerEvent(MakerCheckerRequestEntity makerCheckerRequest, @NotBlank String maker_Id, String checker_Id) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String payloadJson = objectMapper.writeValueAsString(makerCheckerRequest);
                MakerCheckerMessageInput msg = MakerCheckerMessageInput.builder()
                        .id(UUID.randomUUID())
                        .entityName(makerCheckerRequest.getEntityName().name())
                        .entityPk(makerCheckerRequest.getEntityPk())
                        .payload(payloadJson)
                        .timestamp(LocalDateTime.now())
                        .maker_id(maker_Id)
                        .checker_id(checker_Id)
                        .build();
                auditEventPublisher.publishMakerChecker(msg);
            } catch (JsonProcessingException e) {
                log.warn("Échec de sérialisation du payload MakerChecker pour ClickHouse : {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Échec de publication de l'événement MakerChecker vers RabbitMQ : {}", e.getMessage());
            }
        });
    }

}
