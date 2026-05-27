package com.thinkerscave.common.fee.domain;

/** Mechanism used to collect a {@link FeePayment}. */
public enum PaymentMethod {
    CASH,
    CHEQUE,
    BANK_TRANSFER,
    UPI,
    CARD,
    NETBANKING,
    WALLET,
    DEMAND_DRAFT,
    ONLINE_GATEWAY
}
