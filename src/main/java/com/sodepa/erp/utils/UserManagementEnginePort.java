package com.sodepa.erp.utils;


import java.util.UUID;

public interface UserManagementEnginePort {
    UserOutput getUserById(UUID id);
}
