package com.thinkerscave.common.fee.domain;

/** Channel through which a {@link FeeReminder} is delivered. */
public enum ReminderChannel {
    EMAIL,
    SMS,
    PUSH,
    WHATSAPP,
    IN_APP
}
