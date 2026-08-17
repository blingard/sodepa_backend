package com.sodepa.erp.share;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapstruct mapper for Maker-Checker records.
 */
@Mapper(componentModel = "spring")
public interface MakerCheckerMapper {

    MakerCheckerSmartOutput toSmartDTO(MakerCheckerRequestEntity entity);

    @Mapping(target = "maker", ignore = true)
    @Mapping(target = "checker", ignore = true)
    MakerCheckerOutput toDTO(MakerCheckerRequestEntity entity);
}