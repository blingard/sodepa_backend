package com.sodepa.erp.notification.infrastructure.channel.whatsapp;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;

/**
 * Contrat d'adaptateur WhatsApp Business (Meta Cloud API ou fournisseur secondaire Twilio).
 */
public interface WhatsAppProvider extends ChannelProvider<WhatsAppDispatchRequest> {
}
