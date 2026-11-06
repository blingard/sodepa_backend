package com.sodepa.erp.utils;


import com.sodepa.erp.share.AffectationPosteDto;

import java.util.Set;
import java.util.UUID;

public interface OrganisationUnitEnginePort {
    Set<AffectationPosteDto> getUserAllActiveAffectationByUserId(UUID id);
    Set<AffectationPosteDto> getUserAllAffectationByUserId(UUID id);
}
