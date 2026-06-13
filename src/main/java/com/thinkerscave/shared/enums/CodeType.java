package com.thinkerscave.shared.enums;

public enum CodeType {

    STUDENT("STU"),
    PARENT("PAR"),
    STAFF("EMP"),
    USER("USR"),
    DOCUMENT("DOC");

    private final String prefix;

    CodeType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
