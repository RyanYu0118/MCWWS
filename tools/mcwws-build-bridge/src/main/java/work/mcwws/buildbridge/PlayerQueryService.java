package work.mcwws.buildbridge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerQueryService {

    private final McwwsBuildBridgePlugin plugin;

    public PlayerQueryService(McwwsBuildBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, Object> listPlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", player.getName());
            row.put("uuid", player.getUniqueId().toString());
            Location loc = player.getLocation();
            row.put("world", loc.getWorld() != null ? loc.getWorld().getName() : null);
            row.put("x", loc.getX());
            row.put("y", loc.getY());
            row.put("z", loc.getZ());
            players.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("count", players.size());
        out.put("players", players);
        return out;
    }

    public Map<String, Object> getPlayerPos(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            throw new IllegalArgumentException("Player not online: " + name);
        }
        Location loc = player.getLocation();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("name", player.getName());
        out.put("uuid", player.getUniqueId().toString());
        out.put("world", loc.getWorld() != null ? loc.getWorld().getName() : null);
        out.put("x", loc.getX());
        out.put("y", loc.getY());
        out.put("z", loc.getZ());
        out.put("block_x", loc.getBlockX());
        out.put("block_y", loc.getBlockY());
        out.put("block_z", loc.getBlockZ());
        out.put("yaw", loc.getYaw());
        out.put("pitch", loc.getPitch());
        return out;
    }

    public World resolveWorld(String worldName) {
        String name = worldName;
        if (name == null || name.isBlank()) {
            name = plugin.getConfig().getString("default-world", "world");
        }
        World world = Bukkit.getWorld(name);
        if (world == null) {
            throw new IllegalArgumentException("World not found: " + name);
        }
        return world;
    }
}
