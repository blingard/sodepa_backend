package com.sodepa.erp.comptabilite.generale.infrastructure.adapter.user;

import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public interface UserAdapterInterface {
    UserRecordSmartOutput getUserById(@NotNull UUID id);
}
