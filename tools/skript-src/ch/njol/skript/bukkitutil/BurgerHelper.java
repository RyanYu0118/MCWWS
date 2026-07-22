/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  org.bukkit.Material
 */
package ch.njol.skript.bukkitutil;

import com.google.gson.Gson;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;

public class BurgerHelper {
    private static MethodHandle typeIdMethod;
    public Burger burger;

    public BurgerHelper(String data) {
        if (typeIdMethod == null) {
            throw new IllegalStateException("requires Minecraft 1.12.2 or older");
        }
        this.burger = ((Burger[])new Gson().fromJson(data, Burger[].class))[0];
    }

    public Map<String, Material> mapMaterials() {
        HashMap<String, Material> materials = new HashMap<String, Material>();
        for (Material material : Material.values()) {
            int id;
            try {
                id = typeIdMethod.invokeExact(material);
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }
            String vanillaId = null;
            for (ItemOrBlock item : this.burger.items.item.values()) {
                if (item.numeric_id != id) continue;
                vanillaId = item.text_id;
                break;
            }
            for (ItemOrBlock block : this.burger.blocks.block.values()) {
                if (block.numeric_id != id) continue;
                vanillaId = block.text_id;
                break;
            }
            materials.put(vanillaId, material);
        }
        return materials;
    }

    public static Map<Integer, Material> mapIds() {
        if (typeIdMethod == null) {
            throw new IllegalStateException("requires Minecraft 1.12.2 or older");
        }
        HashMap<Integer, Material> ids = new HashMap<Integer, Material>();
        for (Material mat : Material.values()) {
            try {
                ids.put(typeIdMethod.invokeExact(mat), mat);
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return ids;
    }

    static {
        try {
            MethodHandle mh;
            typeIdMethod = mh = MethodHandles.lookup().findVirtual(Material.class, "getId", MethodType.methodType(Integer.TYPE));
        }
        catch (IllegalAccessException | NoSuchMethodException e) {
            typeIdMethod = null;
        }
    }

    public static class Burger {
        public Items items;
        public Blocks blocks;
        public Source source;
    }

    public static class Items {
        public Map<String, ItemOrBlock> item;
    }

    public static class ItemOrBlock {
        public String text_id;
        public int numeric_id;
        public String display_name;
    }

    public static class Blocks {
        public Map<String, ItemOrBlock> block;
    }

    public static class Source {
        public String file;
    }
}

