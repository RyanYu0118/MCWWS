package work.mcwws.buildbridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class FaweBridgeService {

    private final McwwsBuildBridgePlugin plugin;
    private final PlayerQueryService players;

    public FaweBridgeService(McwwsBuildBridgePlugin plugin, PlayerQueryService players) {
        this.plugin = plugin;
        this.players = players;
    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
    }

    public Map<String, Object> requireFawe() {
        if (!isAvailable()) {
            throw new IllegalStateException("FastAsyncWorldEdit is not loaded");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("available", true);
        return out;
    }

    public Map<String, Object> pos(String which, String worldName, int x, int y, int z) {
        return sync(() -> {
            requireFaweLoaded();
            World world = players.resolveWorld(worldName);
            String side = which == null ? "" : which.trim().toLowerCase(Locale.ROOT);
            if (!side.equals("pos1") && !side.equals("1") && !side.equals("pos2") && !side.equals("2")) {
                throw new IllegalArgumentException("which must be pos1 or pos2");
            }
            boolean first = side.equals("pos1") || side.equals("1");
            String cmd = first
                    ? "//pos1 " + x + "," + y + "," + z
                    : "//pos2 " + x + "," + y + "," + z;
            // Ensure selection world: teleport actor briefly if player actor and different world
            CommandSender actor = resolveActor();
            if (actor instanceof Player player && player.getWorld() != world) {
                Location stay = player.getLocation().clone();
                player.teleport(new Location(world, x + 0.5, y, z + 0.5));
                try {
                    dispatch(actor, cmd);
                } finally {
                    player.teleport(stay);
                }
            } else {
                dispatch(actor, cmd);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("which", first ? "pos1" : "pos2");
            out.put("world", world.getName());
            out.put("x", x);
            out.put("y", y);
            out.put("z", z);
            out.put("actor", actorName(actor));
            return out;
        });
    }

    public Map<String, Object> set(String pattern) {
        return runEdit("//set " + requirePattern(pattern));
    }

    public Map<String, Object> replace(String from, String to) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("from and to patterns required");
        }
        return runEdit("//replace " + from.trim() + " " + to.trim());
    }

    public Map<String, Object> copy() {
        return runEdit("//copy");
    }

    public Map<String, Object> paste(Integer offsetX, Integer offsetY, Integer offsetZ, boolean ignoreAir) {
        return sync(() -> {
            requireFaweLoaded();
            CommandSender actor = resolveActor();
            if (offsetX != null || offsetY != null || offsetZ != null) {
                int ox = offsetX == null ? 0 : offsetX;
                int oy = offsetY == null ? 0 : offsetY;
                int oz = offsetZ == null ? 0 : offsetZ;
                if (actor instanceof Player player) {
                    Location stay = player.getLocation().clone();
                    player.teleport(stay.clone().add(ox, oy, oz));
                    try {
                        dispatch(actor, ignoreAir ? "//paste -a" : "//paste");
                    } finally {
                        player.teleport(stay);
                    }
                } else {
                    // Console has no location; use //paste then note offsets unsupported without actor-player
                    if (ox != 0 || oy != 0 || oz != 0) {
                        throw new IllegalArgumentException("paste offset requires config actor-player (online player)");
                    }
                    dispatch(actor, ignoreAir ? "//paste -a" : "//paste");
                }
            } else {
                dispatch(actor, ignoreAir ? "//paste -a" : "//paste");
            }
            return okCommand(actor, "paste");
        });
    }

    public Map<String, Object> undo(int times) {
        int n = Math.max(1, times);
        return runEdit("//undo " + n);
    }

    public Map<String, Object> redo(int times) {
        int n = Math.max(1, times);
        return runEdit("//redo " + n);
    }

    public Map<String, Object> schemList() {
        return sync(() -> {
            requireFaweLoaded();
            Path dir = resolveSchematicsDir();
            List<String> names = new ArrayList<>();
            if (Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path path : stream) {
                        String file = path.getFileName().toString();
                        String lower = file.toLowerCase(Locale.ROOT);
                        if (lower.endsWith(".schem") || lower.endsWith(".schematic") || lower.endsWith(".litematic")) {
                            names.add(file);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to list schematics: " + e.getMessage(), e);
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("ok", true);
            out.put("directory", dir.toString());
            out.put("schematics", names);
            out.put("count", names.size());
            return out;
        });
    }

    public Map<String, Object> schemLoad(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("schematic name required");
        }
        String cleaned = name.trim();
        if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\")) {
            throw new IllegalArgumentException("Invalid schematic name");
        }
        return runEdit("//schem load " + cleaned);
    }

    public Map<String, Object> schemPaste(String worldName, int x, int y, int z, boolean ignoreAir) {
        return sync(() -> {
            requireFaweLoaded();
            World world = players.resolveWorld(worldName);
            CommandSender actor = resolveActor();
            if (!(actor instanceof Player player)) {
                throw new IllegalArgumentException("schem paste at coordinates requires config actor-player (online player)");
            }
            Location stay = player.getLocation().clone();
            player.teleport(new Location(world, x + 0.5, y, z + 0.5));
            try {
                dispatch(actor, ignoreAir ? "//paste -a" : "//paste");
            } finally {
                player.teleport(stay);
            }
            Map<String, Object> out = okCommand(actor, "schem_paste");
            out.put("world", world.getName());
            out.put("x", x);
            out.put("y", y);
            out.put("z", z);
            return out;
        });
    }

    private Map<String, Object> runEdit(String command) {
        return sync(() -> {
            requireFaweLoaded();
            CommandSender actor = resolveActor();
            dispatch(actor, command);
            return okCommand(actor, command);
        });
    }

    private Map<String, Object> okCommand(CommandSender actor, String command) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("command", command);
        out.put("actor", actorName(actor));
        return out;
    }

    private void requireFaweLoaded() {
        if (!isAvailable()) {
            throw new IllegalStateException("FastAsyncWorldEdit is not loaded");
        }
    }

    private CommandSender resolveActor() {
        String name = plugin.getConfig().getString("actor-player", "");
        if (name != null && !name.isBlank()) {
            Player player = Bukkit.getPlayerExact(name.trim());
            if (player == null) {
                throw new IllegalArgumentException("actor-player not online: " + name);
            }
            return player;
        }
        return Bukkit.getConsoleSender();
    }

    private static String actorName(CommandSender actor) {
        return actor instanceof Player p ? p.getName() : "CONSOLE";
    }

    private void dispatch(CommandSender actor, String command) {
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        boolean ok = Bukkit.dispatchCommand(actor, cmd);
        if (!ok) {
            throw new IllegalStateException("Command failed or unknown: " + command);
        }
    }

    private Path resolveSchematicsDir() {
        String relative = plugin.getConfig().getString("schematics-directory", "plugins/FastAsyncWorldEdit/schematics");
        Path root = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path configured = Path.of(relative);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return root.resolve(configured).normalize();
    }

    private static String requirePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("pattern required");
        }
        return pattern.trim();
    }

    private <T> T sync(Callable<T> task) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        Future<T> future = Bukkit.getScheduler().callSyncMethod(plugin, task);
        try {
            return future.get(120, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timed out waiting for main thread", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for main thread", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(cause != null ? cause : e);
        }
    }
}
