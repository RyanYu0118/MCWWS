/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.profile.PlayerProfile
 *  com.destroystokyo.paper.profile.ProfileProperty
 *  io.papermc.paper.persistence.PersistentDataContainerView
 *  org.bukkit.BanEntry
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Statistic
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jspecify.annotations.Nullable
 */
package ch.njol.skript.test.utils;

import ch.njol.skript.test.runner.TestMode;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.persistence.PersistentDataContainerView;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class TestOfflinePlayer
implements OfflinePlayer {
    private static final String PLAYER_NAME = "SkriptLang";
    private static final OfflinePlayer PLAYER = Bukkit.getOfflinePlayer((String)"SkriptLang");
    private static final UUID PLAYER_UUID = PLAYER.getUniqueId();
    private static final PlayerProfile PLAYER_PROFILE = PLAYER.getPlayerProfile();
    private static final @Nullable TestOfflinePlayer instance;

    private TestOfflinePlayer() {
    }

    public static @Nullable TestOfflinePlayer getInstance() {
        return instance;
    }

    public @Nullable String getName() {
        return PLAYER_NAME;
    }

    public UUID getUniqueId() {
        return PLAYER_UUID;
    }

    public boolean isOnline() {
        return PLAYER.isOnline();
    }

    public boolean isConnected() {
        return PLAYER.isConnected();
    }

    public PlayerProfile getPlayerProfile() {
        return PLAYER_PROFILE;
    }

    public boolean isBanned() {
        return PLAYER.isBanned();
    }

    public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(@Nullable String reason, @Nullable Date expires, @Nullable String source) {
        return (E)PLAYER.ban(reason, expires, source);
    }

    public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(@Nullable String reason, @Nullable Instant expires, @Nullable String source) {
        return (E)PLAYER.ban(reason, expires, source);
    }

    public <E extends BanEntry<? super PlayerProfile>> @Nullable E ban(@Nullable String reason, @Nullable Duration duration, @Nullable String source) {
        return (E)PLAYER.ban(reason, duration, source);
    }

    public boolean isWhitelisted() {
        return PLAYER.isWhitelisted();
    }

    public void setWhitelisted(boolean value) {
        PLAYER.setWhitelisted(value);
    }

    public @Nullable Player getPlayer() {
        return PLAYER.getPlayer();
    }

    public long getFirstPlayed() {
        return PLAYER.getFirstPlayed();
    }

    public long getLastPlayed() {
        return PLAYER.getLastPlayed();
    }

    public boolean hasPlayedBefore() {
        return PLAYER.hasPlayedBefore();
    }

    public long getLastLogin() {
        return PLAYER.getLastLogin();
    }

    public long getLastSeen() {
        return PLAYER.getLastSeen();
    }

    public @Nullable Location getRespawnLocation(boolean loadLocationAndValidate) {
        return PLAYER.getRespawnLocation(loadLocationAndValidate);
    }

    public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
        PLAYER.incrementStatistic(statistic);
    }

    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
        PLAYER.decrementStatistic(statistic);
    }

    public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
        PLAYER.incrementStatistic(statistic, amount);
    }

    public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
        PLAYER.decrementStatistic(statistic, amount);
    }

    public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
        PLAYER.setStatistic(statistic, newValue);
    }

    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        return PLAYER.getStatistic(statistic);
    }

    public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        PLAYER.incrementStatistic(statistic, material);
    }

    public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        PLAYER.decrementStatistic(statistic, material);
    }

    public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        return PLAYER.getStatistic(statistic, material);
    }

    public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
        PLAYER.incrementStatistic(statistic, material, amount);
    }

    public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
        PLAYER.decrementStatistic(statistic, material, amount);
    }

    public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
        PLAYER.setStatistic(statistic, material, newValue);
    }

    public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        PLAYER.incrementStatistic(statistic, entityType);
    }

    public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        PLAYER.decrementStatistic(statistic, entityType);
    }

    public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        return PLAYER.getStatistic(statistic, entityType);
    }

    public void incrementStatistic(Statistic statistic, EntityType entityType, int amount) throws IllegalArgumentException {
        PLAYER.incrementStatistic(statistic, entityType, amount);
    }

    public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
        PLAYER.decrementStatistic(statistic, entityType, amount);
    }

    public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
        PLAYER.setStatistic(statistic, entityType, newValue);
    }

    public @Nullable Location getLastDeathLocation() {
        return PLAYER.getLastDeathLocation();
    }

    public @Nullable Location getLocation() {
        return PLAYER.getLocation();
    }

    public PersistentDataContainerView getPersistentDataContainer() {
        return PLAYER.getPersistentDataContainer();
    }

    @NotNull
    public Map<String, Object> serialize() {
        return PLAYER.serialize();
    }

    public boolean isOp() {
        return PLAYER.isOp();
    }

    public void setOp(boolean value) {
        PLAYER.setOp(value);
    }

    static {
        if (TestMode.ENABLED) {
            instance = new TestOfflinePlayer();
            PLAYER_PROFILE.setProperty(new ProfileProperty("textures", "ewogICJ0aW1lc3RhbXAiIDogMTc0NzQyOTg2MTQwOCwKICAicHJvZmlsZUlkIiA6ICI2OWUzNzAyNjJjN2Q0MjU1YWM3NjliMTNhNWZlOGY3NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTYWh2ZGUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTE2MGFiZWVhNDI1YzZmODMyYjc0NmE0NTQ0YzVmYjlhOTgxYjAyZTFiZDg1ZmVhNWM3ZWY4MzFiZGM4NzRmMyIKICAgIH0KICB9Cn0="));
        } else {
            instance = null;
        }
    }
}

