/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.bukkit.entity.EntityType
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.tags.elements.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.tags.SkriptTag;
import org.skriptlang.skript.bukkit.tags.TagModule;
import org.skriptlang.skript.bukkit.tags.TagType;
import org.skriptlang.skript.bukkit.tags.sources.SkriptTagSource;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Register Tag")
@Description(value={"Registers a new tag containing either items or entity datas. Note that items will NOT keep any information other than their type, so adding `diamond sword named \"test\"` to a tag is the same as adding `diamond sword`", "Item tags should be used for contexts where the item is not placed down, while block tags should be used for contexts where the item is placed. For example, and item tag could be \"skript:edible\", while a block tag would be \"skript:needs_water_above\".", "All custom tags will be given the namespace \"skript\", followed by the name you give it. The name must only include the characters A to Z, 0 to 9, and '/', '.', '_', and '-'. Otherwise, the tag will not register.", "", "Please note that two tags can share a name if they are of different types. Registering a new tag of the same name and type will overwrite the existing tag. Tags will reset on server shutdown."})
@Example.Examples(value={@Example(value="register a new custom entity tag named \"fish\" using cod, salmon, tropical fish, and pufferfish"), @Example(value="register an item tag named \"skript:wasp_weapons/swords\" containing diamond sword and netherite sword"), @Example(value="register block tag named \"pokey\" containing sweet berry bush and bamboo sapling"), @Example(value="on player move:\n\tblock at player is tagged as tag \"skript:pokey\"\n\tdamage the player by 1 heart\n")})
@Since(value={"2.10"})
@Keywords(value={"blocks", "minecraft tag", "type", "category"})
public class EffRegisterTag
extends Effect {
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-zA-Z0-9/._-]+");
    private Expression<String> name;
    private Expression<?> contents;
    private TagType<?> type;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffRegisterTag.class).addPattern("register [a[n]] [custom] " + TagType.getFullPattern(true) + " tag named %string% (containing|using) %entitydatas/itemtypes%").supplier(EffRegisterTag::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Literal literal;
        String key;
        this.name = expressions[0];
        Expression<String> expression = this.name;
        if (expression instanceof Literal && !KEY_PATTERN.matcher(key = EffRegisterTag.removeSkriptNamespace((String)(literal = (Literal)expression).getSingle())).matches()) {
            Skript.error("Tag names can only contain the following characters: letters, numbers, and some symbols: '/', '.', '_', and '-'");
            return false;
        }
        this.contents = expressions[1];
        this.type = TagType.getType(parseResult.mark - 1)[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        String name = this.name.getSingle(event);
        if (name == null) {
            return;
        }
        if (!KEY_PATTERN.matcher(name = EffRegisterTag.removeSkriptNamespace(name)).matches()) {
            return;
        }
        NamespacedKey key = new NamespacedKey((Plugin)Skript.getInstance(), name);
        Object[] contents = this.contents.getArray(event);
        if (contents.length == 0) {
            return;
        }
        if (this.type.type() == Material.class) {
            Tag<Material> tag = this.getMaterialTag(key, contents);
            if (this.type == TagType.ITEMS) {
                SkriptTagSource.ITEMS().addTag(tag);
            } else if (this.type == TagType.BLOCKS) {
                SkriptTagSource.BLOCKS().addTag(tag);
            }
        } else if (this.type.type() == EntityType.class) {
            Tag<EntityType> tag = this.getEntityTag(key, contents);
            SkriptTagSource.ENTITIES().addTag(tag);
        }
    }

    @NotNull
    private static String removeSkriptNamespace(@NotNull String key) {
        if (key.startsWith("skript:")) {
            key = key.substring(7);
        }
        return key;
    }

    @Contract(value="_, _ -> new")
    @NotNull
    private Tag<Material> getMaterialTag(NamespacedKey key, Object @NotNull [] contents) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ArrayList<Material> tagContents = new ArrayList<Material>();
        for (Object object : contents) {
            ItemType itemType;
            Keyed[] values = TagModule.getKeyed(object);
            if (object instanceof ItemType && !(itemType = (ItemType)object).isAll()) {
                tagContents.add((Material)values[random.nextInt(0, values.length)]);
                continue;
            }
            for (Keyed value : values) {
                if (!(value instanceof Material)) continue;
                Material material = (Material)value;
                tagContents.add(material);
            }
        }
        return new SkriptTag<Material>(key, tagContents);
    }

    @Contract(value="_, _ -> new")
    @NotNull
    private Tag<EntityType> getEntityTag(NamespacedKey key, Object @NotNull [] contents) {
        ArrayList<EntityType> tagContents = new ArrayList<EntityType>();
        for (Object object : contents) {
            for (Keyed value : TagModule.getKeyed(object)) {
                if (!(value instanceof EntityType)) continue;
                EntityType entityType = (EntityType)value;
                tagContents.add(entityType);
            }
        }
        return new SkriptTag<EntityType>(key, tagContents);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return new SyntaxStringBuilder(event, debug).append("register a new", this.type.toString(), "tag named", this.name, "containing", this.contents).toString();
    }
}

