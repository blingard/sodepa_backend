package com.sodepa.erp.share.erreurs;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Traduit les exceptions en réponses HTTP exploitables.
 *
 * <p>
 * Sans ce gestionnaire, le projet renvoyait <strong>500 pour tout</strong> :
 * mot de passe erroné, en-tête de corrélation absent, habilitation manquante,
 * nom d'utilisateur déjà pris. Le client ne pouvait ni informer correctement
 * l'utilisateur, ni décider quoi faire — un refus de droits et une panne
 * serveur se ressemblaient trait pour trait.
 * </p>
 *
 * <p>
 * Le corps suit la forme que le front sait déjà lire :
 * {@code {status, code, message, errors?, path, timestamp}}. {@code errors}
 * n'apparaît que pour les erreurs de validation, indexé par nom de champ, ce
 * qui permet de les replacer dans le formulaire au lieu d'une bulle globale.
 * </p>
 *
 * <p>
 * <strong>Aucune trace d'exécution n'est exposée.</strong> Elle part au journal
 * serveur ; la renvoyer au client livrerait la structure interne de
 * l'application à qui sait provoquer une erreur.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GestionnaireErreursApi {

    /** Erreurs applicatives, qui portent déjà leur statut. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> traiterApi(ApiException ex, HttpServletRequest requete) {
        // `warn` et non `error` : ce sont des refus attendus, pas des pannes.
        log.warn("{} sur {} : {}", ex.getCode(), requete.getRequestURI(), ex.getMessage());
        return reponse(ex.getStatus(), ex.getCode(), ex.getMessage(), null, requete);
    }

    /** Refus émis par Spring Security lui-même. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> traiterAccesRefuse(
            AccessDeniedException ex, HttpServletRequest requete) {
        log.warn("Accès refusé sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponse(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                "Vous n'avez pas les droits nécessaires pour cette opération.", null, requete);
    }

    /**
     * Violations de contraintes sur le corps de la requête.
     *
     * <p>Renvoyées champ par champ : c'est ce qui permet au formulaire
     * d'afficher chaque message sous le bon libellé.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> traiterValidation(
            MethodArgumentNotValidException ex, HttpServletRequest requete) {
        Map<String, String> champs = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erreur -> champs.put(
                erreur.getField(),
                erreur.getDefaultMessage() == null ? "Valeur invalide." : erreur.getDefaultMessage()));
        return reponse(HttpStatus.BAD_REQUEST, "VALIDATION",
                "Certains champs sont invalides.", champs, requete);
    }

    /** Paramètre de requête absent ou d'un type inattendu. */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> traiterRequeteInvalide(
            Exception ex, HttpServletRequest requete) {
        log.warn("Requête invalide sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponse(HttpStatus.BAD_REQUEST, "REQUETE_INVALIDE", ex.getMessage(), null, requete);
    }

    /** Exceptions déjà porteuses d'un statut, levées par le code d'infrastructure. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> traiterStatut(
            ResponseStatusException ex, HttpServletRequest requete) {
        HttpStatus statut = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? statut.getReasonPhrase() : ex.getReason();
        log.warn("{} sur {} : {}", statut.value(), requete.getRequestURI(), message);
        return reponse(statut, "ERREUR_" + statut.value(), message, null, requete);
    }

    /**
     * Filet de sécurité.
     *
     * <p>Le message réel part au journal avec sa trace ; le client reçoit un
     * texte neutre. Une exception non prévue est un défaut à corriger, pas une
     * information à publier.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> traiterInattendu(Exception ex, HttpServletRequest requete) {
        log.error("Erreur non traitée sur {}", requete.getRequestURI(), ex);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur interne est survenue.", null, requete);
    }

    private ResponseEntity<Map<String, Object>> reponse(
            HttpStatus statut, String code, String message,
            Map<String, String> champs, HttpServletRequest requete) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("timestamp", Instant.now().toString());
        corps.put("status", statut.value());
        corps.put("code", code);
        corps.put("message", message == null ? statut.getReasonPhrase() : message);
        corps.put("path", requete.getRequestURI());
        if (champs != null && !champs.isEmpty()) {
            corps.put("errors", champs);
        }
        return ResponseEntity.status(statut).body(corps);
    }
}
