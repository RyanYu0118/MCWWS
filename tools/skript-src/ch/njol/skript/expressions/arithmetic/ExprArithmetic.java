/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions.arithmetic;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprArgument;
import ch.njol.skript.expressions.arithmetic.ArithmeticChain;
import ch.njol.skript.expressions.arithmetic.ArithmeticGettable;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.lang.parser.ParsingStack;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;

@Name(value="Arithmetic")
@Description(value={"Arithmetic expressions, e.g. 1 + 2, (health of player - 2) / 3, etc."})
@Example.Examples(value={@Example(value="set the player's health to 10 - the player's health"), @Example(value="loop (argument + 2) / 5 times:\n\tmessage \"Two useless numbers: %loop-num * 2 - 5%, %2^loop-num - 1%\"\n"), @Example(value="message \"You have %health of player * 2% half hearts of HP!\"")})
@Since(value={"1.4.2"})
public class ExprArithmetic<L, R, T>
extends SimpleExpression<T> {
    private static Patterns<PatternInfo> patterns = null;
    private Expression<L> first;
    private Expression<R> second;
    private Operator operator;
    private Class<? extends T> returnType;
    private Collection<Class<?>> knownReturnTypes;
    private final List<Object> chain = new ArrayList<Object>();
    private ArithmeticGettable<? extends T> arithmeticGettable;
    private boolean leftGrouped;
    private boolean rightGrouped;
    private boolean isTopLevel;

    public static void registerExpression() {
        Skript.checkAcceptRegistrations();
        ArrayList<Object[]> infos = new ArrayList<Object[]>();
        for (Operator operator : Arithmetics.getAllOperators()) {
            infos.add(new Object[]{"\\(%object%\\)[ ]" + operator.sign() + "[ ]\\(%object%\\)", new PatternInfo(operator, true, true)});
            infos.add(new Object[]{"\\(%object%\\)[ ]" + operator.sign() + "[ ]%object%", new PatternInfo(operator, true, false)});
            infos.add(new Object[]{"%object%[ ]" + operator.sign() + "[ ]\\(%object%\\)", new PatternInfo(operator, false, true)});
            infos.add(new Object[]{"%object%[ ]" + operator.sign() + "[ ]%object%", new PatternInfo(operator, false, false)});
        }
        Object[][] arr = new Object[infos.size()][];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = (Object[])infos.get(i);
        }
        patterns = new Patterns(arr);
        Skript.registerExpression(ExprArithmetic.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, patterns.getPatterns());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Class<L> firstClass;
        this.first = exprs[0];
        this.second = exprs[1];
        PatternInfo patternInfo = patterns.getInfo(matchedPattern);
        this.leftGrouped = patternInfo.leftGrouped;
        this.rightGrouped = patternInfo.rightGrouped;
        this.operator = patternInfo.operator;
        ParsingStack stack = this.getParser().getParsingStack();
        this.isTopLevel = stack.isEmpty() || stack.peek().getSyntaxElementClass() != ExprArithmetic.class;
        this.printArgWarning(this.first, this.second, this.operator);
        if (this.first instanceof UnparsedLiteral) {
            if (this.second instanceof UnparsedLiteral) {
                for (OperationInfo<?, ?, ?> operation : Arithmetics.getOperations(this.operator)) {
                    Expression convertedSecond;
                    Expression convertedFirst = this.first.getConvertedExpression(operation.left());
                    if (convertedFirst == null || (convertedSecond = this.second.getConvertedExpression(operation.right())) == null) continue;
                    this.first = convertedFirst;
                    this.second = convertedSecond;
                    this.returnType = operation.returnType();
                }
            } else {
                Class<R> secondClass = this.second.getReturnType();
                operations = Arithmetics.lookupRightOperations(this.operator, secondClass);
                if (operations.isEmpty()) {
                    if (secondClass != Object.class) {
                        return this.error(this.first.getReturnType(), secondClass);
                    }
                    this.first = this.first.getConvertedExpression(Object.class);
                } else {
                    this.first = this.first.getConvertedExpression((Class[])operations.stream().map(OperationInfo::left).toArray(Class[]::new));
                }
            }
        } else if (this.second instanceof UnparsedLiteral) {
            firstClass = this.first.getReturnType();
            operations = Arithmetics.lookupLeftOperations(this.operator, firstClass);
            if (operations.isEmpty()) {
                if (firstClass != Object.class) {
                    return this.error(firstClass, this.second.getReturnType());
                }
                this.second = this.second.getConvertedExpression(Object.class);
            } else {
                this.second = this.second.getConvertedExpression((Class[])operations.stream().map(OperationInfo::right).toArray(Class[]::new));
            }
        }
        if (!LiteralUtils.canInitSafely(this.first, this.second)) {
            return false;
        }
        firstClass = this.first.getReturnType();
        Class<R> secondClass = this.second.getReturnType();
        if (firstClass == Object.class || secondClass == Object.class) {
            Object[] returnTypes = null;
            if (firstClass != Object.class || secondClass != Object.class) {
                returnTypes = firstClass == Object.class ? (Class[])Arithmetics.lookupRightOperations(this.operator, secondClass).stream().map(OperationInfo::returnType).toArray(Class[]::new) : (Class[])Arithmetics.lookupLeftOperations(this.operator, firstClass).stream().map(OperationInfo::returnType).toArray(Class[]::new);
            }
            if (returnTypes == null) {
                this.returnType = Object.class;
                this.knownReturnTypes = Arithmetics.getAllReturnTypes(this.operator);
            } else {
                if (returnTypes.length == 0) {
                    return this.error(firstClass, secondClass);
                }
                this.returnType = Classes.getSuperClassInfo(returnTypes).getC();
                this.knownReturnTypes = ImmutableSet.copyOf((Object[])returnTypes);
            }
        } else if (this.returnType == null) {
            OperationInfo<L, R, ?> operationInfo = Arithmetics.lookupOperationInfo(this.operator, firstClass, secondClass);
            if (operationInfo == null) {
                return this.error(firstClass, secondClass);
            }
            this.returnType = operationInfo.returnType();
        }
        if (this.first instanceof ExprArithmetic && !this.leftGrouped) {
            this.chain.addAll(((ExprArithmetic)this.first).chain);
        } else {
            this.chain.add(this.first);
        }
        this.chain.add(this.operator);
        if (this.second instanceof ExprArithmetic && !this.rightGrouped) {
            this.chain.addAll(((ExprArithmetic)this.second).chain);
        } else {
            this.chain.add(this.second);
        }
        this.arithmeticGettable = ArithmeticChain.parse(this.chain);
        return this.arithmeticGettable != null || this.error(firstClass, secondClass);
    }

    private void printArgWarning(Expression<L> first, Expression<R> second, Operator operator) {
        ExprArgument argument;
        if (operator == Operator.SUBTRACTION && !this.rightGrouped && !this.leftGrouped && first instanceof ExprArgument && (argument = (ExprArgument)first).couldCauseArithmeticConfusion() && second instanceof ExprArithmetic) {
            double number;
            Literal secondLiteral;
            Literal literal;
            ExprArithmetic secondArith = (ExprArithmetic)second;
            Expression<L> expression = secondArith.first;
            if (expression instanceof Literal && (literal = (Literal)expression).canReturn(Number.class) && LiteralUtils.canInitSafely(secondLiteral = (Literal)LiteralUtils.defendExpression(literal)) && (number = ((Number)secondLiteral.getSingle()).doubleValue()) == 1.0) {
                Skript.warning("This subtraction is ambiguous and could be interpreted as either the 'first argument' expression ('argument-1') or as subtraction from the argument value ('(argument) - 1'). If you meant to use 'argument-1', omit the hyphen ('arg 1') or use parentheses to clarify your intent.");
            }
        }
    }

    @Override
    protected T[] get(Event event) {
        T result = this.arithmeticGettable.get(event);
        Object[] one = (Object[])Array.newInstance(result == null ? this.returnType : result.getClass(), 1);
        one[0] = result;
        return one;
    }

    private boolean error(Class<?> firstClass, Class<?> secondClass) {
        ClassInfo<?> first = Classes.getSuperClassInfo(firstClass);
        ClassInfo<?> second = Classes.getSuperClassInfo(secondClass);
        if (first.getC() != Object.class && second.getC() != Object.class) {
            Skript.error(this.operator.getName() + " can't be performed on " + first.getName().withIndefiniteArticle() + " and " + second.getName().withIndefiniteArticle());
        }
        return false;
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.returnType;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        if (this.returnType == Object.class) {
            return this.knownReturnTypes.toArray(new Class[0]);
        }
        return super.possibleReturnTypes();
    }

    @Override
    public boolean canReturn(Class<?> returnType) {
        if (this.returnType == Object.class && this.knownReturnTypes.contains(returnType)) {
            return true;
        }
        return super.canReturn(returnType);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object one = this.first.toString(event, debug);
        Object two = this.second.toString(event, debug);
        if (this.leftGrouped) {
            one = "(" + (String)one + ")";
        }
        if (this.rightGrouped) {
            two = "(" + (String)two + ")";
        }
        return (String)one + " " + String.valueOf(this.operator) + " " + (String)two;
    }

    @Override
    public Expression<T> simplify() {
        if (this.isTopLevel) {
            return this.simplifyInternal();
        }
        return this;
    }

    private Expression<T> simplifyInternal() {
        Expression<Object> expression = this.first;
        if (expression instanceof ExprArithmetic) {
            ExprArithmetic firstArith = (ExprArithmetic)expression;
            this.first = firstArith.simplifyInternal();
        } else {
            this.first = this.first.simplify();
        }
        expression = this.second;
        if (expression instanceof ExprArithmetic) {
            ExprArithmetic secondArith = (ExprArithmetic)expression;
            this.second = secondArith.simplifyInternal();
        } else {
            this.second = this.second.simplify();
        }
        if (this.first instanceof Literal && this.second instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    Expression<L> getFirst() {
        return this.first;
    }

    Expression<R> getSecond() {
        return this.second;
    }

    private record PatternInfo(Operator operator, boolean leftGrouped, boolean rightGrouped) {
    }
}

