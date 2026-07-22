/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprInput;
import ch.njol.skript.expressions.ExprSortedList;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.InputSource;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Sort")
@Description(value={"Sorts a list variable using either the natural ordering of the contents or the results of the given expression.\nBe warned, this will overwrite the indices of the list variable.\n\nWhen using the full <code>sort %~objects% (by|based on) &lt;expression&gt;</code> pattern,\nthe input expression can be used to refer to the current item being sorted.\n(See input expression for more information.)"})
@Example.Examples(value={@Example(value="set {_words::*} to \"pineapple\", \"banana\", \"yoghurt\", and \"apple\""), @Example(value="sort {_words::*} # alphabetical sort"), @Example(value="sort {_words::*} by length of input # shortest to longest"), @Example(value="sort {_words::*} in descending order by length of input # longest to shortest"), @Example(value="sort {_words::*} based on {tastiness::%input%} # sort based on custom value")})
@Since(value={"2.9.0, 2.10 (sort order)"})
@Keywords(value={"input"})
public class EffSort
extends Effect
implements InputSource {
    @Nullable
    private Expression<?> mappingExpr;
    private @UnknownNullability Variable<?> unsortedObjects;
    private boolean descendingOrder;
    private final Set<ExprInput<?>> dependentInputs = new HashSet();
    @Nullable
    private Object currentValue;
    private @UnknownNullability String currentIndex;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<?> expression;
        if (expressions[0].isSingle() || !((expression = expressions[0]) instanceof Variable)) {
            Skript.error("You can only sort list variables!");
            return false;
        }
        Variable variable = (Variable)expression;
        this.unsortedObjects = variable;
        this.descendingOrder = parseResult.hasTag("descending");
        if (!parseResult.regexes.isEmpty()) {
            @Nullable String unparsedExpression = parseResult.regexes.get(0).group();
            assert (unparsedExpression != null);
            this.mappingExpr = this.parseExpression(unparsedExpression, this.getParser(), 1);
            if (this.mappingExpr == null) {
                return false;
            }
            if (!this.mappingExpr.isSingle()) {
                Skript.error("The mapping expression in the sort effect must only return a single value for a single input.");
                return false;
            }
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object[] sorted;
        int sortingMultiplier;
        int n = sortingMultiplier = this.descendingOrder ? -1 : 1;
        if (this.mappingExpr == null) {
            try {
                sorted = this.unsortedObjects.stream(event).sorted((o1, o2) -> ExprSortedList.compare(o1, o2) * sortingMultiplier).toArray();
            }
            catch (ClassCastException | IllegalArgumentException e) {
                return;
            }
        }
        ArrayList<MappedValue> mappedValues = new ArrayList<MappedValue>();
        Iterator<KeyedValue<?>> it = this.unsortedObjects.keyedIterator(event);
        while (it.hasNext()) {
            KeyedValue<?> keyedValue = it.next();
            this.currentIndex = keyedValue.key();
            this.currentValue = keyedValue.value();
            Object mappedValue = this.mappingExpr.getSingle(event);
            if (mappedValue == null) {
                this.error("Sorting failed because Skript cannot sort null values. The mapping expression '" + this.mappingExpr.toString(event, false) + "' returned a null value when given the input '" + String.valueOf(this.currentValue) + "'.");
                return;
            }
            mappedValues.add(new MappedValue(this.currentValue, mappedValue));
        }
        try {
            sorted = mappedValues.stream().sorted((o1, o2) -> ExprSortedList.compare(o1.mapped(), o2.mapped()) * sortingMultiplier).map(MappedValue::original).toArray();
        }
        catch (ClassCastException | IllegalArgumentException e) {
            return;
        }
        this.unsortedObjects.change(event, sorted, Changer.ChangeMode.SET);
    }

    @Override
    public Set<ExprInput<?>> getDependentInputs() {
        return this.dependentInputs;
    }

    @Override
    @Nullable
    public Object getCurrentValue() {
        return this.currentValue;
    }

    @Override
    public boolean hasIndices() {
        return true;
    }

    @Override
    public @UnknownNullability String getCurrentIndex() {
        return this.currentIndex;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "sort " + this.unsortedObjects.toString(event, debug) + " in " + (this.descendingOrder ? "descending" : "ascending") + " order" + (String)(this.mappingExpr == null ? "" : " by " + this.mappingExpr.toString(event, debug));
    }

    static {
        Skript.registerEffect(EffSort.class, "sort %~objects% [in (:descending|ascending) order] [(by|based on) <.+>]");
        if (!ParserInstance.isRegistered(InputSource.InputData.class)) {
            ParserInstance.registerData(InputSource.InputData.class, InputSource.InputData::new);
        }
    }

    private record MappedValue(Object original, Object mapped) {
    }
}

