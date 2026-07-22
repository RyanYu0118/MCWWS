/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.InputSource;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.DefaultClasses;
import ch.njol.skript.util.ClassInfoReference;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

@Name(value="Input")
@Description(value={"Represents the input in a filter expression or sort effect.", "For example, if you ran 'broadcast \"something\" and \"something else\" where [input is \"something\"]", "the condition would be checked twice, using \"something\" and \"something else\" as the inputs.", "The 'input index' pattern can be used when acting on a variable to access the index of the input."})
@Example.Examples(value={@Example(value="send \"congrats on being staff!\" to all players where [input has permission \"staff\"]"), @Example(value="sort {_list::*} based on length of input index")})
@Since(value={"2.2-dev36, 2.9.0 (input index)"})
public class ExprInput<T>
extends SimpleExpression<T> {
    @Nullable
    private final ExprInput<?> source;
    private Class<? extends T>[] types;
    private Class<T> superType;
    private InputSource inputSource;
    @Nullable
    private ClassInfo<?> specifiedType;
    private boolean isIndex = false;

    public ExprInput() {
        this(null, Object.class);
    }

    public ExprInput(@Nullable ExprInput<?> source, Class<? extends T> ... types) {
        this.source = source;
        if (source != null) {
            this.isIndex = source.isIndex;
            this.specifiedType = source.specifiedType;
            this.inputSource = source.inputSource;
            Set<ExprInput<?>> dependentInputs = this.inputSource.getDependentInputs();
            dependentInputs.remove(this.source);
            dependentInputs.add(this);
        }
        this.types = types;
        this.superType = Utils.getSuperType(types);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.inputSource = this.getParser().getData(InputSource.InputData.class).getSource();
        if (this.inputSource == null) {
            return false;
        }
        switch (matchedPattern) {
            case 1: {
                ClassInfoReference classInfoReference = (ClassInfoReference)((Literal)ClassInfoReference.wrap(exprs[0])).getSingle();
                if (classInfoReference.isPlural().isTrue()) {
                    Skript.error("An input can only be a single value! Please use a singular type (for example: players input -> player input).");
                    return false;
                }
                this.specifiedType = classInfoReference.getClassInfo();
                break;
            }
            case 2: {
                if (!this.inputSource.hasIndices()) {
                    Skript.error("You cannot use 'input index' on expressions without indices!");
                    return false;
                }
                this.specifiedType = DefaultClasses.STRING;
                this.isIndex = true;
                break;
            }
            default: {
                this.specifiedType = null;
            }
        }
        if (this.specifiedType != null) {
            this.superType = this.specifiedType.getC();
            this.types = new Class[]{this.superType};
        }
        return true;
    }

    @Override
    protected T[] get(Event event) {
        Object currentValue;
        Object object = currentValue = this.isIndex ? this.inputSource.getCurrentIndex() : this.inputSource.getCurrentValue();
        if (currentValue == null || this.specifiedType != null && !this.specifiedType.getC().isInstance(currentValue)) {
            return (Object[])Array.newInstance(this.superType, 0);
        }
        try {
            return Converters.convert(new Object[]{currentValue}, this.types, this.superType);
        }
        catch (ClassCastException exception) {
            return (Object[])Array.newInstance(this.superType, 0);
        }
    }

    @Override
    public <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        return new ExprInput<R>(this, to);
    }

    @Override
    public Expression<?> getSource() {
        return this.source == null ? this : this.source;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.superType;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        return Arrays.copyOf(this.types, this.types.length);
    }

    @Nullable
    public ClassInfo<?> getSpecifiedType() {
        return this.specifiedType;
    }

    @Override
    public String toString(Event event, boolean debug) {
        if (this.isIndex) {
            return "input index";
        }
        return this.specifiedType == null ? "input" : this.specifiedType.getCodeName() + " input";
    }

    static {
        Skript.registerExpression(ExprInput.class, Object.class, ExpressionType.COMBINED, "input", "%*classinfo% input", "input index");
    }
}

