/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.LoopSection;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="While Loop")
@Description(value={"While Loop sections are loops that will just keep repeating as long as a condition is met."})
@Example.Examples(value={@Example(value="while size of all players < 5:\n\tsend \"More players are needed to begin the adventure\" to all players\n\twait 5 seconds\n"), @Example(value="set {_counter} to 1\ndo while {_counter} > 1: # false but will increase {_counter} by 1 then get out\n\tadd 1 to {_counter}\n"), @Example(value="# Be careful when using while loops with conditions that are almost\n# always true for a long time without using 'wait %timespan%' inside it,\n# otherwise it will probably hang and crash your server.\nwhile player is online:\n\tgive player 1 dirt\n\twait 1 second # without using a delay effect the server will crash\n")})
@Since(value={"2.0, 2.6 (do while)"})
public class SecWhile
extends LoopSection {
    private Condition condition;
    @Nullable
    private TriggerItem actualNext;
    private boolean doWhile;
    private final Set<Event> ranDoWhile = Collections.newSetFromMap(new WeakHashMap());

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        String expr = parseResult.regexes.get(0).group();
        this.condition = Condition.parse(expr, "Can't understand this condition: " + expr);
        if (this.condition == null) {
            return false;
        }
        this.doWhile = parseResult.hasTag("do");
        this.loadOptionalCode(sectionNode);
        super.setNext(this);
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        if (this.doWhile && this.ranDoWhile.add(event) || this.condition.check(event)) {
            this.currentLoopCounter.put(event, this.currentLoopCounter.getOrDefault(event, 0L) + 1L);
            return this.walk(event, true);
        }
        this.exit(event);
        this.debug(event, false);
        return this.actualNext;
    }

    @Override
    @Nullable
    public ExecutionIntent executionIntent() {
        return this.doWhile ? this.triggerExecutionIntent() : null;
    }

    @Override
    public SecWhile setNext(@Nullable TriggerItem next) {
        this.actualNext = next;
        return this;
    }

    @Override
    @Nullable
    public TriggerItem getActualNext() {
        return this.actualNext;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.doWhile ? "do " : "") + "while " + this.condition.toString(event, debug);
    }

    @Override
    public void exit(Event event) {
        this.ranDoWhile.remove(event);
        super.exit(event);
    }

    static {
        Skript.registerSection(SecWhile.class, "[:do] while <.+>");
    }
}

