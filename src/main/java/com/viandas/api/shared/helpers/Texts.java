package com.viandas.api.shared.helpers;

public class Texts {

    private Texts() {

    }

    public static String trim(String value) {
        return value.trim();
    }

    public static String lowerTrim(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }
}
