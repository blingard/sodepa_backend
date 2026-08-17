package com.sodepa.erp.user.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.user.application.inputs.*;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.application.usecase.*;
import com.sodepa.erp.user.presentation.requests.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Contrôleur REST pour les utilisateurs.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRestController {

    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final ChangePhotoUseCase changePhotoUseCase;
    private final UpdateUserPermissionsUseCase updateUserPermissionsUseCase;
    private final UserValidateOrRejectUseCase userValidateOrRejectUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetPageUsersUseCase getPageUsersUseCase;
    private final SearchUsersUseCase searchUsersUseCase;

    @PostMapping("/init_create")
    public ResponseEntity<Void> initCreate(
            @Valid @RequestPart("request") CreateUserRequest request,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {
        CreateUserInput input = new CreateUserInput(
                request.username(),
                request.nom(),
                request.prenom(),
                request.email(),
                request.telephones(),
                request.permissions(),
                file
        );
        createUserUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/init_update/{id}")
    public ResponseEntity<Void> initUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UpdateUserInput input = new UpdateUserInput(
                id,
                request.nom(),
                request.prenom(),
                request.email(),
                request.telephones(),
                request.actif()
        );
        updateUserUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/init_change_photo/{id}")
    public ResponseEntity<Void> initChangePhoto(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        ChangePhotoInput input = new ChangePhotoInput(id, file);
        changePhotoUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/init_update_permissions/{id}")
    public ResponseEntity<Void> initUpdatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionsRequest request
    ) {
        UpdatePermissionsInput input = new UpdatePermissionsInput(id, request.permissions());
        updateUserPermissionsUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/validate_or_reject/{id}")
    public ResponseEntity<Void> validateOrReject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOrValidateUserSubmitRequest request
    ) {
        ValidateOrRejectSubmissionInput input = new ValidateOrRejectSubmissionInput(
                id,
                request.decision(),
                request.notes(),
                request.checkerOperationType()
        );
        userValidateOrRejectUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserOutput> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(getUserByIdUseCase.execute(id));
    }

    @GetMapping
    public ResponseEntity<PageRecord<UserOutput>> getPage(Pageable pageable) {
        return ResponseEntity.ok(getPageUsersUseCase.execute(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<PageRecord<UserOutput>> search(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telephone,
            Pageable pageable
    ) {
        SearchUsersInput input = new SearchUsersInput(nom, prenom, email, telephone, pageable);
        return ResponseEntity.ok(searchUsersUseCase.execute(input));
    }
}
