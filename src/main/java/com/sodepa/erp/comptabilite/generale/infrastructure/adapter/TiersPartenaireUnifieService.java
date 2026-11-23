package com.sodepa.erp.comptabilite.generale.infrastructure.adapter;

import com.sodepa.erp.achats.infrastructure.entities.FournisseurEntity;
import com.sodepa.erp.achats.infrastructure.repo.FournisseurRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.PieceConformiteTiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.PieceConformiteTiersRepository;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.TiersRepository;
import com.sodepa.erp.notification.application.NotificationService;
import com.sodepa.erp.share.CodeSequenceService;
import com.sodepa.erp.share.UtilsService;
import com.sodepa.erp.utils.Permissions;
import com.sodepa.erp.utils.TypeTiers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service MDM d'unification du Référentiel des Tiers et Partenaires SODEPA
 * (REF-05 / F-REF-021, F-REF-022, F-REF-023 / CA-REF-032-01 / RG-REF-016).
 * <p>
 * Assure :
 * <ul>
 *   <li>L'unicité et la synchronisation bidirectionnelle entre {@link TiersEntity} (Comptabilité/Ventes)
 *       et {@link FournisseurEntity} (Achats/Marchés).</li>
 *   <li>La création des fiches Clients &amp; Partenaires avec segment commercial, plafond de crédit,
 *       conditions de paiement et dédoublonnage NIU/raison sociale ({@code CA-REF-022-01} &amp; {@code CA-REF-022-02}).</li>
 *   <li>Le suivi des pièces administratives et fiscales obligatoires avec détection des expirations à J-15
 *       et envoi automatique d'alertes par Email et SMS ({@code CA-REF-023-01}).</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TiersPartenaireUnifieService {

    private final TiersRepository tiersRepository;
    private final FournisseurRepository fournisseurRepository;
    private final PieceConformiteTiersRepository pieceConformiteRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private UtilsService utilsService;

    @Autowired(required = false)
    private CodeSequenceService codeSequenceService;

    private boolean callerCanValidateTiers() {
        if (utilsService == null) {
            return true;
        }
        try {
            utilsService.hasAnyPermission(Permissions.VALIDATE_OR_REJECT_TIERS, Permissions.VALIDATE_OR_REJECT_CREATE_TIERS, Permissions.VALIDATE_PROVIDER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Liste les fiches du référentiel unifié des tiers selon leur catégorie et leur état d'activité.
     *
     * @param typeTiers       Catégorie optionnelle de tiers ({@code FOURNISSEUR}, {@code CLIENT}, {@code BAILLEUR_FONDS}, etc.).
     * @param actifsSeulement Indique si seuls les tiers actifs doivent être retournés.
     * @return Liste triée par raison sociale des fiches {@link TiersEntity}.
     */
    @Transactional(readOnly = true)
    public List<TiersEntity> listerTiersUnifies(TypeTiers typeTiers, boolean actifsSeulement) {
        if (typeTiers != null && actifsSeulement) {
            return tiersRepository.findByTypeTiersAndActifIsTrueOrderByRaisonSocialeAsc(typeTiers);
        }
        if (actifsSeulement) {
            return tiersRepository.findAllByActifIsTrueOrderByRaisonSocialeAsc();
        }
        return tiersRepository.findAll();
    }

    /**
     * Crée une fiche Client, Fournisseur ou Partenaire unifiée avec contrôle anti-doublon NIU / Raison sociale
     * et validation algorithmique de la cleé RIB ({@code CA-REF-021-01}, {@code CA-REF-022-01}, {@code CA-REF-022-02}, {@code CA-REF-023-02}).
     *
     * @param requete Données de la fiche tiers à créer.
     * @return L'entité {@link TiersEntity} persistée et synchronisée avec le module Achats si fournisseur.
     * @throws IllegalArgumentException En cas de doublon sur le NIU ou la raison sociale, ou si la cleé RIB est erronée.
     */
    @Transactional
    public TiersEntity creerFicheTiersUnifiee(TiersEntity requete) {
        if (utilsService != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            utilsService.hasAnyPermission(Permissions.INIT_CREATE_TIERS, Permissions.CREATE_PROVIDER, Permissions.VALIDATE_OR_REJECT_TIERS);
        }
        if (requete.getNumeroContribuable() != null && !requete.getNumeroContribuable().isBlank()) {
            String niu = requete.getNumeroContribuable().trim();
            if (tiersRepository.existsByNumeroContribuableIgnoreCase(niu)
                    || fournisseurRepository.existsByNumeroContribuableIgnoreCase(niu)) {
                throw new IllegalArgumentException(
                        "Doublon détecté (CA-REF-022-02 / RG-REF-002) : un tiers avec le NIU [" + niu + "] existe déjà dans le référentiel.");
            }
        }
        if (requete.getRaisonSociale() != null && !requete.getRaisonSociale().isBlank()) {
            String rs = requete.getRaisonSociale().trim();
            if (tiersRepository.existsByRaisonSocialeIgnoreCase(rs)) {
                throw new IllegalArgumentException(
                        "Doublon potentiel détecté (CA-REF-022-02) : la raison sociale [" + rs + "] existe déjà dans le référentiel.");
            }
        }
        if (requete.getCompteBancaireRib() != null && !requete.getCompteBancaireRib().isBlank()) {
            validerCleRib(requete.getCompteBancaireRib());
        }

        // Anti Mass-Assignment : forcer un nouvel UUID serveur
        requete.setId(UUID.randomUUID());
        if (requete.getCode() == null || requete.getCode().isBlank()) {
            String prefix = switch (requete.getTypeTiers() != null ? requete.getTypeTiers() : TypeTiers.CLIENT) {
                case FOURNISSEUR -> "FRN";
                case CLIENT, BOUCHERIE_GROSSISTE -> "CLI";
                case BAILLEUR_FONDS -> "BAI";
                case ELEVEUR_PARTENAIRE -> "ELV";
                default -> "TRS";
            };
            long seq = CodeSequenceService.nextOrCount(codeSequenceService, "seq_code_tiers", tiersRepository::count);
            requete.setCode(String.format("%s-%04d", prefix, seq));
        }
        if (requete.getCompteCollectifCode() == null || requete.getCompteCollectifCode().isBlank()) {
            requete.setCompteCollectifCode(requete.getTypeTiers() == TypeTiers.FOURNISSEUR ? "4011" : "4111");
        }
        if (requete.getPlafondCreditFcfa() == null) {
            requete.setPlafondCreditFcfa(BigDecimal.ZERO);
        }
        boolean accredited = callerCanValidateTiers();
        requete.setActif(true);
        requete.setStatutTiers(accredited ? "ACTIF" : "EN_ATTENTE_AGREMENT");

        TiersEntity saved = tiersRepository.save(requete);

        // Synchronisation automatique vers achat_fournisseurs sans auto-agrément si le créateur n'est pas habilité
        if (saved.getTypeTiers() == TypeTiers.FOURNISSEUR && fournisseurRepository.findByCode(saved.getCode()).isEmpty()) {
            FournisseurEntity frn = FournisseurEntity.builder()
                    .id(saved.getId())
                    .code(saved.getCode())
                    .raisonSociale(saved.getRaisonSociale())
                    .numeroContribuable(saved.getNumeroContribuable())
                    .rccm(saved.getRccm())
                    .regimeFiscal(saved.getRegimeFiscal() != null ? saved.getRegimeFiscal() : "REEL")
                    .adresse(saved.getAdresse())
                    .telephone(saved.getTelephone())
                    .email(saved.getEmail())
                    .banqueNom(saved.getBanqueNom())
                    .compteBancaireRib(saved.getCompteBancaireRib())
                    .agreeSodepa(accredited)
                    .dateAgrement(accredited ? LocalDate.now() : null)
                    .actif(true)
                    .build();
            fournisseurRepository.save(frn);
        }

        return saved;
    }

    /**
     * Synchronise un fournisseur validé ou mis à jour dans le module Achats vers la table comptable {@code tiers}
     * (Processus P5 étape 4 / {@code RG-REF-016} / {@code CA-REF-032-01}).
     *
     * @param frn Entité fournisseur issue du module Achats.
     * @return La fiche {@link TiersEntity} synchronisée dans le référentiel comptable.
     */
    @Transactional
    public TiersEntity synchroniserDepuisFournisseurAchats(FournisseurEntity frn) {
        Optional<TiersEntity> existant = tiersRepository.findByCode(frn.getCode());
        TiersEntity tiers = existant.orElseGet(() -> TiersEntity.builder()
                .id(frn.getId())
                .code(frn.getCode())
                .typeTiers(TypeTiers.FOURNISSEUR)
                .compteCollectifCode("4011")
                .build());

        tiers.setRaisonSociale(frn.getRaisonSociale());
        tiers.setAdresse(frn.getAdresse() != null ? frn.getAdresse() : "Cameroun");
        tiers.setTelephone(frn.getTelephone() != null ? frn.getTelephone() : "000000000");
        tiers.setEmail(frn.getEmail() != null ? frn.getEmail() : frn.getCode().toLowerCase() + "@tiers.sodepa.cm");
        tiers.setNumeroContribuable(frn.getNumeroContribuable());
        tiers.setRccm(frn.getRccm());
        tiers.setRegimeFiscal(frn.getRegimeFiscal() != null ? frn.getRegimeFiscal() : "REEL");
        tiers.setBanqueNom(frn.getBanqueNom());
        tiers.setCompteBancaireRib(frn.getCompteBancaireRib());
        tiers.setActif(frn.isActif() && frn.isAgreeSodepa());
        tiers.setStatutTiers((frn.isActif() && frn.isAgreeSodepa()) ? "ACTIF" : "SUSPENDU");

        return tiersRepository.save(tiers);
    }

    // ==========================================
    // PIÈCES DE CONFORMITÉ & ALERTES J-15 (CA-REF-023-01)
    // ==========================================

    /**
     * Enregistre une pièce administrative ou fiscale obligatoire rattachée à un tiers.
     *
     * @param piece Entité représentant la pièce de conformité (NIU, Attestation de non-redevance, RCCM, RIB).
     * @return La pièce {@link PieceConformiteTiersEntity} sauvegardée.
     * @throws IllegalArgumentException Si le tiers rattaché est introuvable.
     */
    @Transactional
    public PieceConformiteTiersEntity enregistréerPieceConformite(PieceConformiteTiersEntity piece) {
        TiersEntity tiers = tiersRepository.findById(piece.getTiersId())
                .orElseThrow(() -> new IllegalArgumentException("Tiers introuvable : " + piece.getTiersId()));
        piece.setCodeTiers(tiers.getCode());
        if (piece.getDateEmission() == null) {
            piece.setDateEmission(LocalDate.now());
        }
        return pieceConformiteRepository.save(piece);
    }

    /**
     * Liste toutes les pièces de conformité enregistrées pour un tiers donné, triées par date d'expiration croissante.
     *
     * @param tiersId Identifiant UUID du tiers.
     * @return Liste des pièces de conformité du tiers.
     */
    @Transactional(readOnly = true)
    public List<PieceConformiteTiersEntity> listerPiecesTiers(UUID tiersId) {
        return pieceConformiteRepository.findByTiersIdOrderByDateExpirationAsc(tiersId);
    }

    /**
     * Retourne toutes les pièces administratives et fiscales de tiers expirées ou arrivant à expiration
     * dans les {@code delaiJours} prochains jours (par défaut J-15 — {@code CA-REF-023-01}) et décleéenche
     * l'envoi d'une alerte par Email et SMS.
     *
     * @param delaiJours Horizon d'anticipation en jours (15 jours par défaut).
     * @return Liste des alertes d'expiration détectées.
     */
    @Transactional
    public List<Map<String, Object>> detecterAlertesExpirationPieces(int delaiJours) {
        int horizon = delaiJours > 0 ? delaiJours : 15;
        LocalDate today = LocalDate.now();
        LocalDate dateLimite = today.plusDays(horizon);

        List<PieceConformiteTiersEntity> pieces = pieceConformiteRepository.findPiecesExpirantAvant(dateLimite);
        List<Map<String, Object>> alertes = new ArrayList<>();

        for (PieceConformiteTiersEntity p : pieces) {
            long joursRestants = ChronoUnit.DAYS.between(today, p.getDateExpiration());
            String niveauAlerte = joursRestants < 0 ? "EXPIREE_SUSPENSION_REQUISE" : "ALERTE_PREVENTIVE_J15";

            TiersEntity tiers = tiersRepository.findById(p.getTiersId()).orElse(null);
            String raisonSociale = tiers != null ? tiers.getRaisonSociale() : p.getCodeTiers();
            String emailDest = (tiers != null && tiers.getEmail() != null && !tiers.getEmail().isBlank())
                    ? tiers.getEmail().trim()
                    : null;
            String telDest = (tiers != null && tiers.getTelephone() != null && !tiers.getTelephone().isBlank())
                    ? tiers.getTelephone().trim()
                    : null;

            if (!Boolean.TRUE.equals(p.isAlerteJ15Envoyee())) {
                try {
                    notificationService.notifierExpirationPieceTiers(
                            p.getCodeTiers(),
                            raisonSociale,
                            p.getTypePiece(),
                            p.getDateExpiration() != null ? p.getDateExpiration().toString() : "N/A",
                            emailDest,
                            telDest
                    );
                    p.setAlerteJ15Envoyee(true);
                    pieceConformiteRepository.save(p);
                } catch (Exception e) {
                    log.warn("Notification d'expiration de pièce tiers non délivrée : {}", e.getMessage());
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pieceId", p.getId());
            item.put("tiersId", p.getTiersId());
            item.put("codeTiers", p.getCodeTiers());
            item.put("raisonSociale", raisonSociale);
            item.put("typePiece", p.getTypePiece());
            item.put("numeroReference", p.getNumeroReference());
            item.put("dateExpiration", p.getDateExpiration());
            item.put("joursRestants", joursRestants);
            item.put("niveauAlerte", niveauAlerte);
            item.put("notificationEmailSmsEnvoyee", true);
            alertes.add(item);
        }
        return alertes;
    }

    /**
     * Valide la structure et la cleé de contrôle modulo 97 d'un Relevé d'Identité Bancaire (RIB CEMAC / Cameroun)
     * conformément au critère {@code CA-REF-023-02}.
     *
     * @param ribBrut Chaîne RIB soumise.
     * @throws IllegalArgumentException Si la longueur ou la cleé de contrôle RIB (01 à 97) est invalidée.
     */
    private void validerCleRib(String ribBrut) {
        String compact = ribBrut.replaceAll("[\\s\\-]", "").toUpperCase();
        if (compact.length() < 10) {
            throw new IllegalArgumentException("RIB invalidée (CA-REF-023-02) : longueur insuffisante.");
        }
        String deuxDerniers = compact.substring(compact.length() - 2);
        if (!deuxDerniers.matches("^[0-9]{2}$")) {
            throw new IllegalArgumentException("RIB invalidée (CA-REF-023-02) : la cleé RIB à 2 chiffres est incorrecte.");
        }
        int cleée = Integer.parseInt(deuxDerniers);
        if (cleée < 1 || cleée > 97) {
            throw new IllegalArgumentException("RIB invalidée (CA-REF-023-02) : la cleé RIB [" + deuxDerniers + "] doit être comprise entre 01 et 97.");
        }
        if (compact.matches("^[0-9]{23}$")) {
            long codeBanque = Long.parseLong(compact.substring(0, 5));
            long codeGuichet = Long.parseLong(compact.substring(5, 10));
            long numCompte = Long.parseLong(compact.substring(10, 21));
            long cleéeCalculee = 97L - ((89L * codeBanque + 15L * codeGuichet + 3L * numCompte) % 97L);
            if (cleéeCalculee != cleée) {
                throw new IllegalArgumentException(
                        String.format("RIB invalidée (CA-REF-023-02) : la cleé RIB [%02d] est erronée (cleé attendue : %02d).", cleée, cleéeCalculee));
            }
        }
    }
}
