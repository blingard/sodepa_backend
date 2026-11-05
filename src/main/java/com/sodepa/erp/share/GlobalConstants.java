package com.sodepa.erp.share;

public final class GlobalConstants {

    private GlobalConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String PREFERENCE_USERNAME = "preferred_username";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_HEADER_ERROR = "CORRELATION_ID_HEADER_ERROR";
    public static final int MAX_BODY_SIZE = 10_000;

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static final String DEFAULT_DATE_FORMAT_WITHOUT_MILLIS = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String NEED_EXPRESSION_NOT_FOUND = "NEED_EXPRESSION_NOT_FOUND";
    public static final String ORDER_OR_CONTRACT_FILE_NOT_FOUND = "ORDER_OR_CONTRACT_FILE_NOT_FOUND";
    public static final String SUPPLIER_NOT_FOUND  = "SUPPLIER_NOT_FOUND";


    public static final String PV_REC  = "PV-REC-%d-%04d";
    public static final String PV_REC_NOT_FOUND  = "PV_REC_NOT_FOUND";

    public static final String CLIENT_ID  = "client_id";
    public static final String CLIENT_SECRET  = "client_secret";
    public static final String GRANT_TYPE  = "grant_type";
    public static final String USERNAME  = "username";
    public static final String PASSWORD  = "password";
    public static final String REFRESH_TOKEN  = "refresh_token";


    public static final String USER_ID  = "USER_ID";




    public static final String ACHATS_TAUX_TVA  = "achats.taux_tva";
    public static final String ACHATS_SEUIL_ACHAT_DIRECT  = "achats.seuil_achat_direct";
    public static final String ACHATS_SEUIL_BON_COMMAND  = "achats.seuil_bon_commande";
    public static final String ACHATS_SEUIL_LETTRE_COMMAND  = "achats.seuil_lettre_commande";





    public static final Integer DELAIS_LIVRAISON_PAR_DEFAULT  = 90;

    public static final String ERROR_CREATE_KEYCLOAK_USER  = "ERROR_CREATE_KEYCLOAK_USER";
    public static final String ERROR_KEYCLOAK_USER_NOT_FOUND  = "ERROR_KEYCLOAK_USER_NOT_FOUND";
    public static final String ERROR_KEYCLOAK_CHANGE_PASSWORD  = "ERROR_KEYCLOAK_CHANGE_PASSWORD";
    public static final String ERROR_HORS_PERIMETRE  = "ERROR_HORS_PERIMETRE";
}
