/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.BlockDisplay
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.displays;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.variables.Variables;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class DisplayData
extends EntityData<Display> {
    public static final Color DEFAULT_BACKGROUND_COLOR = ColorRGB.fromRGBA(0, 0, 0, 64).asBukkitColor();
    private DisplayType type = DisplayType.ANY;
    @Nullable
    private BlockData blockData;
    @Nullable
    private ItemStack item;
    @Nullable
    private Component text;

    public static void register(SyntaxRegistry registry) {
        EntityData.register(DisplayData.class, "display", Display.class, 0, DisplayType.codeNames);
        Variables.yggdrasil.registerSingleClass(DisplayType.class, "DisplayType");
    }

    public DisplayData() {
    }

    public DisplayData(DisplayType type) {
        this.type = type;
        this.codeNameIndex = type.ordinal();
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.type = DisplayType.values()[matchedCodeName];
        if (exprs.length == 0 || exprs[0] == null) {
            return true;
        }
        if (this.type == DisplayType.BLOCK) {
            Object object = exprs[0].getSingle();
            if (object instanceof ItemType) {
                ItemType itemType = (ItemType)object;
                if (!itemType.hasBlock()) {
                    Skript.error("A block display must be of a block item. " + Classes.toString(itemType.getMaterial()) + " is not a block. If you want to display an item, use an 'item display'.");
                    return false;
                }
                this.blockData = Bukkit.createBlockData((Material)itemType.getBlockMaterial());
            } else {
                this.blockData = (BlockData)object;
            }
        } else if (this.type == DisplayType.ITEM) {
            ItemType itemType = (ItemType)exprs[0].getSingle();
            if (!itemType.hasItem()) {
                Skript.error("An item display must be of a valid item. " + Classes.toString(itemType.getMaterial()) + " is not a valid item. If you want to display a block, use a 'block display'.");
                return false;
            }
            this.item = itemType.getRandom();
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Display> displayClass, @Nullable Display entity) {
        DisplayType[] types = DisplayType.values();
        for (int i = types.length - 1; i >= 0; --i) {
            Class<? extends Display> display = types[i].displaySubClass;
            if (display == null || !(entity == null ? displayClass.isAssignableFrom(display) : display.isInstance(entity))) continue;
            this.type = types[i];
            if (entity != null) {
                switch (this.type.ordinal()) {
                    case 1: {
                        this.blockData = ((BlockDisplay)entity).getBlock();
                        break;
                    }
                    case 2: {
                        this.item = ((ItemDisplay)entity).getItemStack();
                        break;
                    }
                    case 3: {
                        this.text = ((TextDisplay)entity).text();
                    }
                }
            }
            return true;
        }
        assert (false);
        return false;
    }

    @Override
    public void set(Display entity) {
        switch (this.type.ordinal()) {
            case 1: {
                if (this.blockData == null || !(entity instanceof BlockDisplay)) break;
                BlockDisplay blockDisplay = (BlockDisplay)entity;
                blockDisplay.setBlock(this.blockData);
                break;
            }
            case 2: {
                if (this.item == null || !(entity instanceof ItemDisplay)) break;
                ItemDisplay itemDisplay = (ItemDisplay)entity;
                itemDisplay.setItemStack(this.item);
                break;
            }
            case 3: {
                if (this.text == null || !(entity instanceof TextDisplay)) break;
                TextDisplay textDisplay = (TextDisplay)entity;
                textDisplay.text(this.text);
            }
        }
    }

    @Override
    public boolean match(Display entity) {
        switch (this.type.ordinal()) {
            case 1: {
                if (!(entity instanceof BlockDisplay)) {
                    return false;
                }
                BlockDisplay blockDisplay = (BlockDisplay)entity;
                if (this.blockData == null || blockDisplay.getBlock().equals((Object)this.blockData)) break;
                return false;
            }
            case 2: {
                if (!(entity instanceof ItemDisplay)) {
                    return false;
                }
                ItemDisplay itemDisplay = (ItemDisplay)entity;
                if (this.item == null || itemDisplay.getItemStack().isSimilar(this.item)) break;
                return false;
            }
            case 3: {
                if (!(entity instanceof TextDisplay)) {
                    return false;
                }
                TextDisplay textDisplay = (TextDisplay)entity;
                if (this.text == null || this.text.equals((Object)Component.empty())) {
                    return true;
                }
                return textDisplay.text().equals((Object)this.text);
            }
        }
        return this.type.displaySubClass != null && this.type.displaySubClass.isInstance(entity);
    }

    @Override
    public Class<? extends Display> getType() {
        return this.type.displaySubClass != null ? this.type.displaySubClass : Display.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new DisplayData(DisplayType.ANY);
    }

    @Override
    protected int hashCode_i() {
        return this.type.hashCode();
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (entityData instanceof DisplayData) {
            DisplayData other = (DisplayData)entityData;
            return this.type == other.type;
        }
        return false;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (entityData instanceof DisplayData) {
            DisplayData displayData = (DisplayData)entityData;
            return this.type == DisplayType.ANY || displayData.type == this.type;
        }
        return Display.class.isAssignableFrom(entityData.getType());
    }

    @Override
    public boolean canSpawn(@Nullable World world) {
        return this.type != DisplayType.ANY;
    }

    private static enum DisplayType {
        ANY("org.bukkit.entity.Display", "display"),
        BLOCK("org.bukkit.entity.BlockDisplay", "block display"),
        ITEM("org.bukkit.entity.ItemDisplay", "item display"),
        TEXT("org.bukkit.entity.TextDisplay", "text display");

        @Nullable
        private Class<? extends Display> displaySubClass;
        private final String codeName;
        private static final String[] codeNames;

        private DisplayType(String className, String codeName) {
            try {
                this.displaySubClass = Class.forName(className);
            }
            catch (ClassNotFoundException classNotFoundException) {
                // empty catch block
            }
            this.codeName = codeName;
        }

        public String toString() {
            return this.codeName;
        }

        static {
            ArrayList<String> codeNamesList = new ArrayList<String>();
            for (DisplayType type : DisplayType.values()) {
                if (type.displaySubClass == null) continue;
                codeNamesList.add(type.codeName);
            }
            codeNames = codeNamesList.toArray(new String[0]);
        }
    }
}

