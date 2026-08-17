package com.sodepa.erp.user.application.usecase;

import com.sodepa.erp.utils.UseCase;
import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cas d'utilisation pour valider ou rejeter la soumission concernant un utilisateur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidateOrRejectUseCase implements UseCase<ValidateOrRejectSubmissionInput, Void> {

    private final UserAdapter userAdapter;

    @Override
    public Void execute(ValidateOrRejectSubmissionInput input) {
        log.info("Validation ou rejet de la soumission de l'utilisateur {}", input.id());
        userAdapter.validateOrReject(input);
        return null;
    }
}
