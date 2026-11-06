package com.sodepa.erp.share;

import com.sodepa.erp.utils.Permissions;
import lombok.Builder;

import java.util.Set;

@Builder
public record PosteData(
    String id,
    String name,
    String codePoste,
    Set<Permissions> permissions
) {

    public static PosteData fromDto(PosteDto posteDto) {
        return new PosteData(
                posteDto.id().toString(),
                posteDto.intitule(),
                posteDto.codePoste(),
                posteDto.permissions()
        );
    }
}
