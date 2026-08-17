package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.GetTvaDeclarationInput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetTvaDeclarationUseCase implements UseCase<GetTvaDeclarationInput, ReportingUseCase.TvaDeclaration> {

    private final ReportingUseCase reportingUseCase;

    @Override
    public ReportingUseCase.TvaDeclaration execute(GetTvaDeclarationInput input) {
        return reportingUseCase.genererDeclarationTva(input.annee(), input.mois());
    }
}
