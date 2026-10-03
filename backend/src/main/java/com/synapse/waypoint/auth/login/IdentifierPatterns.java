package com.synapse.waypoint.auth.login;

import java.util.regex.Pattern;

/** How the three kinds of sign-in identifier look. Keeps the login methods mutually exclusive. */
final class IdentifierPatterns {

    /** Staff IDs such as {@code DRV-0036}. */
    static final Pattern STAFF_ID = Pattern.compile("^[A-Za-z]{2,5}-\\d{2,6}$");

    private IdentifierPatterns() {
    }

    static boolean isEmail(String identifier) {
        return identifier.contains("@");
    }

    static boolean isStaffId(String identifier) {
        return STAFF_ID.matcher(identifier).matches();
    }
}
