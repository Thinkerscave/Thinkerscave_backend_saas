package com.thinkerscave.academics.repository;

/** Grouped count projection used by batch queries that avoid per-row lookups. */
public interface IdCount {

    Long getId();

    Long getTotal();
}
