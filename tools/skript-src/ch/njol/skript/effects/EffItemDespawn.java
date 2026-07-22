/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Item
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Item Despawn")
@Description(value={"Prevent a dropped item from naturally despawning through Minecraft's timer."})
@Example.Examples(value={@Example(value="prevent all dropped items from naturally despawning"), @Example(value="allow all dropped items to naturally despawn")})
@Since(value={"2.11"})
public class EffItemDespawn
extends Effect {
    private Expression<Item> entities;
    private boolean prevent;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.prevent = matchedPattern == 0;
        this.entities = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Item item : this.entities.getArray(event)) {
            item.setUnlimitedLifetime(this.prevent);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.prevent) {
            builder.append("prevent", this.entities, "from naturally despawning");
        } else {
            builder.append("allow", this.entities, "to naturally despawn");
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffItemDespawn.class, "(prevent|disallow) %itementities% from (naturally despawning|despawning naturally)", "allow natural despawning of %itementities%", "allow %itementities% to (naturally despawn|despawn naturally)");
    }
}

