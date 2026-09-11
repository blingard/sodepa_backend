package com.sodepa.erp.user.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.user.application.inputs.*;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.application.usecase.*;
import com.sodepa.erp.user.presentation.requests.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    private final ChangeOwnPhotoUseCase changeOwnPhotoUseCase;
    private final UpdateUserPermissionsUseCase updateUserPermissionsUseCase;
    private final UserValidateOrRejectUseCase userValidateOrRejectUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final GetPageUsersUseCase getPageUsersUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final GetPendingUserRequestsUseCase getPendingUserRequestsUseCase;

    /**
     * Soumet la création d'un utilisateur.
     *
     * <p>
     * Ne crée rien : dépose une demande. La réponse porte les deux
     * identifiants dont le client a besoin ensuite — celui de la demande, à
     * passer à {@code validate_or_reject}, et celui qu'aura le compte si elle
     * est acceptée.
     * </p>
     */
    // `@ResponseStatus` est redondant avec `ResponseEntity.accepted()` pour
    // l'exécution, mais c'est lui que springdoc lit : sans lui, le contrat
    // publié annonce un 200 que le serveur ne renvoie jamais.
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/init_create")
    public ResponseEntity<SubmissionOutput> initCreate(
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
        return ResponseEntity.accepted().body(createUserUseCase.execute(input));
    }

    /**
     * Demandes d'utilisateur en attente de décision — la boîte de réception du
     * checker. Exige {@code VALIDATE_OR_REJECT_USER}.
     */
    @GetMapping("/pending")
    public ResponseEntity<PageRecord<MakerCheckerSmartOutput>> getPending(Pageable pageable) {
        return ResponseEntity.ok(getPendingUserRequestsUseCase.execute(pageable));
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

    /**
     * Permet à l'utilisateur connecté de changer sa propre photo de profil.
     * Pas de Maker-Checker requis — mise à jour directe.
     */
    @PutMapping("/change_photo")
    public ResponseEntity<Void> changePhoto(
            @RequestPart("file") MultipartFile file
    ) {
        ChangePhotoInput input = new ChangePhotoInput(null, file);
        changeOwnPhotoUseCase.execute(input);
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

    @GetMapping("/profile")
    public ResponseEntity<UserOutput> getUserProfile() {
        return ResponseEntity.ok(getUserProfileUseCase.execute(null));
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
