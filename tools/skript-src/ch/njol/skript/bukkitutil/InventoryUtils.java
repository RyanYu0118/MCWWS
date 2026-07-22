/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

public class InventoryUtils {
    @Nullable
    private static final MethodHandle GET_TITLE;
    @Nullable
    private static final MethodHandle GET_INVENTORY;
    @Nullable
    private static final MethodHandle CONVERT_SLOT;
    @Nullable
    private static final MethodHandle GET_TOP_INVENTORY;
    @Nullable
    private static final MethodHandle GET_BOTTOM_INVENTORY;

    @Nullable
    public static String getTitle(InventoryView inventoryView) {
        if (GET_TITLE == null) {
            return inventoryView.getTitle();
        }
        try {
            return GET_TITLE.invoke(inventoryView);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    @Nullable
    public static Inventory getInventory(InventoryView inventoryView, int rawSlot) {
        if (GET_INVENTORY == null) {
            return inventoryView.getInventory(rawSlot);
        }
        try {
            return GET_INVENTORY.invoke(inventoryView, rawSlot);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    @Nullable
    public static Integer convertSlot(InventoryView inventoryView, int rawSlot) {
        if (CONVERT_SLOT == null) {
            return inventoryView.convertSlot(rawSlot);
        }
        try {
            return CONVERT_SLOT.invoke(inventoryView, rawSlot);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    @Nullable
    public static Inventory getTopInventory(InventoryView inventoryView) {
        if (GET_TOP_INVENTORY == null) {
            return inventoryView.getTopInventory();
        }
        try {
            return GET_TOP_INVENTORY.invoke(inventoryView);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    @Nullable
    public static Inventory getBottomInventory(InventoryView inventoryView) {
        if (GET_BOTTOM_INVENTORY == null) {
            return inventoryView.getBottomInventory();
        }
        try {
            return GET_BOTTOM_INVENTORY.invoke(inventoryView);
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    static {
        MethodHandle getTitle = null;
        MethodHandle getInventory = null;
        MethodHandle convertSlot = null;
        MethodHandle getTopInventory = null;
        MethodHandle getBottomInventory = null;
        if (!InventoryView.class.isInterface()) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                getTitle = lookup.findVirtual(InventoryView.class, "getTitle", MethodType.methodType(String.class));
                getInventory = lookup.findVirtual(InventoryView.class, "getInventory", MethodType.methodType(Inventory.class, Integer.TYPE));
                convertSlot = lookup.findVirtual(InventoryView.class, "convertSlot", MethodType.methodType(Integer.TYPE, Integer.TYPE));
                getTopInventory = lookup.findVirtual(InventoryView.class, "getTopInventory", MethodType.methodType(Inventory.class));
                getBottomInventory = lookup.findVirtual(InventoryView.class, "getBottomInventory", MethodType.methodType(Inventory.class));
            }
            catch (IllegalAccessException | NoSuchMethodException e) {
                Skript.exception((Throwable)e, "Failed to load old inventory view support.");
            }
        }
        GET_TITLE = getTitle;
        GET_INVENTORY = getInventory;
        CONVERT_SLOT = convertSlot;
        GET_TOP_INVENTORY = getTopInventory;
        GET_BOTTOM_INVENTORY = getBottomInventory;
    }
}

