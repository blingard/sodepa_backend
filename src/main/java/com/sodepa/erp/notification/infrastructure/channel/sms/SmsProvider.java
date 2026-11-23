package com.sodepa.erp.notification.infrastructure.channel.sms;

import com.sodepa.erp.notification.infrastructure.channel.ChannelProvider;

/**
 * Contrat d'adaptateur d'une passerelle SMS CEMAC / internationale.
 */
public interface SmsProvider extends ChannelProvider<SmsDispatchRequest> {
}
