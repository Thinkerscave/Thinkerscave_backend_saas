package com.thinkerscave.security.service;

/**
 * Free-friendly SMS channel.
 * Providers: {@code log} (always free), {@code textbelt} (1 free SMS/day with key {@code textbelt}).
 */
public interface SmsService {

    /**
     * @return true when the provider accepted the message (or logged it in log mode)
     */
    boolean sendSms(String mobileNumber, String message);

    boolean isEnabled();
}
