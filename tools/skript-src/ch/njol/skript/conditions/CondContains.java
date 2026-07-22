/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.VerboseAssert;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.common.AnyContains;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import java.lang.invoke.LambdaMetafactory;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converters;

@Name(value="Contains")
@Description(value={"Checks whether an inventory contains an item, a text contains another piece of text, a container contains something, or a list (e.g. {list variable::*} or 'drops') contains another object."})
@Example.Examples(value={@Example(value="block contains 20 cobblestone"), @Example(value="player has 4 flint and 2 iron ingots"), @Example(value="{list::*} contains 5")})
@Since(value={"1.0"})
@Deprecated(since="2.13", forRemoval=true)
public class CondContains
extends Condition
implements VerboseAssert {
    private Expression<?> containers;
    private Expression<?> items;
    private boolean explicitSingle;
    private CheckType checkType;
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.containers = LiteralUtils.defendExpression(exprs[0]);
        this.items = LiteralUtils.defendExpression(exprs[1]);
        this.explicitSingle = matchedPattern == 2 && parseResult.mark != 1 || this.containers.isSingle();
        this.checkType = matchedPattern <= 1 ? CheckType.INVENTORY : CheckType.UNKNOWN;
        this.setNegated(matchedPattern % 2 == 1);
        return LiteralUtils.canInitSafely(this.containers, this.items);
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public boolean check(Event event) {
        block9: {
            block11: {
                block10: {
                    checkType = this.checkType;
                    containerValues = this.containers.getAll(event);
                    if (containerValues.length == 0) {
                        return this.isNegated();
                    }
                    if (checkType != CheckType.UNKNOWN) break block9;
                    if (!Arrays.stream(containerValues).allMatch((Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, isInstance(java.lang.Object ), (Ljava/lang/Object;)Z)(Inventory.class))) break block10;
                    checkType = CheckType.INVENTORY;
                    break block9;
                }
                if (!this.explicitSingle || !Arrays.stream(containerValues).allMatch((Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$check$0(java.lang.Object ), (Ljava/lang/Object;)Z)())) break block11;
                checkType = CheckType.CONTAINER;
                break block9;
            }
            if (!this.explicitSingle) ** GOTO lbl-1000
            if (Arrays.stream(containerValues).allMatch((Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, isInstance(java.lang.Object ), (Ljava/lang/Object;)Z)(String.class))) {
                checkType = CheckType.STRING;
            } else lbl-1000:
            // 2 sources

            {
                checkType = CheckType.OBJECTS;
            }
        }
        switch (checkType.ordinal()) {
            case 1: {
                v0 = SimpleExpression.check(containerValues, (Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$check$1(org.bukkit.event.Event java.lang.Object ), (Ljava/lang/Object;)Z)((CondContains)this, (Event)event), this.isNegated(), this.containers.getAnd());
                break;
            }
            case 0: {
                caseSensitive = SkriptConfig.caseSensitive.value();
                v0 = SimpleExpression.check(containerValues, (Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$check$3(org.bukkit.event.Event boolean java.lang.Object ), (Ljava/lang/Object;)Z)((CondContains)this, (Event)event, (boolean)caseSensitive), this.isNegated(), this.containers.getAnd());
                break;
            }
            case 4: {
                v0 = SimpleExpression.check(containerValues, (Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$check$5(org.bukkit.event.Event java.lang.Object ), (Ljava/lang/Object;)Z)((CondContains)this, (Event)event), this.isNegated(), this.containers.getAnd());
                break;
            }
            default: {
                if (!CondContains.$assertionsDisabled && checkType != CheckType.OBJECTS) {
                    throw new AssertionError();
                }
                v0 = this.items.check(event, (Predicate<Object>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$check$6(java.lang.Object[] java.lang.Object ), (Ljava/lang/Object;)Z)((Object[])containerValues), this.isNegated());
            }
        }
        return v0;
    }

    @Override
    public String getExpectedMessage(Event event) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("to");
        if (this.isNegated()) {
            joiner.add("not");
        }
        joiner.add("find %s".formatted(VerboseAssert.getExpressionValue(this.items, event)));
        return joiner.toString();
    }

    @Override
    public String getReceivedMessage(Event event) {
        StringJoiner joiner = new StringJoiner(" ");
        if (!this.isNegated()) {
            joiner.add("no");
        } else {
            joiner.add("a");
        }
        joiner.add("match in %s".formatted(VerboseAssert.getExpressionValue(this.containers, event)));
        return joiner.toString();
    }

    @Override
    public Condition simplify() {
        if (this.containers instanceof Literal && this.items instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.containers.toString(e, debug) + (this.isNegated() ? " doesn't contain " : " contains ") + this.items.toString(e, debug);
    }

    private static /* synthetic */ boolean lambda$check$6(Object[] containerValues, Object o1) {
        for (Object o2 : containerValues) {
            if (Comparators.compare(o1, o2) != Relation.EQUAL) continue;
            return true;
        }
        return false;
    }

    private /* synthetic */ boolean lambda$check$5(Event event, Object object) {
        AnyContains container = object instanceof AnyContains ? (AnyContains)object : Converters.convert(object, AnyContains.class);
        if (container == null) {
            return false;
        }
        return this.items.check(event, container::checkSafely);
    }

    private /* synthetic */ boolean lambda$check$3(Event event, boolean caseSensitive, Object o) {
        String string = (String)o;
        return this.items.check(event, o1 -> {
            if (o1 instanceof String) {
                String text = (String)o1;
                return StringUtils.contains(string, text, caseSensitive);
            }
            return false;
        });
    }

    private /* synthetic */ boolean lambda$check$1(Event event, Object o) {
        Inventory inventory = (Inventory)o;
        return this.items.check(event, o1 -> {
            if (o1 instanceof ItemType) {
                ItemType type = (ItemType)o1;
                return type.isContainedIn((Iterable<ItemStack>)inventory);
            }
            if (o1 instanceof ItemStack) {
                ItemStack stack = (ItemStack)o1;
                return inventory.containsAtLeast(stack, stack.getAmount());
            }
            if (o1 instanceof Inventory) {
                return Objects.equals(inventory, o1);
            }
            return false;
        });
    }

    private static /* synthetic */ boolean lambda$check$0(Object object) {
        return object instanceof AnyContains || Converters.converterExists(object.getClass(), AnyContains.class);
    }

    static {
        boolean bl = $assertionsDisabled = !CondContains.class.desiredAssertionStatus();
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            Skript.registerCondition(CondContains.class, "%inventories% (has|have) %itemtypes% [in [(the[ir]|his|her|its)] inventory]", "%inventories% (doesn't|does not|do not|don't) have %itemtypes% [in [(the[ir]|his|her|its)] inventory]", "%inventories/strings/objects% contain[(1\u00a6s)] %itemtypes/strings/objects%", "%inventories/strings/objects% (doesn't|does not|do not|don't) contain %itemtypes/strings/objects%");
        }
    }

    private static enum CheckType {
        STRING,
        INVENTORY,
        OBJECTS,
        UNKNOWN,
        CONTAINER;

    }
}

