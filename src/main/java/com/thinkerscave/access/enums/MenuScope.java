package com.thinkerscave.access.enums;

/**
 * Governs whether a top-level menu is subscription-gated.
 */
public enum MenuScope {

    /** Super Admin / Platform only — never copied into a tenant schema. */
    PLATFORM,

    /** Every organization gets this regardless of subscription plan. */
    CORE,

    /** Gated by the organization's subscription plan via {@code Menu.feature}. */
    SUBSCRIPTION

}
