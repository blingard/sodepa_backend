package com.sodepa.erp.utils;


import com.sodepa.erp.share.AffectationPosteDto;
import com.sodepa.erp.share.PosteDto;

import java.util.Set;
import java.util.UUID;

/**
 * Composant applicatif `UserManagementEnginePort` du système ERP SODEPA.
 */
public interface UserManagementEnginePort {
    UserOutput getUserById(UUID id);
    Set<PosteDto> getUserAllActivePosteByUserId(UUID id);
    Set<PosteDto> getUserAllPostByUserId(UUID id);
}
