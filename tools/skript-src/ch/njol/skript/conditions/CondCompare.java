/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.VerboseAssert;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.ComparatorInfo;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.util.Cyclical;

@Name(value="Comparison")
@Description(value={"A very general condition, it simply compares two values. Usually you can only compare for equality (e.g. block is/isn't of &lt;type&gt;), but some values can also be compared using greater than/less than. In that case you can also test for whether an object is between two others.", "Note: This is the only element where not all patterns are shown. It has actually another two sets of similar patters, but with <code>(was|were)</code> or <code>will be</code> instead of <code>(is|are)</code> respectively, which check different <a href='#ExprTimeState'>time states</a> of the first expression."})
@Example.Examples(value={@Example(value="the clicked block is a stone slab or a double stone slab"), @Example(value="time in the player's world is greater than 8:00"), @Example(value="the creature is not an enderman or an ender dragon")})
@Since(value={"1.0"})
public class CondCompare
extends Condition
implements VerboseAssert {
    private static final Patterns<Relation> patterns = new Patterns(new Object[][]{{"(1\u00a6neither|) %objects% ((is|are)(|2\u00a6(n't| not|4\u00a6 neither)) ((greater|more|higher|bigger|larger) than|above)|\\>) %objects%", Relation.GREATER}, {"(1\u00a6neither|) %objects% ((is|are)(|2\u00a6(n't| not|4\u00a6 neither)) (greater|more|higher|bigger|larger|above) [than] or (equal to|the same as)|\\>=) %objects%", Relation.GREATER_OR_EQUAL}, {"(1\u00a6neither|) %objects% ((is|are)(|2\u00a6(n't| not|4\u00a6 neither)) ((less|smaller|lower) than|below)|\\<) %objects%", Relation.SMALLER}, {"(1\u00a6neither|) %objects% ((is|are)(|2\u00a6(n't| not|4\u00a6 neither)) (less|smaller|lower|below) [than] or (equal to|the same as)|\\<=) %objects%", Relation.SMALLER_OR_EQUAL}, {"(1\u00a6neither|) %objects% (2\u00a6)((is|are) (not|4\u00a6neither)|isn't|aren't|!=) [equal to] %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects% (is|are|=) [(equal to|the same as)] %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects% (is|are) between %objects% and %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects% (2\u00a6)(is not|are not|isn't|aren't) between %objects% and %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@-1% (was|were)(|2\u00a6(n't| not|4\u00a6 neither)) ((greater|more|higher|bigger|larger) than|above) %objects%", Relation.GREATER}, {"(1\u00a6neither|) %objects@-1% (was|were)(|2\u00a6(n't| not|4\u00a6 neither)) (greater|more|higher|bigger|larger|above) [than] or (equal to|the same as) %objects%", Relation.GREATER_OR_EQUAL}, {"(1\u00a6neither|) %objects@-1% (was|were)(|2\u00a6(n't| not|4\u00a6 neither)) ((less|smaller|lower) than|below) %objects%", Relation.SMALLER}, {"(1\u00a6neither|) %objects@-1% (was|were)(|2\u00a6(n't| not|4\u00a6 neither)) (less|smaller|lower|below) [than] or (equal to|the same as) %objects%", Relation.SMALLER_OR_EQUAL}, {"(1\u00a6neither|) %objects@-1% (2\u00a6)((was|were) (not|4\u00a6neither)|wasn't|weren't) [equal to] %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@-1% (was|were) [(equal to|the same as)] %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@-1% (was|were) between %objects% and %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@-1% (2\u00a6)(was not|were not|wasn't|weren't) between %objects% and %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@1% (will be|2\u00a6(will (not|4\u00a6neither) be|won't be)) ((greater|more|higher|bigger|larger) than|above) %objects%", Relation.GREATER}, {"(1\u00a6neither|) %objects@1% (will be|2\u00a6(will (not|4\u00a6neither) be|won't be)) (greater|more|higher|bigger|larger|above) [than] or (equal to|the same as) %objects%", Relation.GREATER_OR_EQUAL}, {"(1\u00a6neither|) %objects@1% (will be|2\u00a6(will (not|4\u00a6neither) be|won't be)) ((less|smaller|lower) than|below) %objects%", Relation.SMALLER}, {"(1\u00a6neither|) %objects@1% (will be|2\u00a6(will (not|4\u00a6neither) be|won't be)) (less|smaller|lower|below) [than] or (equal to|the same as) %objects%", Relation.SMALLER_OR_EQUAL}, {"(1\u00a6neither|) %objects@1% (2\u00a6)((will (not|4\u00a6neither) be|won't be)|(isn't|aren't|is not|are not) (turning|changing) [in]to) [equal to] %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@1% (will be [(equal to|the same as)]|(is|are) (turning|changing) [in]to) %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@1% will be between %objects% and %objects%", Relation.EQUAL}, {"(1\u00a6neither|) %objects@1% (2\u00a6)(will not be|won't be) between %objects% and %objects%", Relation.EQUAL}});
    private Expression<?> first;
    private Expression<?> second;
    @Nullable
    private Expression<?> third;
    private Relation relation;
    @Nullable
    private Comparator comparator;

    @Override
    public boolean init(Expression<?>[] vars, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.first = vars[0];
        this.second = vars[1];
        if (vars.length == 3) {
            this.third = vars[2];
        }
        this.relation = patterns.getInfo(matchedPattern);
        if ((parser.mark & 2) != 0) {
            this.setNegated(true);
        }
        if ((parser.mark & 1) != 0) {
            this.setNegated(!this.isNegated());
        }
        if ((parser.mark & 4) != 0) {
            if (this.second instanceof ExpressionList) {
                ((ExpressionList)this.second).invertAnd();
            }
            if (this.third instanceof ExpressionList) {
                ((ExpressionList)this.third).invertAnd();
            }
        }
        boolean b = this.init(parser.expr);
        Expression<?> third = this.third;
        if (!b) {
            if (third == null && this.first.getReturnType() == Object.class && this.second.getReturnType() == Object.class) {
                return false;
            }
            Skript.error("Can't compare " + CondCompare.f(this.first) + " with " + CondCompare.f(this.second) + (String)(third == null ? "" : " and " + CondCompare.f(third)), ErrorQuality.NOT_AN_EXPRESSION);
            return false;
        }
        Comparator comp = this.comparator;
        if (comp != null) {
            if (third == null) {
                if (!this.relation.isImpliedBy(Relation.EQUAL, Relation.NOT_EQUAL) && !comp.supportsOrdering()) {
                    Skript.error("Can't test " + CondCompare.f(this.first) + " for being '" + String.valueOf((Object)this.relation) + "' " + CondCompare.f(this.second), ErrorQuality.NOT_AN_EXPRESSION);
                    return false;
                }
            } else if (!comp.supportsOrdering()) {
                Skript.error("Can't test " + CondCompare.f(this.first) + " for being 'between' " + CondCompare.f(this.second) + " and " + CondCompare.f(third), ErrorQuality.NOT_AN_EXPRESSION);
                return false;
            }
        }
        return true;
    }

    public static String f(Expression<?> e) {
        if (e.getReturnType() == Object.class) {
            return e.toString(null, false);
        }
        return Classes.getSuperClassInfo(e.getReturnType()).getName().withIndefiniteArticle();
    }

    private boolean init(String expr) {
        Class<?> secondReturnType;
        Expression<?> third = this.third;
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            Expression<Object> expression;
            if (this.first.getReturnType() == Object.class) {
                expression = null;
                if (this.first instanceof UnparsedLiteral) {
                    expression = this.attemptReconstruction((UnparsedLiteral)this.first, this.second);
                }
                if (expression == null) {
                    expression = this.first.getConvertedExpression(Object.class);
                }
                if (expression == null) {
                    log.printError();
                    boolean bl = false;
                    return bl;
                }
                this.first = expression;
            }
            if (this.second.getReturnType() == Object.class) {
                expression = null;
                if (this.second instanceof UnparsedLiteral) {
                    expression = this.attemptReconstruction((UnparsedLiteral)this.second, this.first);
                }
                if (expression == null) {
                    expression = this.second.getConvertedExpression(Object.class);
                }
                if (expression == null) {
                    log.printError();
                    boolean bl = false;
                    return bl;
                }
                this.second = expression;
            }
            if (third != null && third.getReturnType() == Object.class) {
                expression = null;
                if (third instanceof UnparsedLiteral) {
                    expression = this.attemptReconstruction((UnparsedLiteral)third, this.first);
                }
                if (expression == null) {
                    expression = third.getConvertedExpression(Object.class);
                }
                if (expression == null) {
                    log.printError();
                    boolean bl = false;
                    return bl;
                }
                third = expression;
                this.third = third;
            }
            log.printLog(false);
        }
        Class<?> firstReturnType = this.first.getReturnType();
        Class<?> clazz = secondReturnType = third == null ? this.second.getReturnType() : Utils.getSuperType(this.second.getReturnType(), third.getReturnType());
        if (firstReturnType == Object.class || secondReturnType == Object.class) {
            return true;
        }
        this.comparator = Comparators.getComparator(firstReturnType, secondReturnType);
        if (this.comparator == null) {
            SimpleLiteral<?> reparsedSecond = this.reparseLiteral(firstReturnType, this.second);
            if (reparsedSecond != null) {
                this.second = reparsedSecond;
                this.comparator = Comparators.getComparator(firstReturnType, this.second.getReturnType());
            } else {
                SimpleLiteral<?> reparsedFirst = this.reparseLiteral(this.second.getReturnType(), this.first);
                if (reparsedFirst != null) {
                    this.first = reparsedFirst;
                    this.comparator = Comparators.getComparator(this.first.getReturnType(), secondReturnType);
                }
            }
        }
        return this.comparator != null;
    }

    @Nullable
    private <T> SimpleLiteral<T> reparseLiteral(Class<T> type, Expression<?> expression) {
        Expression<?> source = expression;
        if (expression instanceof SimpleLiteral) {
            source = expression.getSource();
        }
        if (source instanceof UnparsedLiteral) {
            UnparsedLiteral unparsedLiteral = (UnparsedLiteral)source;
            return unparsedLiteral.reparse(type);
        }
        return null;
    }

    private Literal<?> attemptReconstruction(UnparsedLiteral one, Expression<?> two) {
        SimpleLiteral<?> expression = null;
        expression = one.getConvertedExpression(new Class[]{Number.class});
        if (expression == null) {
            for (ClassInfo<?> classinfo : Classes.getClassInfos()) {
                ComparatorInfo<?, ?> comparator;
                if (classinfo.getParser() != null && (comparator = Comparators.getComparatorInfo(two.getReturnType(), classinfo.getC())) != null && comparator.getFirstType() != Object.class && (expression = this.reparseLiteral(classinfo.getC(), one)) != null) break;
            }
        }
        if (expression == null) {
            expression = one.getConvertedExpression(new Class[]{two.getReturnType()});
        }
        return expression;
    }

    @Override
    public boolean check(Event event) {
        Expression<?> third = this.third;
        if (this.relation == Relation.EQUAL && third == null && this.first.getAnd() && !this.first.isSingle() && this.second.getAnd() && !this.second.isSingle()) {
            return this.compareLists(event);
        }
        return this.first.check(event, o1 -> this.second.check(event, o2 -> {
            if (third == null) {
                return this.relation.isImpliedBy(this.comparator != null ? this.comparator.compare(o1, o2) : Comparators.compare(o1, o2));
            }
            return third.check(event, o3 -> {
                boolean isBetween = this.comparator != null ? (o1 instanceof Cyclical && o2 instanceof Cyclical && o3 instanceof Cyclical ? (Relation.GREATER_OR_EQUAL.isImpliedBy(this.comparator.compare(o2, o3)) ? Relation.GREATER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o2)) || Relation.SMALLER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o3)) : Relation.GREATER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o2)) && Relation.SMALLER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o3))) : Relation.GREATER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o2)) && Relation.SMALLER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o3)) || Relation.GREATER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o3)) && Relation.SMALLER_OR_EQUAL.isImpliedBy(this.comparator.compare(o1, o2))) : (o1 instanceof Cyclical && o2 instanceof Cyclical && o3 instanceof Cyclical ? (Relation.GREATER_OR_EQUAL.isImpliedBy(Comparators.compare(o2, o3)) ? Relation.GREATER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o2)) || Relation.SMALLER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o3)) : Relation.GREATER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o2)) && Relation.SMALLER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o3))) : Relation.GREATER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o2)) && Relation.SMALLER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o3)) || Relation.GREATER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o3)) && Relation.SMALLER_OR_EQUAL.isImpliedBy(Comparators.compare(o1, o2)));
                return this.relation == Relation.NOT_EQUAL ^ isBetween;
            });
        }), this.isNegated());
    }

    @Override
    public String getExpectedMessage(Event event) {
        Object message = "a value ";
        if (this.third == null) {
            return (String)message + (this.isNegated() ? "not " : "") + String.valueOf((Object)this.relation) + " " + VerboseAssert.getExpressionValue(this.second, event);
        }
        if (this.isNegated()) {
            message = (String)message + "not ";
        }
        message = (String)message + "between " + VerboseAssert.getExpressionValue(this.second, event) + " and " + VerboseAssert.getExpressionValue(this.third, event);
        return message;
    }

    @Override
    public String getReceivedMessage(Event event) {
        return VerboseAssert.getExpressionValue(this.first, event);
    }

    private boolean compareLists(Event event) {
        boolean shouldMatch;
        ?[] first = this.first.getArray(event);
        ?[] second = this.second.getArray(event);
        boolean bl = shouldMatch = !this.isNegated();
        if (first.length != second.length) {
            return !shouldMatch;
        }
        for (int i = 0; i < first.length; ++i) {
            if (this.relation.isImpliedBy(this.comparator != null ? this.comparator.compare(first[i], second[i]) : Comparators.compare(first[i], second[i]))) continue;
            return !shouldMatch;
        }
        return shouldMatch;
    }

    @Override
    public Condition simplify() {
        if (this.first instanceof Literal && this.second instanceof Literal && (this.third == null || this.third instanceof Literal)) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Expression<?> third = this.third;
        String s = third == null ? this.first.toString(event, debug) + " is " + (this.isNegated() ? "not " : "") + String.valueOf((Object)this.relation) + " " + this.second.toString(event, debug) : this.first.toString(event, debug) + " is " + (this.isNegated() ? "not " : "") + "between " + this.second.toString(event, debug) + " and " + third.toString(event, debug);
        if (debug) {
            s = s + " (comparator: " + String.valueOf(this.comparator) + ")";
        }
        return s;
    }

    static {
        Skript.registerCondition(CondCompare.class, Condition.ConditionType.PATTERN_MATCHES_EVERYTHING, patterns.getPatterns());
    }
}

