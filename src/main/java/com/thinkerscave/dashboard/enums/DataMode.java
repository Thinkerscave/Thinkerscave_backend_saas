package com.thinkerscave.dashboard.enums;

/**
 * Marks whether a widget's payload is backed by real persisted data
 * ({@code LIVE}) or by realistic static placeholder data for a domain
 * that has no backend module yet ({@code SAMPLE}). The frontend renders
 * a small "Preview" badge for {@code SAMPLE} widgets so users are never
 * misled into thinking the numbers are real.
 */
public enum DataMode {
    LIVE,
    SAMPLE
}
