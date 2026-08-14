package work.mcwws.worldedit;

import java.util.Locale;
import java.util.Set;

/**
 * 把玩家输入的创世神根命令收成扣费用的规范名。
 * {@code //re} 是 replace，不是 redo；redo 只认 {@code redo}。
 */
final class WeCommandAlias {

    static final Set<String> CHARGE_CANONICAL = Set.of(
            "set",
            "replace",
            "stack",
            "replacenear",
            "walls",
            "overlay",
            "lay",
            "faces",
            "hollow",
            "center",
            "line",
            "curve",
            "move",
            "fall",
            "naturalize",
            "forest",
            "flora",
            "cut",
            "paste",
            "place",
            "cyl",
            "hcyl",
            "sphere",
            "hsphere",
            "pyramid",
            "hpyramid",
            "cone",
            "removenear",
            "removeabove",
            "removebelow",
            "drain",
            "extinguish",
            "snow",
            "thaw",
            "green",
            "fixlava",
            "fixwater"
    );

    private WeCommandAlias() {
    }

    static String canonical(String root) {
        if (root == null || root.isBlank()) {
            return "";
        }
        return switch (root.toLowerCase(Locale.ROOT)) {
            case "0", "air" -> "set";
            case "re", "rep" -> "replace";
            case "outline" -> "faces";
            case "middle" -> "center";
            case "mv" -> "move";
            case "p", "pa" -> "paste";
            case "ex", "ext" -> "extinguish";
            default -> root.toLowerCase(Locale.ROOT);
        };
    }

    static boolean isSetAirAlias(String root) {
        if (root == null) {
            return false;
        }
        String lower = root.toLowerCase(Locale.ROOT);
        return "0".equals(lower) || "air".equals(lower);
    }

    static boolean isRandomCommand(String canonical) {
        return "forest".equals(canonical) || "flora".equals(canonical);
    }
}
