package com.sodepa.erp.share;

import com.sodepa.erp.utils.Permissions;
import lombok.Builder;

import java.util.Set;

@Builder
public record UserData(
    String username,
    String name,
    String userId,
    Set<String> phoneNumbers,
    String email,
    Set<Permissions> permissions,
    String sessionId,
    String jwtToken
) {

}
