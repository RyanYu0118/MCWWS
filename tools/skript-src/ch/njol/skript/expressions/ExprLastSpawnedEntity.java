/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Firework
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.LightningStrike
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.EffDrop;
import ch.njol.skript.effects.EffFireworkLaunch;
import ch.njol.skript.effects.EffLightning;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.sections.EffSecShoot;
import ch.njol.skript.sections.EffSecSpawn;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Spawned Entity")
@Description(value={"Holds the entity that was spawned most recently with the spawn effect (section), dropped with the <a href='../effects/#EffDrop'>drop effect</a>, shot with the <a href='../effects/#EffShoot'>shoot effect</a> or created with the <a href='../effects/#EffLightning'>lightning effect</a>. Please note that even though you can spawn multiple mobs simultaneously (e.g. with 'spawn 5 creepers'), only the last spawned mob is saved and can be used. If you spawn an entity, shoot a projectile and drop an item you can however access all them together."})
@Example.Examples(value={@Example(value="spawn a priest\nset {healer::%spawned priest%} to true\n"), @Example(value="shoot an arrow from the last spawned entity\nignite the shot projectile\n"), @Example(value="drop a diamond sword\npush last dropped item upwards\n"), @Example(value="teleport player to last struck lightning"), @Example(value="delete last launched firework")})
@Since(value={"1.3 (spawned entity), 2.0 (shot entity), 2.2-dev26 (dropped item), 2.7 (struck lightning, firework)"})
public class ExprLastSpawnedEntity
extends SimpleExpression<Entity> {
    private EntityData<?> type;
    private int from;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.from = parseResult.mark;
        this.type = this.from == 2 ? EntityData.fromClass(Item.class) : (this.from == 3 ? EntityData.fromClass(LightningStrike.class) : (this.from == 4 ? EntityData.fromClass(Firework.class) : (EntityData)((Literal)exprs[0]).getSingle()));
        return true;
    }

    @Nullable
    protected Entity[] get(Event event) {
        Entity en = switch (this.from) {
            case 0 -> EffSecSpawn.lastSpawned;
            case 1 -> EffSecShoot.lastSpawned;
            case 2 -> EffDrop.lastSpawned;
            case 3 -> EffLightning.lastSpawned;
            case 4 -> EffFireworkLaunch.lastSpawned;
            default -> null;
        };
        if (en == null) {
            return null;
        }
        if (!this.type.isInstance(en)) {
            return null;
        }
        Entity[] one = (Entity[])Array.newInstance(this.type.getType(), 1);
        one[0] = en;
        return one;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Entity> getReturnType() {
        return this.type.getType();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String word = "";
        switch (this.from) {
            case 0: {
                word = "spawned";
                break;
            }
            case 1: {
                word = "shot";
                break;
            }
            case 2: {
                word = "dropped";
                break;
            }
            case 3: {
                word = "struck";
                break;
            }
            case 4: {
                word = "launched";
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
        return "the last " + word + " " + String.valueOf(this.type);
    }

    static {
        Skript.registerExpression(ExprLastSpawnedEntity.class, Entity.class, ExpressionType.SIMPLE, "[the] [last[ly]] (0:spawned|1:shot) %*entitydata%", "[the] [last[ly]] dropped (2:item)", "[the] [last[ly]] (created|struck) (3:lightning)", "[the] [last[ly]] (launched|deployed) (4:firework)");
    }
}

