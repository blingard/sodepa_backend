package com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user;

import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Composant applicatif `UserAdapterInterface` du système ERP SODEPA.
 */
public interface UserAdapterInterface {
    UserRecordSmartOutput getUserById(@NotNull UUID id);
}
