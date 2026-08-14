package work.mcwws.worldedit;

import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Locale;

final class WeDirection {

    private WeDirection() {
    }

    static BlockVector3 parse(String token, Player player) throws InputParseException {
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.contains(",")) {
            String[] parts = lower.split(",");
            if (parts.length != 3) {
                throw new InputParseException("无效方向: " + token);
            }
            try {
                return BlockVector3.at(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                );
            } catch (NumberFormatException ex) {
                throw new InputParseException("无效方向: " + token);
            }
        }
        return switch (lower) {
            case "north", "n" -> BlockVector3.at(0, 0, -1);
            case "south", "s" -> BlockVector3.at(0, 0, 1);
            case "east", "e" -> BlockVector3.at(1, 0, 0);
            case "west", "w" -> BlockVector3.at(-1, 0, 0);
            case "up", "u" -> BlockVector3.at(0, 1, 0);
            case "down", "d" -> BlockVector3.at(0, -1, 0);
            case "forward", "me", "f" -> forward(player);
            case "back" -> forward(player).multiply(-1);
            case "left", "l" -> left(player);
            case "right" -> right(player);
            default -> throw new InputParseException("无效方向: " + token);
        };
    }

    static BlockVector3 forward(Player player) {
        return faceToVector(player.getFacing());
    }

    private static BlockVector3 left(Player player) {
        return faceToVector(rotateLeft(player.getFacing()));
    }

    private static BlockVector3 right(Player player) {
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
}
