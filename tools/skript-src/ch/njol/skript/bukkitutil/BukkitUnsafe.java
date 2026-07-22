/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.io.ByteStreams
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.UnsafeValues
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import ch.njol.util.EnumTypeAdapter;
import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BukkitUnsafe {
    @Nullable
    private static final UnsafeValues unsafe = Bukkit.getUnsafe();
    @Nullable
    private static Map<Integer, Material> idMappings;

    @Deprecated(since="2.10.0", forRemoval=true)
    @Nullable
    public static Material getMaterialFromMinecraftId(String id) {
        return BukkitUnsafe.getMaterialFromNamespacedId(id);
    }

    @Nullable
    public static Material getMaterialFromNamespacedId(String id) {
        return Material.matchMaterial((String)(id.toLowerCase().startsWith("minecraft:") ? id : id.replace(":", "_")));
    }

    public static void modifyItemStack(ItemStack stack, String arguments) {
        if (unsafe == null) {
            throw new IllegalStateException("modifyItemStack could not be performed as UnsafeValues are not available.");
        }
        unsafe.modifyItemStack(stack, arguments);
    }

    private static void initIdMappings() {
        try (InputStream is = Skript.getInstance().getResource("materials/ids.json");){
            if (is == null) {
                throw new AssertionError((Object)"missing id mappings");
            }
            String data = new String(ByteStreams.toByteArray((InputStream)is), StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<Integer, String>>(){}.getType();
            Map rawMappings = (Map)new GsonBuilder().registerTypeAdapterFactory(EnumTypeAdapter.factory).create().fromJson(data, type);
            HashMap<Integer, Material> parsed = new HashMap<Integer, Material>(rawMappings.size());
            for (Map.Entry entry : rawMappings.entrySet()) {
                parsed.put((Integer)entry.getKey(), Material.matchMaterial((String)((String)entry.getValue()), (boolean)true));
            }
            idMappings = parsed;
        }
        catch (IOException e) {
            throw new AssertionError((Object)e);
        }
    }

    @Nullable
    public static Material getMaterialFromId(int id) {
        if (idMappings == null) {
            BukkitUnsafe.initIdMappings();
        }
        assert (idMappings != null);
        return idMappings.get(id);
    }

    static {
        if (unsafe == null) {
            throw new Error("UnsafeValues are not available.");
        }
    }
}

