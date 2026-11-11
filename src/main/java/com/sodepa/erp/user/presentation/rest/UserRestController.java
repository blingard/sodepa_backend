package com.sodepa.erp.user.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.share.MakerCheckerSmartOutput;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.user.application.inputs.*;
import com.sodepa.erp.user.application.outputs.UserOutput;
import com.sodepa.erp.user.application.usecase.*;
import com.sodepa.erp.user.presentation.requests.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service d'Administration des Comptes Utilisateurs et Collaborateurs SODEPA (Module IAM / User).
 * <p>
 * Ce service regit le cycle de vie des identites numeriques au sein de l'ERP SODEPA :
 * <ul>
 *   <li><b>Creation securisee en double controle (Maker-Checker)</b> : L'ouverture d'un compte utilisateur fait l'objet d'une demande prealable par un initiateur, validee ensuite par un responsable habilite.</li>
 *   <li><b>Mise a jour des donnees signaletiques</b> : Modification de l'identite, contacts, adresses email et numeros de telephone professionnels.</li>
 *   <li><b>Gestion des permissions individuelles</b> : Ajustement des privileges d'acces en conformite avec les profils fonctionnels attribues.</li>
 *   <li><b>Gestion de l'avatar et photo de profil</b> : Mise a jour par l'agent de sa propre photo sans necessiter de validation hierarchique.</li>
 *   <li><b>Recherche et annuaire d'entreprise</b> : Consultation de la fiche profil connectee et recherche multicritere dans l'annuaire des employes SODEPA.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs & Gestion des Identités", description = "Administration des comptes collaborateurs, annuaire d'entreprise et circuit Maker-Checker des utilisateurs")
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
     * Initie une demande de creation de compte utilisateur dans le circuit a 4 yeux (Maker-Checker).
     * <p>
     * Ne cree pas immediatement le compte dans la base, mais depose un ticket d'approbation.
     * La reponse fournit le numero de ticket ainsi que l'identifiant pre-reserve pour le futur utilisateur.
     *
     * @param request donnees d'identite du collaborateur (nom, prenom, email, telephone, permissions souhaitees)
     * @param file photo d'identite optionnelle du collaborateur
     * @return la confirmation de soumission HTTP 202 (ACCEPTED) avec les identifiants de suivi
     */
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/init_create")
    @Operation(summary = "Initier une demande de création de compte collaborateur (Maker-Checker)")
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
     * Liste l'ensemble des demandes de creation ou modification de comptes actuellement en attente d'arbitrage.
     * <p>
     * Constitue la file de travail des validateurs et administrateurs d'identite (Checker inbox).
     *
     * @param pageable informations de pagination
     * @return la page des demandes d'utilisateurs en instance
     */
    @GetMapping("/pending")
    @Operation(summary = "Lister les demandes de comptes en attente de validation (Boîte Checker)")
    public ResponseEntity<PageRecord<MakerCheckerSmartOutput>> getPending(Pageable pageable) {
        return ResponseEntity.ok(getPendingUserRequestsUseCase.execute(pageable));
    }

    /**
     * Initie une mise a jour des coordonnees ou du statut d'activite d'un collaborateur existant.
     *
     * @param id identifiant unique du compte a modifier
     * @param request nouvelles informations signaletiques (nom, prenom, email, telephone, etat actif)
     * @return confirmation d'enregistrement
     */
    @PutMapping("/init_update/{id}")
    @Operation(summary = "Initier la mise à jour des coordonnées d'un collaborateur")
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

    /**
     * Initie le changement de la photo de profil d'un collaborateur tiers par un gestionnaire administratif.
     *
     * @param id identifiant du collaborateur concerne
     * @param file nouveau fichier image
     * @return confirmation
     */
    @PutMapping("/init_change_photo/{id}")
    @Operation(summary = "Initier le changement de photo d'un collaborateur")
    public ResponseEntity<Void> initChangePhoto(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        ChangePhotoInput input = new ChangePhotoInput(id, file);
        changePhotoUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Permet a l'utilisateur connecte de modifier directement et librement sa propre photo de profil.
     * <p>
     * Mise a jour immediate ne necessitant aucune validation hierarchique Maker-Checker.
     *
     * @param file nouvelle photo de profil de l'utilisateur
     * @return confirmation
     */
    @PutMapping("/change_photo")
    @Operation(summary = "Modifier ma propre photo de profil (mise à jour directe)")
    public ResponseEntity<Void> changePhoto(
            @RequestPart("file") MultipartFile file
    ) {
        ChangePhotoInput input = new ChangePhotoInput(null, file);
        changeOwnPhotoUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Initie la mise a jour des permissions unitaires affectees a un compte utilisateur.
     *
     * @param id identifiant du collaborateur
     * @param request nouvelle liste des privileges fonctionnels
     * @return confirmation
     */
    @PutMapping("/init_update_permissions/{id}")
    @Operation(summary = "Initier la modification des permissions d'un utilisateur")
    public ResponseEntity<Void> initUpdatePermissions(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionsRequest request
    ) {
        UpdatePermissionsInput input = new UpdatePermissionsInput(id, request.permissions());
        updateUserPermissionsUseCase.execute(input);
        return ResponseEntity.ok().build();
    }

    /**
     * Approuve ou rejette formellement une demande de creation ou modification de compte soumise au double controle.
     *
     * @param id identifiant du ticket Maker-Checker
     * @param request decision d'accord ou de refus motive
     * @return confirmation de prise en compte
     */
    @PutMapping("/validate_or_reject/{id}")
    @Operation(summary = "Valider ou rejeter une demande relative à un compte utilisateur")
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

    /**
     * Consulte la fiche detaillee d'un utilisateur a partir de son identifiant technique.
     *
     * @param id identifiant unique du compte
     * @return les donnees d'identite, permissions et statut du compte
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consulter la fiche détaillée d'un utilisateur")
    public ResponseEntity<UserOutput> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(getUserByIdUseCase.execute(id));
    }

    /**
     * Renvoie la fiche de profil complete de l'utilisateur actuellement connecte a l'application.
     *
     * @return les informations d'identite et d'habilitations de l'agent connecte
     */
    @GetMapping("/profile")
    @Operation(summary = "Consulter mon profil utilisateur connecté")
    public ResponseEntity<UserOutput> getUserProfile() {
        return ResponseEntity.ok(getUserProfileUseCase.execute(null));
    }

    /**
     * Restitue la liste paginee de l'ensemble des utilisateurs enregistres dans l'ERP SODEPA.
     *
     * @param pageable parametres de pagination et de tri
     * @return la page des comptes collaborateurs
     */
    @GetMapping
    @Operation(summary = "Lister l'ensemble des collaborateurs enregistrés avec pagination")
    public ResponseEntity<PageRecord<UserOutput>> getPage(Pageable pageable) {
        return ResponseEntity.ok(getPageUsersUseCase.execute(pageable));
    }

    /**
     * Recherche des collaborateurs dans l'annuaire d'entreprise a l'aide de criteres multicriteres.
     * <p>
     * Permet le filtrage par nom, prenom, adresse email ou numero de telephone professionnel.
     *
     * @param nom fragment du nom de famille
     * @param prenom fragment du prenom
     * @param email adresse email professionnelle
     * @param telephone numero de telephone
     * @param pageable pagination des resultats
     * @return la liste paginee des collaborateurs correspondants
     */
    @GetMapping("/search")
    @Operation(summary = "Rechercher des collaborateurs dans l'annuaire d'entreprise")
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
