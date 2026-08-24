package com.gmail.legamemc.booknews;

/**
 * Paper 26.2 reports Bukkit version like {@code 26.2.build.112-stable}.
 * Upstream BookNews splits on {@code .} and {@code Integer.parseInt}s the third
 * token, which throws on {@code "build"} and disables the plugin.
 */
public class VersionUtils {
    private static int year;
    private static int release;
    private static int patch;

    public static void setVersion(String raw) {
        if (raw == null || raw.isEmpty()) {
            year = 0;
            release = 0;
            patch = 0;
            return;
        }
        String[] parts = raw.split("\\.");
        int[] nums = new int[3];
        int n = 0;
        for (String part : parts) {
            if (n >= 3) {
                break;
            }
            if (part == null || part.isEmpty()) {
                continue;
            }
            int i = 0;
            while (i < part.length() && Character.isDigit(part.charAt(i))) {
                i++;
            }
            if (i == 0) {
                continue;
            }
            nums[n++] = Integer.parseInt(part.substring(0, i));
        }
        year = n > 0 ? nums[0] : 0;
        release = n > 1 ? nums[1] : 0;
        patch = n > 2 ? nums[2] : 0;
    }

    public static boolean isAboveOrEquals(int release) {
        return isAboveOrEquals(1, release, 0);
    }

    public static boolean isAboveOrEquals(int year, int release) {
        return isAboveOrEquals(year, release, 0);
    }

    public static boolean isAboveOrEquals(int year, int release, int patch) {
        if (VersionUtils.year != year) {
            return VersionUtils.year > year;
        }
        if (VersionUtils.release != release) {
            return VersionUtils.release > release;
        }
        return VersionUtils.patch >= patch;
    }

    public static boolean isNewNumberingSystem() {
        return year != 1;
    }
}
