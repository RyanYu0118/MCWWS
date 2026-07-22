/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.world.LootGenerateEvent
 *  org.bukkit.loot.LootContext
 *  org.bukkit.loot.LootTable
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.loottables;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.loottables.elements.conditions.CondHasLootTable;
import org.skriptlang.skript.bukkit.loottables.elements.conditions.CondIsLootable;
import org.skriptlang.skript.bukkit.loottables.elements.effects.EffGenerateLoot;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLoot;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootContext;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootContextEntity;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootContextLocation;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootContextLooter;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootContextLuck;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootItems;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootTable;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootTableFromString;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprLootTableSeed;
import org.skriptlang.skript.bukkit.loottables.elements.expressions.ExprSecCreateLootContext;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class LootTableModule
extends HierarchicalAddonModule {
    public LootTableModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        Classes.registerClass(new ClassInfo<LootTable>(LootTable.class, "loottable").user("loot ?tables?").name("Loot Table").description("Loot tables represent what items should be in naturally generated containers, what items should be dropped when killing a mob, or what items can be fished.", "You can find more information about this in https://minecraft.wiki/w/Loot_table").since("2.10").parser(new Parser<LootTable>(this){

            @Override
            @Nullable
            public LootTable parse(String key, ParseContext context) {
                NamespacedKey namespacedKey = NamespacedKey.fromString((String)key);
                if (namespacedKey == null) {
                    return null;
                }
                return Bukkit.getLootTable((NamespacedKey)namespacedKey);
            }

            @Override
            public String toString(LootTable o, int flags) {
                return "loot table \"" + String.valueOf(o.getKey()) + "\"";
            }

            @Override
            public String toVariableNameString(LootTable o) {
                return "loot table:" + String.valueOf(o.getKey());
            }
        }).serializer(new Serializer<LootTable>(this){

            @Override
            public Fields serialize(LootTable lootTable) {
                Fields fields = new Fields();
                fields.putObject("key", lootTable.getKey().toString());
                return fields;
            }

            @Override
            public void deserialize(LootTable lootTable, Fields fields) {
                assert (false);
            }

            @Override
            protected LootTable deserialize(Fields fields) throws StreamCorruptedException {
                String key = fields.getAndRemoveObject("key", String.class);
                if (key == null) {
                    throw new StreamCorruptedException();
                }
                NamespacedKey namespacedKey = NamespacedKey.fromString((String)key);
                if (namespacedKey == null) {
                    throw new StreamCorruptedException();
                }
                return Bukkit.getLootTable((NamespacedKey)namespacedKey);
            }

            @Override
            public boolean mustSyncDeserialization() {
                return true;
            }

            @Override
            protected boolean canBeInstantiated() {
                return false;
            }
        }));
        Classes.registerClass(new ClassInfo<LootContext>(LootContext.class, "lootcontext").user("loot ?contexts?").name("Loot Context").description("Represents additional information a loot table can use to modify its generated loot.", "", "Some loot tables will require some values (i.e. looter, location, looted entity) in a loot context when generating loot whereas others may not.", "For example, the loot table of a simple dungeon chest will only require a location, whereas the loot table of a cow will require a looting player, looted entity, and location.", "You can find more information about this in https://minecraft.wiki/w/Loot_context").since("2.10").defaultExpression(new EventValueExpression<LootContext>(LootContext.class)).parser(new Parser<LootContext>(this){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(LootContext context, int flags) {
                StringBuilder builder = new StringBuilder("loot context at ").append(Classes.toString(context.getLocation()));
                if (context.getLootedEntity() != null) {
                    builder.append(" with entity ").append(Classes.toString(context.getLootedEntity()));
                }
                if (context.getKiller() != null) {
                    builder.append(" with killer ").append(Classes.toString(context.getKiller()));
                }
                if (context.getLuck() != 0.0f) {
                    builder.append(" with luck ").append(context.getLuck());
                }
                return builder.toString();
            }

            @Override
            public String toVariableNameString(LootContext context) {
                return "loot context:" + context.hashCode();
            }
        }));
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, CondHasLootTable::register, CondIsLootable::register, EffGenerateLoot::register, ExprLoot::register, ExprLootContext::register, ExprLootContextEntity::register, ExprLootContextLocation::register, ExprLootContextLooter::register, ExprLootContextLuck::register, ExprLootItems::register, ExprLootTable::register, ExprLootTableFromString::register, ExprLootTableSeed::register, ExprSecCreateLootContext::register);
        SyntaxRegistry registry = this.moduleRegistry(addon);
        registry.register(BukkitSyntaxInfos.Event.KEY, ((BukkitSyntaxInfos.Event.Builder)BukkitSyntaxInfos.Event.builder(SimpleEvent.class, "Loot Generate").addEvent(LootGenerateEvent.class).addPattern("loot generat(e|ing)")).addDescription("Called when a loot table of an inventory is generated in the world.", "For example, when opening a shipwreck chest.").addExample("on loot generate:\n\tchance of 10%\n\tadd 64 diamonds to the loot\n\tsend \"You hit the jackpot at %event-location%!\"\n").addSince("2.7").build());
        EventValues.registerEventValue(LootGenerateEvent.class, Entity.class, LootGenerateEvent::getEntity);
        EventValues.registerEventValue(LootGenerateEvent.class, Location.class, event -> event.getLootContext().getLocation());
        EventValues.registerEventValue(LootGenerateEvent.class, LootTable.class, LootGenerateEvent::getLootTable);
        EventValues.registerEventValue(LootGenerateEvent.class, LootContext.class, LootGenerateEvent::getLootContext);
    }

    @Override
    public String name() {
        return "loot tables";
    }
}

