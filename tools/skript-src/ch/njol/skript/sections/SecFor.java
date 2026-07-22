/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.util.ContainerExpression;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.sections.SecLoop;
import ch.njol.skript.util.Container;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.variables.HintManager;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="For Each Loop")
@Description(value={"A specialised loop section run for each element in a list.\nUnlike the basic loop, this is designed for extracting the key & value from pairs.\nThe loop element's key/index and value can be stored in a variable for convenience.\n\nWhen looping a simple (non-indexed) set of values, e.g. all players, the index will be the loop counter number."})
@Example.Examples(value={@Example(value="for each {_player} in players:\n\tsend \"Hello %{_player}%!\" to {_player}\n"), @Example(value="loop {_item} in {list of items::*}:\n\tbroadcast {_item}'s name\n"), @Example(value="for each key {_index} in {list of items::*}:\n\tbroadcast {_index}\n"), @Example(value="loop key {_index} and value {_value} in {list of items::*}:\n\tbroadcast \"%{_index}% = %{_value}%\"\n"), @Example(value="for each {_index}, {_value} in {my list::*}:\n\tbroadcast \"%{_index}% = %{_value}%\"\n")})
@Since(value={"2.10, 2.14 (stable release)"})
public class SecFor
extends SecLoop {
    @Nullable
    private Expression<?> keyStore;
    @Nullable
    private Expression<?> valueStore;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        switch (matchedPattern) {
            case 0: {
                this.valueStore = exprs[0];
                this.expression = LiteralUtils.defendExpression(exprs[1]);
                break;
            }
            case 1: {
                this.keyStore = exprs[0];
                this.expression = LiteralUtils.defendExpression(exprs[1]);
                break;
            }
            default: {
                this.keyStore = exprs[0];
                this.valueStore = exprs[1];
                this.expression = LiteralUtils.defendExpression(exprs[2]);
            }
        }
        if (this.keyStore != null && !(this.keyStore instanceof Variable)) {
            Skript.error("The 'key' input for a for-loop must be a variable to store the value.");
            return false;
        }
        if (!(this.valueStore instanceof Variable) && this.valueStore != null) {
            Skript.error("The 'value' input for a for-loop must be a variable to store the value.");
            return false;
        }
        if (!LiteralUtils.canInitSafely(this.expression)) {
            Skript.error("Can't understand this loop: '" + parseResult.expr + "'");
            return false;
        }
        if (!(this.expression instanceof Variable) && Container.class.isAssignableFrom(this.expression.getReturnType())) {
            Container.ContainerType type = this.expression.getReturnType().getAnnotation(Container.ContainerType.class);
            if (type == null) {
                throw new SkriptAPIException(this.expression.getReturnType().getName() + " implements Container but is missing the required @ContainerType annotation");
            }
            this.expression = new ContainerExpression(this.expression, type.value());
        }
        if (this.getParser().hasExperiment(Feature.QUEUES) && this.expression.isSingle() && (this.expression instanceof Variable || this.expression.canReturn(Iterable.class))) {
            this.iterableSingle = true;
        } else if (this.expression.isSingle()) {
            Skript.error("Can't loop '" + String.valueOf(this.expression) + "' because it's only a single value");
            return false;
        }
        this.keyed = KeyProviderExpression.canReturnKeys(this.expression);
        HintManager hintManager = this.getParser().getHintManager();
        if (this.keyStore != null && HintManager.canUseHints((Variable)this.keyStore)) {
            Class[] hints = this.expression instanceof Variable ? new Class[]{String.class} : new Class[]{String.class, Long.class};
            hintManager.add((Variable)this.keyStore, hints);
        }
        if (this.valueStore != null && HintManager.canUseHints((Variable)this.valueStore)) {
            hintManager.add((Variable)this.valueStore, this.expression.possibleReturnTypes());
        }
        this.loadOptionalCode(sectionNode);
        this.setInternalNext(this);
        return true;
    }

    @Override
    protected void store(Event event, Object next) {
        super.store(event, next);
        if (next instanceof KeyedValue) {
            KeyedValue keyedValue = (KeyedValue)next;
            if (this.keyStore != null) {
                this.keyStore.change(event, new Object[]{keyedValue.key()}, Changer.ChangeMode.SET);
            }
            if (this.valueStore != null) {
                this.valueStore.change(event, new Object[]{keyedValue.value()}, Changer.ChangeMode.SET);
            }
        } else {
            if (this.keyStore != null) {
                this.keyStore.change(event, new Object[]{this.getLoopCounter(event)}, Changer.ChangeMode.SET);
            }
            if (this.valueStore != null) {
                this.valueStore.change(event, new Object[]{next}, Changer.ChangeMode.SET);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.keyStore != null && this.valueStore != null) {
            return "for each key " + this.keyStore.toString(event, debug) + " and value " + this.valueStore.toString(event, debug) + " in " + this.expression.toString(event, debug);
        }
        if (this.keyStore != null) {
            return "for each key " + this.keyStore.toString(event, debug) + " in " + this.expression.toString(event, debug);
        }
        assert (this.valueStore != null) : "How did we get here?";
        return "for each value " + this.valueStore.toString(event, debug) + " in " + this.expression.toString(event, debug);
    }

    static {
        Skript.registerSection(SecFor.class, "(for [each]|loop) [value] %~object% in %objects%", "(for [each]|loop) (key|index) %~object% in %objects%", "(for [each]|loop) [key|index] %~object%(,| and) [value] %~object% in %objects%");
    }
}

