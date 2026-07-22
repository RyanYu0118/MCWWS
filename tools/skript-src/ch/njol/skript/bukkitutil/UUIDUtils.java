/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.util.StringUtils;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UUIDUtils {
    @Nullable
    public static UUID asUUID(@NotNull Object object) {
        String string;
        if (object instanceof OfflinePlayer) {
            OfflinePlayer offlinePlayer = (OfflinePlayer)object;
            return offlinePlayer.getUniqueId();
        }
        if (object instanceof Entity) {
            Entity entity = (Entity)object;
            return entity.getUniqueId();
        }
        if (object instanceof String && StringUtils.containsAny(string = (String)object, "-")) {
            try {
                return UUID.fromString(string);
            }
            catch (Exception exception) {
            }
        } else if (object instanceof UUID) {
            UUID uuid = (UUID)object;
            return uuid;
        }
        return null;
    }
}

