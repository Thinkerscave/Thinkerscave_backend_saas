package com.thinkerscave.dashboard.enums;

/**
 * Server-computed rendering state for an individual widget. {@code LOADING}
 * is never sent by the backend (it is a purely client-side state before the
 * response arrives) — it is included here only so the frontend's state
 * union type can share this contract.
 */
public enum WidgetState {
    LOADING,
    SUCCESS,
    EMPTY,
    ERROR
}
