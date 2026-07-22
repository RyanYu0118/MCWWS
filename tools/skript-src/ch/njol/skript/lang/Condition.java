/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Statement;
import ch.njol.skript.lang.simplification.Simplifiable;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.condition.Conditional;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Priority;

public abstract class Condition
extends Statement
implements Conditional<Event>,
SyntaxRuntimeErrorProducer,
Simplifiable<Condition> {
    private boolean negated;
    private Node node;

    protected Condition() {
    }

    @Override
    public boolean preInit() {
        this.node = this.getParser().getNode();
        return super.preInit();
    }

    public abstract boolean check(Event var1);

    @Override
    public Kleenean evaluate(Event event) {
        return Kleenean.get(this.check(event));
    }

    @Override
    public final boolean run(Event event) {
        return this.check(event);
    }

    protected final void setNegated(boolean invert) {
        this.negated = invert;
    }

    public final boolean isNegated() {
        return this.negated;
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    @NotNull
    public String getSyntaxTypeName() {
        return "condition";
    }

    @Override
    public Condition simplify() {
        return this;
    }

    @Nullable
    public static Condition parse(String input, @Nullable String defaultError) {
        input = input.trim();
        while (input.startsWith("(") && SkriptParser.next(input, 0, ParseContext.DEFAULT) == input.length()) {
            input = input.substring(1, input.length() - 1);
        }
        Iterator<SyntaxInfo<? extends Condition>> iterator = Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.CONDITION).iterator();
        return SkriptParser.parse(input, iterator, defaultError);
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static enum ConditionType {
        COMBINED(SyntaxInfo.COMBINED),
        PROPERTY(PropertyCondition.DEFAULT_PRIORITY),
        PATTERN_MATCHES_EVERYTHING(SyntaxInfo.PATTERN_MATCHES_EVERYTHING);

        private final Priority priority;

        private ConditionType(Priority priority) {
            this.priority = priority;
        }

        public Priority priority() {
            return this.priority;
        }
    }
}

