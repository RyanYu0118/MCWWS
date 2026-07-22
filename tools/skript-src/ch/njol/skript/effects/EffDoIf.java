/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Do If")
@Description(value={"Execute an effect if a condition is true."})
@Example(value="on join:\n\tgive a diamond to the player if the player has permission \"rank.vip\"\n")
@Since(value={"2.3"})
public class EffDoIf
extends Effect {
    private Effect effect;
    private Condition condition;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String eff = parseResult.regexes.get(0).group();
        String cond = parseResult.regexes.get(1).group();
        this.effect = Effect.parse(eff, "Can't understand this effect: " + eff);
        if (this.effect instanceof EffDoIf) {
            Skript.error("Do if effects may not be nested!");
            return false;
        }
        this.condition = Condition.parse(cond, "Can't understand this condition: " + cond);
        if (this.effect == null || this.condition == null) {
            return false;
        }
        ExecutionIntent executionIntent = this.effect.executionIntent();
        if (executionIntent instanceof ExecutionIntent.StopSections) {
            ExecutionIntent.StopSections intent = (ExecutionIntent.StopSections)executionIntent;
            this.getParser().getHintManager().mergeScope(0, intent.levels(), true);
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
    }

    @Override
    @Nullable
    public TriggerItem walk(Event e) {
        if (this.condition.check(e)) {
            this.effect.setParent(this.getParent());
            this.effect.setNext(this.getNext());
            return this.effect;
        }
        return this.getNext();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.effect.toString(e, debug) + " if " + this.condition.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffDoIf.class, "<.+> if <.+>");
    }
}

