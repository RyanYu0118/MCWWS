/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.common.AnyValued;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Value")
@Description(value={"Returns the value of something that has a value, e.g. a node in a config.", "The value is automatically converted to the specified type (e.g. text, number) where possible."})
@Example.Examples(value={@Example(value="set {_node} to node \"language\" in the skript config\nbroadcast the text value of {_node}\n"), @Example(value="set {_node} to node \"update check interval\" in the skript config\n\nbroadcast text value of {_node}\n# text value of {_node} = \"12 hours\" (text)\n\nwait for {_node}'s timespan value\n# timespan value of {_node} = 12 hours (duration)\n")})
@Since(value={"2.10 (Nodes), 2.10 (Any)"})
@Deprecated(since="2.13", forRemoval=true)
public class ExprValue
extends SimplePropertyExpression<Object, Object> {
    private boolean isSingle;
    private ClassInfo<?> classInfo;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Literal format;
        switch (pattern) {
            case 0: {
                this.isSingle = true;
            }
            case 1: {
                format = (Literal)expressions[0];
                this.setExpr(expressions[1]);
                break;
            }
            case 2: {
                this.isSingle = true;
            }
            default: {
                format = (Literal)expressions[1];
                this.setExpr(expressions[0]);
            }
        }
        this.classInfo = (ClassInfo)format.getSingle();
        return true;
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof AnyValued) {
            AnyValued valued = (AnyValued)object;
            return valued.convertedValue(this.classInfo);
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case SET: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = Object.class;
                break;
            }
            case RESET: 
            case DELETE: {
                classArray = new Class[]{};
                break;
            }
            default: {
                classArray = null;
            }
        }
        return classArray;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Object newValue = delta != null ? delta[0] : null;
        for (Object object : this.getExpr().getArray(event)) {
            AnyValued valued;
            if (!(object instanceof AnyValued) || !(valued = (AnyValued)object).supportsValueChange()) continue;
            valued.changeValueSafely(newValue);
        }
    }

    @Override
    public Class<?> getReturnType() {
        return this.classInfo.getC();
    }

    @Override
    public boolean isSingle() {
        return this.isSingle;
    }

    @Override
    protected String getPropertyName() {
        return this.classInfo.getCodeName() + " value" + (this.isSingle ? "" : "s");
    }

    static {
        if (!SkriptConfig.useTypeProperties.value().booleanValue()) {
            Skript.registerExpression(ExprValue.class, Object.class, ExpressionType.PROPERTY, "[the] %*classinfo% value of %valued%", "[the] %*classinfo% values of %valueds%", "%valued%'s %*classinfo% value", "%valueds%'[s] %*classinfo% values");
        }
    }
}

