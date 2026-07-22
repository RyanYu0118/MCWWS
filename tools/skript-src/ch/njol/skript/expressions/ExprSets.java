/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
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
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import com.google.common.collect.Lists;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Sets")
@Description(value={"Returns a list of all the values of a type. Useful for looping."})
@Example(value="loop all attribute types:\n\tset loop-value attribute of player to 10\n\tmessage \"Set attribute %loop-value% to 10!\"\n")
@Since(value={"1.0 pre-5, 2.7 (classinfo)"})
public class ExprSets
extends SimpleExpression<Object> {
    @Nullable
    private Supplier<? extends Iterator<?>> supplier;
    private ClassInfo<?> classInfo;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        boolean plural = Utils.getEnglishPlural(parser.expr).getSecond();
        if (!plural && !parser.expr.startsWith("every")) {
            return false;
        }
        this.classInfo = (ClassInfo)((Literal)exprs[0]).getSingle();
        this.supplier = this.classInfo.getSupplier();
        if (this.supplier == null) {
            Skript.error("You cannot get all values of type '" + this.classInfo.getName().getSingular() + "'");
            return false;
        }
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        ArrayList objects = Lists.newArrayList(this.supplier.get());
        return objects.toArray((Object[])Array.newInstance(this.classInfo.getC(), objects.size()));
    }

    @Override
    @Nullable
    public Iterator<?> iterator(Event event) {
        return this.supplier.get();
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<?> getReturnType() {
        return this.classInfo.getC();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "all of the " + this.classInfo.getName().getPlural();
    }

    static {
        Skript.registerExpression(ExprSets.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "[all [[of] the]|the|every] %*classinfo%");
    }
}

