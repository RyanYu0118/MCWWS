package work.mcwws.worldedit;

import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class StackCommandArgs {

    final int count;
    final BlockVector3 offsetUnits;
    final boolean blockUnits;
    final boolean ignoreAir;

    private StackCommandArgs(int count, BlockVector3 offsetUnits, boolean blockUnits, boolean ignoreAir) {
        this.count = count;
        this.offsetUnits = offsetUnits;
        this.blockUnits = blockUnits;
        this.ignoreAir = ignoreAir;
    }

    static StackCommandArgs parse(String raw, Player player) throws InputParseException {
        List<String> tokens = tokenize(raw);
        Integer count = null;
        BlockVector3 offsetUnits = null;
        boolean blockUnits = false;
        boolean ignoreAir = false;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (isFlagToken(token)) {
                if ("-m".equals(token)) {
                    if (i + 1 < tokens.size()) {
                        i++;
                    }
                    continue;
                }
                for (int j = 1; j < token.length(); j++) {
                    switch (token.charAt(j)) {
                        case 'a' -> ignoreAir = true;
                        case 'r' -> blockUnits = true;
                        case 's', 'e', 'b' -> { /* no fee impact */ }
                        default -> throw new InputParseException("未知 //stack 开关: -" + token.charAt(j));
                    }
                }
                continue;
            }
            if (count == null && token.matches("\\d+")) {
                count = parseCount(token);
                continue;
            }
            if (offsetUnits == null) {
                offsetUnits = parseOffsetToken(token, player);
            }
        }

        if (count == null) {
            count = 1;
        }
        if (offsetUnits == null) {
            offsetUnits = forwardOffset(player);
        }
        if (offsetUnits.x() == 0 && offsetUnits.y() == 0 && offsetUnits.z() == 0) {
            throw new InputParseException("堆叠方向不能为零向量");
        }
        return new StackCommandArgs(count, offsetUnits, blockUnits, ignoreAir);
    }

    BlockVector3 blockOffset(Region region) {
        BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        return blockUnits ? offsetUnits : offsetUnits.multiply(size);
    }

    long scanVolume(Region region) {
        return FeeEstimate.regionVolume(region) * count;
    }

    private static int parseCount(String token) throws InputParseException {
        int value = Integer.parseInt(token);
        if (value < 1) {
            throw new InputParseException("堆叠次数必须 >= 1");
        }
        return value;
    }

    private static boolean isFlagToken(String token) {
        return token.startsWith("-") && token.length() > 1 && !token.contains(",");
    }

    private static List<String> tokenize(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("//")) {
            trimmed = trimmed.substring(2);
        } else if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int space = trimmed.indexOf(' ');
        if (space >= 0) {
            trimmed = trimmed.substring(space + 1).trim();
        } else {
            trimmed = "";
        }
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return List.of(trimmed.split("\\s+"));
    }

    private static BlockVector3 forwardOffset(Player player) {
        return faceToVector(player.getFacing());
    }

    private static BlockVector3 leftOffset(Player player) {
        return faceToVector(rotateLeft(player.getFacing()));
    }

    private static BlockVector3 rightOffset(Player player) {
        return faceToVector(rotateRight(player.getFacing()));
    }

    private static BlockFace rotateLeft(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    private static BlockFace rotateRight(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private static BlockVector3 faceToVector(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockVector3.at(0, 0, -1);
            case SOUTH -> BlockVector3.at(0, 0, 1);
            case EAST -> BlockVector3.at(1, 0, 0);
            case WEST -> BlockVector3.at(-1, 0, 0);
            case UP -> BlockVector3.at(0, 1, 0);
            case DOWN -> BlockVector3.at(0, -1, 0);
            default -> BlockVector3.at(0, 0, 1);
        };
    }

    private static BlockVector3 parseOffsetToken(String token, Player player) throws InputParseException {
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.contains(",")) {
            String[] parts = lower.split(",");
            if (parts.length != 3) {
                throw new InputParseException("无效偏移: " + token);
            }
            try {
                return BlockVector3.at(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                );
            } catch (NumberFormatException ex) {
                throw new InputParseException("无效偏移: " + token);
            }
        }
        return switch (lower) {
            case "north", "n" -> BlockVector3.at(0, 0, -1);
            case "south", "s" -> BlockVector3.at(0, 0, 1);
            case "east", "e" -> BlockVector3.at(1, 0, 0);
            case "west", "w" -> BlockVector3.at(-1, 0, 0);
            case "up", "u" -> BlockVector3.at(0, 1, 0);
            case "down", "d" -> BlockVector3.at(0, -1, 0);
            case "forward", "me", "f" -> forwardOffset(player);
            case "back" -> forwardOffset(player).multiply(-1);
            case "left", "l" -> leftOffset(player);
            case "right" -> rightOffset(player);
            default -> throw new InputParseException("无效堆叠方向: " + token);
        };
    }
}
