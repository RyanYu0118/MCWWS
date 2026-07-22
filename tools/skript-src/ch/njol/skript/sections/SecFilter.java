/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.sections;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprInput;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.InputSource;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.Pair;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Filter")
@Description(value={"Filters a variable list based on the supplied conditions. Unlike the filter expression, this effect maintains the indices of the filtered list.", "It also supports filtering based on meeting any of the given criteria, rather than all, like multi-line if statements."})
@Example.Examples(value={@Example(value="set {_a::*} to integers between -10 and 10"), @Example(value="filter {_a::*} to match:\n\tinput is a number\n\tmod(input, 2) = 0\n\tinput > 0\nsend {_a::*} # sends 2, 4, 6, 8, and 10\n")})
@Since(value={"2.10"})
public class SecFilter
extends Section
implements InputSource {
    private @UnknownNullability Variable<?> unfilteredObjects;
    private final List<Condition> conditions = new ArrayList<Condition>();
    private boolean isAny;
    @Nullable
    private Object currentValue;
    private @UnknownNullability String currentIndex;
    private final Set<ExprInput<?>> dependentInputs = new HashSet();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        if (expressions[0].isSingle() || !(expressions[0] instanceof Variable)) {
            Skript.error("You can only filter list variables!");
            return false;
        }
        this.unfilteredObjects = (Variable)expressions[0];
        this.isAny = parseResult.hasTag("any");
        ParserInstance parser = this.getParser();
        if (sectionNode.isEmpty()) {
            Skript.error("filter sections must contain at least one condition");
            return false;
        }
        InputSource.InputData inputData = this.getParser().getData(InputSource.InputData.class);
        InputSource originalSource = inputData.getSource();
        inputData.setSource(this);
        try {
            for (Node childNode : sectionNode) {
                if (!(childNode instanceof SimpleNode)) {
                    Skript.error("Filter sections may not contain other sections");
                    boolean bl = false;
                    return bl;
                }
                String childKey = childNode.getKey();
                if (childKey == null) continue;
                childKey = ScriptLoader.replaceOptions(childKey);
                parser.setNode(childNode);
                Condition condition = Condition.parse(childKey, "Can't understand this condition: '" + childKey + "'");
                parser.setNode(sectionNode);
                if (condition == null) {
                    boolean bl = false;
                    return bl;
                }
                this.conditions.add(condition);
            }
        }
        finally {
            inputData.setSource(originalSource);
        }
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        String varName = this.unfilteredObjects.getName().toString(event);
        String varSubName = StringUtils.substring(varName, 0, -1);
        boolean local = this.unfilteredObjects.isLocal();
        Map rawVariable = (Map)this.unfilteredObjects.getRaw(event);
        if (rawVariable == null) {
            return this.getNext();
        }
        int initialSize = rawVariable.size();
        ArrayList toKeep = new ArrayList();
        ArrayList toRemove = new ArrayList();
        Iterator<Pair<String, Object>> variableIterator = Variables.getVariableIterator(varName, local, event);
        Stream<Pair<String, Object>> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(variableIterator, 16), false);
        if (this.isAny) {
            stream.forEach(pair -> {
                this.currentValue = pair.getValue();
                this.currentIndex = (String)pair.getKey();
                if (this.conditions.stream().anyMatch(c -> c.check(event))) {
                    toKeep.add(pair);
                } else {
                    toRemove.add((String)pair.getKey());
                }
            });
        } else {
            stream.forEach(pair -> {
                this.currentValue = pair.getValue();
                this.currentIndex = (String)pair.getKey();
                if (this.conditions.stream().allMatch(c -> c.check(event))) {
                    toKeep.add(pair);
                } else {
                    toRemove.add((String)pair.getKey());
                }
            });
        }
        if (toKeep.size() < initialSize / 2) {
            Variables.deleteVariable(varName, event, local);
            for (Pair pair2 : toKeep) {
                Variables.setVariable(varSubName + (String)pair2.getKey(), pair2.getValue(), event, local);
            }
        } else {
            for (String index : toRemove) {
                Variables.setVariable(varSubName + index, null, event, local);
            }
        }
        return this.getNext();
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
    public @UnknownNullability String getCurrentIndex() {
        return this.currentIndex;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "filter " + this.unfilteredObjects.toString(event, debug) + " to match " + (this.isAny ? "any" : "all");
    }

    static {
        Skript.registerSection(SecFilter.class, "filter %~objects% to match [:any|all]");
        if (!ParserInstance.isRegistered(InputSource.InputData.class)) {
            ParserInstance.registerData(InputSource.InputData.class, InputSource.InputData::new);
        }
    }
}

