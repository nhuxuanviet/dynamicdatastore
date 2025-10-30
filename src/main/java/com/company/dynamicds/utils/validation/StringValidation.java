package com.company.dynamicds.utils.validation;

public final class StringValidation {
    private StringValidation() {
        // private constructor to prevent instantiation
    }


    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    public static boolean anyNullOrEmpty(String... strings) {
        if (strings == null || strings.length == 0) return true;
        for (String s : strings) {
            if (isNullOrEmpty(s)) return true;
        }
        return false;
    }

    public static boolean allNotNullOrEmpty(String... strings) {
        if (strings == null || strings.length == 0) return false;
        for (String s : strings) {
            if (isNullOrEmpty(s)) return false;
        }
        return true;
    }


    public static boolean anyNotNullOrEmpty(String... strings) {
        if (strings == null) return false;
        for (String s : strings) {
            if (isNotNullOrEmpty(s)) return true;
        }
        return false;
    }


    public static boolean allNullOrEmpty(String... strings) {
        if (strings == null) return true;
        for (String s : strings) {
            if (isNotNullOrEmpty(s)) return false;
        }
        return true;
    }
}
