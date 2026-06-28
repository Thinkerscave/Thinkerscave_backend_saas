package com.thinkerscave.shared.enums;

public enum CodeType {

    STUDENT("STU"),
    PARENT("PAR"),
    STAFF("EMP"),
    USER("USR"),
    DOCUMENT("DOC"),
    CUSTOMER("CUS"),
    CONTACT("CON"),
    ORGANIZATION("ORG"),
    TENANT("TEN"),
    PROVISION_JOB("JOB"),
    PROMOTION("PRO"),
    TEMPLATE("TPL");

    private final String prefix;

    CodeType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
