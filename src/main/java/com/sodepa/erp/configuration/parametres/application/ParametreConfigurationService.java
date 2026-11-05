package com.sodepa.erp.configuration.parametres.application;

import com.sodepa.erp.configuration.parametres.infrastructure.entities.ParametreSystemeEntity;
import com.sodepa.erp.configuration.parametres.infrastructure.repo.ParametreSystemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service centralisé de gestion dynamique des paramètres système SODEPA.
 * Fournit un accès en lecture ultra-rapide (cache mémoire ConcurrentHashMap avec fallback BDD)
 * et une mise à jour à chaud sans redémarrage applicatif.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParametreConfigurationService {

    private final ParametreSystemeRepository repository;

    // Cache local synchronisé en mémoire pour des lectures instantanées (< 0.1ms)
    private final Map<String, String> cacheLocal = new ConcurrentHashMap<>();

    /**
     * Récupère une valeur décimale (taux, seuils, montants) avec valeur de repli.
     */
    public BigDecimal getDecimal(String cle, BigDecimal valeurParDefaut) {
        String val = getValeurBrute(cle);
        if (val == null || val.isBlank()) {
            return valeurParDefaut;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            log.warn("Valeur invalide pour le paramètre décimal {}='{}', utilisation du repli: {}", cle, val, valeurParDefaut);
            return valeurParDefaut;
        }
    }

    /**
     * Récupère une valeur entière (durées, délais, seuils d'échelons) avec valeur de repli.
     */
    public int getInt(String cle, int valeurParDefaut) {
        String val = getValeurBrute(cle);
        if (val == null || val.isBlank()) {
            return valeurParDefaut;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            log.warn("Valeur invalide pour le paramètre entier {}='{}', utilisation du repli: {}", cle, val, valeurParDefaut);
            return valeurParDefaut;
        }
    }

    /**
     * Récupère une valeur chaîne (masques, comptes comptables) avec valeur de repli.
     */
    public String getString(String cle, String valeurParDefaut) {
        String val = getValeurBrute(cle);
        return (val != null && !val.isBlank()) ? val.trim() : valeurParDefaut;
    }

    /**
     * Récupère une valeur booléenne avec valeur de repli.
     */
    public boolean getBoolean(String cle, boolean valeurParDefaut) {
        String val = getValeurBrute(cle);
        if (val == null || val.isBlank()) {
            return valeurParDefaut;
        }
        return Boolean.parseBoolean(val.trim());
    }

    /**
     * Lecture d'un paramètre brut avec cache-aside.
     */
    private String getValeurBrute(String cle) {
        return cacheLocal.computeIfAbsent(cle, k -> {
            Optional<ParametreSystemeEntity> opt = repository.findById(k);
            return opt.map(ParametreSystemeEntity::getValeur).orElse(null);
        });
    }

    /**
     * Liste tous les paramètres enregistrés en base.
     */
    @Transactional(readOnly = true)
    public List<ParametreSystemeEntity> listerTous() {
        return repository.findAll();
    }

    /**
     * Liste les paramètres par catégorie fonctionnelle.
     */
    @Transactional(readOnly = true)
    public List<ParametreSystemeEntity> listerParCategorie(String categorie) {
        return repository.findByCategorie(categorie);
    }

    /**
     * Met à jour à chaud un paramètre système sans redémarrage de l'ERP.
     */
    @Transactional
    public ParametreSystemeEntity modifierParametre(String cle, String nouvelleValeur, String modifiePar) {
        ParametreSystemeEntity param = repository.findById(cle)
                .orElseThrow(() -> new IllegalArgumentException("Paramètre système introuvable: " + cle));

        validerFormatValeur(param.getTypeValeur(), nouvelleValeur);

        String ancienneValeur = param.getValeur();
        param.setValeur(nouvelleValeur.trim());
        param.setModifiePar(modifiePar != null ? modifiePar : "SYSTEM");
        param.setDerniereModification(LocalDateTime.now());

        ParametreSystemeEntity saved = repository.save(param);

        // Invalidation et actualisation immédiate du cache en mémoire
        cacheLocal.put(cle, saved.getValeur());

        log.info("PARAMÈTRE SYSTÈME ACTUALISÉ À CHAUD: {} ['{}' -> '{}'] par {}",
                cle, ancienneValeur, nouvelleValeur, modifiePar);

        return saved;
    }

    /**
     * Invalide l'ensemble du cache mémoire pour forcer un rechargement depuis la BDD.
     */
    public void viderCache() {
        cacheLocal.clear();
        log.info("Cache local des paramètres système purgé.");
    }

    private void validerFormatValeur(String typeValeur, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("La valeur du paramètre ne peut pas être vide");
        }
        switch (typeValeur.toUpperCase()) {
            case "DECIMAL":
                try {
                    new BigDecimal(valeur.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("La valeur fournie n'est pas un nombre décimal valide: " + valeur);
                }
                break;
            case "INTEGER":
                try {
                    Integer.parseInt(valeur.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("La valeur fournie n'est pas un entier valide: " + valeur);
                }
                break;
            case "BOOLEAN":
                if (!valeur.trim().equalsIgnoreCase("true") && !valeur.trim().equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("La valeur fournie doit être 'true' ou 'false': " + valeur);
                }
                break;
            default:
                // STRING accepté
                break;
        }
    }

    /**
     * Recharge l'ensemble des paramètres en mémoire depuis la base de données.
     */
    public void rechargerParametres() {
        cacheLocal.clear();
        repository.findAll().forEach(p -> cacheLocal.put(p.getCle(), p.getValeur()));
        log.info("Cache des paramètres système rechargé: {} paramètres actifs", cacheLocal.size());
    }
}
