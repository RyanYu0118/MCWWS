/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffectSectionEffect
extends Effect {
    private final EffectSection effectSection;

    public EffectSectionEffect(EffectSection effectSection) {
        this.effectSection = effectSection;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return this.effectSection.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    protected void execute(Event event) {
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        return this.effectSection.walk(event);
    }

    @Override
    public String getIndentation() {
        return this.effectSection.getIndentation();
    }

    @Override
    public TriggerItem setParent(@Nullable TriggerSection parent) {
        return this.effectSection.setParent(parent);
    }

    @Override
    public TriggerItem setNext(@Nullable TriggerItem next) {
        return this.effectSection.setNext(next);
    }

    @Override
    @Nullable
    public TriggerItem getNext() {
        return this.effectSection.getNext();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.effectSection.toString(event, debug);
    }
}

