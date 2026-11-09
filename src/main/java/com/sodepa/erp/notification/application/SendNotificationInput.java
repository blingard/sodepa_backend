package com.sodepa.erp.notification.application;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;

import java.util.UUID;

public record SendNotificationInput(
        String title,
        String message,
        String recipientPhone,
        String recipientEmail,
        String recipientName,
        UUID recipientId,
        CanalNotification chanel,
        UUID workflowInstanceId,
        String workflowInstanceCodes
) {
}
