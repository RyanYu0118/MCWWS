/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Spawnable")
@Description(value={"Whether an entity type can be spawned in a world.\nAny general types such as 'monster, mob, entity, living entity' etc. will never be spawnable.\n"})
@Example(value="if a pig is spawnable in world \"world\": # true\nif a monster can be spawned in {_world}: # false\n")
@Since(value={"2.13"})
public class CondIsSpawnable
extends Condition {
    private Expression<EntityData<?>> datas;
    private Expression<World> world = null;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.datas = exprs[0];
        this.world = exprs[1];
        this.setNegated(matchedPattern >= 2);
        return true;
    }

    @Override
    public boolean check(Event event) {
        World world = this.world.getSingle(event);
        if (world == null) {
            return false;
        }
        return this.datas.check(event, entityData -> entityData.canSpawn(world), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append(this.datas, "is");
        if (this.isNegated()) {
            builder.append((Object)"not");
        }
        builder.append((Object)"spawnable");
        if (this.world != null) {
            builder.append("in", this.world);
        }
        return builder.toString();
    }

    static {
        Skript.registerCondition(CondIsSpawnable.class, Condition.ConditionType.COMBINED, "%entitydatas% is spawnable [in [the [world]] %world%]", "%entitydatas% can be spawned [in [the [world]] %world%]", "%entitydatas% (isn't|is not) spawnable [in [the [world]] %world%]", "%entitydatas% (can't|can not) be spawned [in [the [world]] %world%]");
    }
}

