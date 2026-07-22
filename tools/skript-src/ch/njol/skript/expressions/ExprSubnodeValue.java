/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.util.StringMode;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

@Name(value="Value of Subnode")
@Description(value={"Returns the value of an sub-node of the given node, following the provided path.", "The value is automatically converted to the specified type (e.g. text, number) where possible."})
@Example(value="set {_node} to node \"language\" in the skript config\nbroadcast the text value of {_node}\n")
@Since(value={"2.10"})
public class ExprSubnodeValue
extends SimplePropertyExpression<Node, Object> {
    private boolean isSingle;
    private ClassInfo<?> classInfo;
    private Expression<String> pathExpression;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().hasExperiment(Feature.SCRIPT_REFLECTION)) {
            return false;
        }
        this.isSingle = true;
        @NotNull Literal format = (Literal)expressions[0];
        this.pathExpression = expressions[1];
        this.setExpr(expressions[2]);
        this.classInfo = (ClassInfo)format.getSingle();
        return true;
    }

    @Override
    @Nullable
    public Object convert(@Nullable Node node) {
        if (node == null) {
            return null;
        }
        if (!(node instanceof EntryNode)) {
            return null;
        }
        EntryNode entryNode = (EntryNode)node;
        return ExprSubnodeValue.convertedValue(entryNode.getValue(), this.classInfo);
    }

    public static <Converted> Converted convertedValue(@NotNull Object value, @NotNull ClassInfo<Converted> expected) {
        if (expected.getC().isInstance(value)) {
            return expected.getC().cast(value);
        }
        if (expected.getC() == String.class) {
            return (Converted)Classes.toString(value, StringMode.MESSAGE);
        }
        if (value instanceof String) {
            String string = (String)value;
            if (expected.getParser() != null && expected.getParser().canParse(ParseContext.CONFIG)) {
                return expected.getParser().parse(string, ParseContext.CONFIG);
            }
        }
        return Converters.convert(value, expected.getC());
    }

    protected Object[] get(Event event, Node[] source) {
        String path = this.pathExpression.getSingle(event);
        Node node = source[0].getNodeAt(path);
        Object[] array = (Object[])Array.newInstance(this.getReturnType(), 1);
        if (!(node instanceof EntryNode)) {
            return (Object[])Array.newInstance(this.getReturnType(), 0);
        }
        EntryNode entryNode = (EntryNode)node;
        array[0] = this.convert(entryNode);
        return array;
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

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the " + this.getPropertyName() + " at " + this.pathExpression.toString(event, debug) + " in " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprSubnodeValue.class, Object.class, ExpressionType.PROPERTY, "[the] %*classinfo% value [at] %string% (from|in) %node%");
    }
}

