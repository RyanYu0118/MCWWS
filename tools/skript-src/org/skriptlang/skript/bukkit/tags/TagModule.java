/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.Tag
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.slot.Slot;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.tags.TagRegistry;
import org.skriptlang.skript.bukkit.tags.elements.conditions.CondIsTagged;
import org.skriptlang.skript.bukkit.tags.elements.effects.EffRegisterTag;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTag;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTagContents;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTagKey;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTagsOf;
import org.skriptlang.skript.bukkit.tags.elements.expressions.ExprTagsOfType;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

public class TagModule
extends HierarchicalAddonModule {
    public static final boolean PAPER_TAGS_EXIST = Skript.classExists("com.destroystokyo.paper.MaterialTags");
    public static TagRegistry tagRegistry;

    public TagModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected boolean canLoadSelf(SkriptAddon addon) {
        return Skript.classExists("org.bukkit.Tag");
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        Classes.registerClass(new ClassInfo<Tag>(Tag.class, "minecrafttag").user("minecraft ?tags?").name("Minecraft Tag").description("A tag that classifies a material, or entity.").since("2.10").parser(new Parser<Tag<?>>(this){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(Tag<?> tag, int flags) {
                return "tag " + String.valueOf(tag.getKey());
            }

            @Override
            public String toVariableNameString(Tag<?> tag) {
                return this.toString(tag, 0);
            }
        }));
        Comparators.registerComparator(Tag.class, Tag.class, (a, b) -> Relation.get(a.getKey().equals((Object)b.getKey())));
        tagRegistry = new TagRegistry();
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, CondIsTagged::register, EffRegisterTag::register, ExprTag::register, ExprTagContents::register, ExprTagKey::register, ExprTagsOf::register, ExprTagsOfType::register);
    }

    @Contract(value="null -> null", pure=true)
    @Nullable
    public static Keyed[] getKeyed(Object input) {
        EntityType value = null;
        Material[] values = null;
        if (input == null) {
            return null;
        }
        if (input instanceof Entity) {
            Entity entity = (Entity)input;
            value = entity.getType();
        }
        if (input instanceof EntityData) {
            EntityData data = (EntityData)input;
            value = EntityUtils.toBukkitEntityType(data);
        } else if (input instanceof ItemType) {
            ItemType itemType = (ItemType)input;
            values = itemType.getMaterials();
        } else if (input instanceof ItemStack) {
            ItemStack itemStack = (ItemStack)input;
            value = itemStack.getType();
        } else if (input instanceof Slot) {
            Slot slot = (Slot)input;
            ItemStack stack = slot.getItem();
            if (stack == null) {
                return null;
            }
            value = stack.getType();
        } else if (input instanceof Block) {
            Block block = (Block)input;
            value = block.getType();
        } else if (input instanceof BlockData) {
            BlockData data = (BlockData)input;
            value = data.getMaterial();
        }
        if (value == null) {
            return values;
        }
        return new Keyed[]{value};
    }

    @Override
    public String name() {
        return "tags";
    }
}

