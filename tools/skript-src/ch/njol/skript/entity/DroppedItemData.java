/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Item
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.util.Consumer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DroppedItemData
extends EntityData<Item> {
    private static final boolean HAS_JAVA_CONSUMER_DROP = Skript.methodExists(World.class, "dropItem", Location.class, ItemStack.class, Consumer.class);
    @Nullable
    private static Method BUKKIT_CONSUMER_DROP;
    private static final Adjective m_adjective;
    private ItemType @Nullable [] types = null;

    public DroppedItemData() {
    }

    public DroppedItemData(ItemType @Nullable [] types) {
        this.types = types;
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        if (exprs.length > 0 && exprs[0] != null) {
            for (ItemType type : this.types = (ItemType[])exprs[0].getAll()) {
                if (type.getMaterial().isItem()) continue;
                Skript.error("'" + String.valueOf(type) + "' cannot represent a dropped item");
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Item> entityClass, @Nullable Item item) {
        if (item != null) {
            ItemStack itemStack = item.getItemStack();
            this.types = new ItemType[]{new ItemType(itemStack)};
        }
        return true;
    }

    @Override
    public void set(Item item) {
        if (this.types == null) {
            return;
        }
        ItemType itemType = CollectionUtils.getRandom(this.types);
        assert (itemType != null);
        ItemStack stack = itemType.getItem().getRandom();
        assert (stack != null);
        item.setItemStack(stack);
    }

    @Override
    protected boolean match(Item item) {
        if (this.types != null) {
            for (ItemType itemType : this.types) {
                if (!itemType.isOfType(item.getItemStack())) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public Class<? extends Item> getType() {
        return Item.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new DroppedItemData();
    }

    @Override
    protected int hashCode_i() {
        return Arrays.hashCode(this.types);
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof DroppedItemData)) {
            return false;
        }
        DroppedItemData other = (DroppedItemData)entityData;
        return Arrays.equals(this.types, other.types);
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> otherData) {
        if (!(otherData instanceof DroppedItemData)) {
            return false;
        }
        DroppedItemData other = (DroppedItemData)otherData;
        if (this.types != null) {
            return other.types != null && ItemType.isSubset(this.types, other.types);
        }
        return true;
    }

    @Override
    public String toString(int flags) {
        if (this.types == null) {
            return super.toString(flags);
        }
        int gender = this.types[0].getTypes().get(0).getGender();
        return Noun.getArticleWithSpace(gender, flags) + m_adjective.toString(gender, flags) + " " + Classes.toString((Object[])this.types, flags & 0xFFFFFFF9, false);
    }

    @Override
    public boolean canSpawn(@Nullable World world) {
        return this.types != null && this.types.length > 0 && world != null;
    }

    @Override
    @Nullable
    public Item spawn(Location location, @Nullable Consumer<Item> consumer) {
        Item item;
        World world = location.getWorld();
        if (!this.canSpawn(world)) {
            return null;
        }
        assert (this.types != null && this.types.length > 0);
        ItemType itemType = CollectionUtils.getRandom(this.types);
        assert (itemType != null);
        ItemStack stack = itemType.getItem().getRandom();
        assert (stack != null);
        if (consumer == null) {
            item = world.dropItem(location, stack);
        } else if (HAS_JAVA_CONSUMER_DROP) {
            item = world.dropItem(location, stack, consumer);
        } else if (BUKKIT_CONSUMER_DROP != null) {
            try {
                Object[] objectArray = new Object[3];
                objectArray[0] = location;
                objectArray[1] = stack;
                objectArray[2] = consumer::accept;
                item = (Item)BUKKIT_CONSUMER_DROP.invoke((Object)world, objectArray);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                if (Skript.testing()) {
                    Skript.exception((Throwable)e, "Can't spawn " + String.valueOf(this.getName()));
                }
                return null;
            }
        } else {
            item = world.dropItem(location, stack);
            consumer.accept(item);
        }
        return item;
    }

    static {
        EntityData.register(DroppedItemData.class, "dropped item", Item.class, "dropped item");
        try {
            BUKKIT_CONSUMER_DROP = World.class.getDeclaredMethod("dropItem", Location.class, ItemStack.class, org.bukkit.util.Consumer.class);
        }
        catch (NoSuchMethodException | SecurityException exception) {
            // empty catch block
        }
        m_adjective = new Adjective("entities.dropped item.adjective");
    }
}

