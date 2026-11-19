package com.termux.app.terminal.theme;

/**
 * Tiny pure-JVM ARGB parser. Android's {@code Color.parseColor} is deliberately avoided so the
 * theme registry can be exercised directly by unit tests without a device or Robolectric.
 */
final class Argb {

    private Argb() {
    }

    /** Parses {@code #RGB}, {@code #ARGB}, {@code #RRGGBB} or {@code #AARRGGBB}. */
    static int parse(String hex) {
        if (hex == null) throw new IllegalArgumentException("null color");
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        switch (value.length()) {
            case 3:
                value = expandShort(value);
                break;
            case 4:
                value = "" + value.charAt(0) + value.charAt(0)
                    + expandShort(value.substring(1));
                break;
            case 6:
            case 8:
                break;
            default:
                throw new IllegalArgumentException("bad color: " + hex);
        }
        if (value.length() == 6) value = "FF" + value;
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad color: " + hex, e);
        }
    }

    private static String expandShort(String value) {
        StringBuilder out = new StringBuilder(6);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append(c).append(c);
        }
        return out.toString();
    }
}
