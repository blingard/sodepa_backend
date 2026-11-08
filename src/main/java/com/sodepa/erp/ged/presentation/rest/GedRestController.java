package com.sodepa.erp.ged.presentation.rest;

import com.sodepa.erp.ged.application.GedService;
import com.sodepa.erp.ged.infrastructure.entities.DocumentGedEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST de la Gestion Électronique des Documents (GED SODEPA)
 * (REF-07 / F-REF-031 / CA-REF-031-01 à 04 / RG-REF-012).
 */
@RestController
@RequestMapping({"/api/ged", "/api/v1/ged"})
@RequiredArgsConstructor
@Tag(name = "Gestion Électronique des Documents — GED SODEPA (REF-07 / F-REF-031)",
     description = "Archivage S3 MinIO, indexation multicritère, versioning sans écrasement, signature électronique SHA-256 et protection de durée de conservation")
/**
 * Contrôleur REST exposant les points de terminaison pour `GedRestController`.
 * Permet la gestion sécurisée des opérations de l'ERP SODEPA.
 */
public class GedRestController {

    private final GedService gedService;

    /**
     * Téléverse et indexe un fichier binaire dans la GED avec versioning automatique.
     *
     * @param file                    Fichier binaire téléversé.
     * @param typeEntite              Type de l'entité métier rattachée.
     * @param entiteId                Identifiant UUID de l'entité rattachée.
     * @param categorieDocumentaire   Catégorie documentaire (ex: {@code PIECE_JUSTIFICATIVE}, {@code CONTRAT}).
     * @param description             Description du document.
     * @param motsClesIndexation      Mots-cleés de recherche plein texte.
     * @param dureeConservationAnnees Durée légale de conservation en années (10 ans par défaut).
     * @param uploadPar               Identifiant de l'utilisateur auteur du dépôt.
     * @return Réponse HTTP 201 contenant la fiche GED crée.
     */
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Téléverser et indexer un document dans la GED avec versioning automatique (CA-REF-031-01 & 02)")
    public ResponseEntity<DocumentGedEntity> uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam String typeEntite,
            @RequestParam UUID entiteId,
            @RequestParam(required = false) String categorieDocumentaire,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String motsClesIndexation,
            @RequestParam(required = false, defaultValue = "10") Integer dureeConservationAnnees,
            @RequestParam(required = false) String uploadPar) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                gedService.archiverDocument(
                        file, typeEntite, entiteId, categorieDocumentaire,
                        description, motsClesIndexation, dureeConservationAnnees, uploadPar));
    }

    /**
     * Enregistre et versionne un document officiel généré au format JSON/métadonnées dans la GED.
     *
     * @param request Métadonnées du document à archiver.
     * @return Réponse HTTP 201 contenant le document archivé.
     */
    @PostMapping("/documents")
    @Operation(summary = "Enregistrer et versionner un document officiel généré dans la GED (CA-REF-031-01 & 02 / CT-REF-06)")
    public ResponseEntity<DocumentGedEntity> archiverDocumentJson(@Valid @RequestBody DocumentGedEntity request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gedService.archiverDocumentJson(request));
    }

    /**
     * Consulte tous les documents et versions rattachés à une transaction ou entité métier donnée.
     *
     * @param typeEntite Type de l'entité métier.
     * @param entiteId   Identifiant UUID de l'entité.
     * @return Réponse HTTP 200 contenant la liste des documents rattachés.
     */
    @GetMapping("/documents/entite/{typeEntite}/{entiteId}")
    @Operation(summary = "Consulter tous les documents et versions rattachés à une transaction ou entité métier (BF-REF-052)")
    public ResponseEntity<List<DocumentGedEntity>> consulterDocumentsEntite(
            @PathVariable String typeEntite,
            @PathVariable UUID entiteId) {
        return ResponseEntity.ok(gedService.consulterDocumentsEntite(typeEntite, entiteId));
    }

    /**
     * Recherche multicritère et plein texte de documents dans la GED.
     *
     * @param typeEntite Filtre optionnel sur le type d'entité.
     * @param categorie  Filtre optionnel sur la catégorie documentaire.
     * @param motCle     Mot-cleé plein texte recherché sur le nom, la description et les mots-cleés.
     * @return Réponse HTTP 200 contenant les documents correspondants.
     */
    @GetMapping("/documents/recherche")
    @Operation(summary = "Rechercher des documents dans la GED par type d'entité, catégorie et mots-cleés plein texte (CA-REF-031-01)")
    public ResponseEntity<List<DocumentGedEntity>> rechercherDocuments(
            @RequestParam(required = false) String typeEntite,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String motCle) {
        return ResponseEntity.ok(gedService.rechercherDocuments(typeEntite, categorie, motCle));
    }

    /**
     * Appose une signature et un cachet électroniques scellant l'empreinte SHA-256 du document.
     *
     * @param id            Identifiant UUID du document GED.
     * @param signataireNom Nom complet ou identifiant du signataire.
     * @return Réponse HTTP 200 contenant le document signé électroniquement.
     */
    @PostMapping("/documents/{id}/signer")
    @Operation(summary = "Apposer une signature et un cachet électroniques scellant l'empreinte SHA-256 du document (CA-REF-031-03)")
    public ResponseEntity<DocumentGedEntity> signerDocument(
            @PathVariable UUID id,
            @RequestParam String signataireNom) {
        return ResponseEntity.ok(gedService.signerDocumentElectroniquement(id, signataireNom));
    }

    /**
     * Vérifie l'authenticité de la signature électronique et détecte toute altération de l'empreinte SHA-256 post-signature.
     *
     * @param id               Identifiant UUID du document GED.
     * @param hashSha256Soumis Empreinte SHA-256 soumise pour vérification d'intégrité.
     * @return Réponse HTTP 200 contenant le diagnostic d'intégrité cryptographique.
     */
    @PostMapping("/documents/{id}/verifier-integrite")
    @Operation(summary = "Vérifier l'authenticité de la signature électronique et détecter toute altération SHA-256 (CA-REF-031-03)")
    public ResponseEntity<Map<String, Object>> verifierIntegrite(
            @PathVariable UUID id,
            @RequestParam(required = false) String hashSha256Soumis) {
        return ResponseEntity.ok(gedService.verifierIntegriteEtSignature(id, hashSha256Soumis));
    }

    /**
     * Supprime un document GED uniquement si sa durée légale de conservation est échue ({@code CA-REF-031-04}).
     *
     * @param id Identifiant UUID du document GED.
     * @return Réponse HTTP 204 si la suppression est autorisée.
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "Supprimer un document (bloqué tant que la durée légale de conservation n'est pas échue — CA-REF-031-04)")
    public ResponseEntity<Void> supprimerDocument(@PathVariable UUID id) {
        gedService.supprimerDocument(id);
        return ResponseEntity.noContent().build();
    }
}
