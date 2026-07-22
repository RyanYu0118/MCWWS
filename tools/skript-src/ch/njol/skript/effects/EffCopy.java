/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.HintManager;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Copy Into Variable")
@Description(value={"Copies objects into a variable. When copying a list over to another list, the source list and its sublists are also copied over.", "<strong>Note: Copying a value into a variable/list will overwrite the existing data.</strong>"})
@Example(value="set {_foo::bar} to 1\nset {_foo::sublist::foobar} to \"hey\"\ncopy {_foo::*} to {_copy::*}\nbroadcast indices of {_copy::*} # bar, sublist\nbroadcast {_copy::bar} # 1\nbroadcast {_copy::sublist::foobar} # \"hey!\"\n")
@Since(value={"2.8.0"})
@Keywords(value={"clone", "variable", "list"})
public class EffCopy
extends Effect {
    private Expression<?> source;
    private Expression<?> rawDestination;
    private List<Variable<?>> destinations;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.source = exprs[0];
        this.rawDestination = exprs[1];
        if (exprs[1] instanceof Variable) {
            this.destinations = Collections.singletonList((Variable)exprs[1]);
        } else if (exprs[1] instanceof ExpressionList) {
            this.destinations = EffCopy.unwrapExpressionList((ExpressionList)exprs[1]);
        }
        if (this.destinations == null) {
            Skript.error("You can only copy objects into variables");
            return false;
        }
        for (Variable<?> destination : this.destinations) {
            if (this.source.isSingle() || !destination.isSingle()) continue;
            Skript.error("Cannot copy multiple objects into a single variable");
            return false;
        }
        Class<?>[] sourceHints = this.source.possibleReturnTypes();
        HintManager hintManager = this.getParser().getHintManager();
        for (Variable<?> destination : this.destinations) {
            if (!HintManager.canUseHints(destination)) continue;
            hintManager.set(destination, sourceHints);
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (!(this.source instanceof Variable) || this.source.isSingle()) {
            Changer.ChangeMode mode = Changer.ChangeMode.SET;
            Object[] clone = (Object[])Classes.clone(this.source.getArray(event));
            if (clone.length == 0) {
                mode = Changer.ChangeMode.DELETE;
            }
            for (Variable<?> dest : this.destinations) {
                dest.change(event, clone, mode);
            }
            return;
        }
        Map<String, Object> source = EffCopy.copyMap((Map)((Variable)this.source).getRaw(event));
        if (source != null) {
            source.remove(null);
        }
        for (Variable<?> destination : this.destinations) {
            destination.change(event, null, Changer.ChangeMode.DELETE);
            if (source == null) continue;
            String target = destination.getName().getSingle(event);
            target = target.substring(0, target.length() - "::*".length());
            EffCopy.set(event, target, source, destination.isLocal());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "copy " + this.source.toString(event, debug) + " into " + this.rawDestination.toString(event, debug);
    }

    @Nullable
    private static Map<String, Object> copyMap(@Nullable Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        HashMap<String, Object> copy = new HashMap<String, Object>(map.size());
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                copy.put((String)key, EffCopy.copyMap((Map)value));
                return;
            }
            copy.put((String)key, Classes.clone(value));
        });
        return copy;
    }

    private static void set(Event event, String targetName, Map<String, Object> source, boolean local) {
        source.forEach((key, value) -> {
            String node = targetName + (String)(key == null ? "" : "::" + key);
            if (value instanceof Map) {
                EffCopy.set(event, node, (Map)value, local);
                return;
            }
            Variables.setVariable(node, value, event, local);
        });
    }

    private static List<Variable<?>> unwrapExpressionList(ExpressionList<?> expressionList) {
        Expression<?>[] expressions = expressionList.getExpressions();
        ArrayList destinations = new ArrayList();
        for (Expression<?> expression : expressions) {
            if (expression instanceof Variable) {
                destinations.add((Variable)expression);
                continue;
            }
            if (!(expression instanceof ExpressionList)) {
                return null;
            }
            destinations.addAll(EffCopy.unwrapExpressionList((ExpressionList)expression));
        }
        return destinations;
    }

    static {
        Skript.registerEffect(EffCopy.class, "copy %~objects% [in]to %~objects%");
    }
}

