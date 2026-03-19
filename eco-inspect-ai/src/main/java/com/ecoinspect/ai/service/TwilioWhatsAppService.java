package com.ecoinspect.ai.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TwilioWhatsAppService {

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    /**
     * Sends a WhatsApp message back to the citizen via Twilio Sandbox.
     */
    public void sendMessage(String toPhone, String messageText) {
        try {
            // Ensure phone has whatsapp: prefix
            String toFormatted = toPhone.startsWith("whatsapp:")
                ? toPhone 
                : "whatsapp:" + toPhone;

            Message message = Message.creator(
                new PhoneNumber(toFormatted),
                new PhoneNumber(fromNumber),
                messageText
            ).create();

            log.info("WhatsApp message sent to: {}, SID: {}", toPhone, message.getSid());

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", toPhone, e.getMessage());
        }
    }
}
