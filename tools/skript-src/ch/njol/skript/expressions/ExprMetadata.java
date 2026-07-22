/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.metadata.Metadatable
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.OperationInfo;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.converter.Converters;

@Name(value="Metadata")
@Description(value={"Metadata is a way to store temporary data on entities, blocks and more that disappears after a server restart."})
@Example.Examples(value={@Example(value="set metadata value \"healer\" of player to true"), @Example(value="broadcast \"%metadata value \"healer\" of player%\""), @Example(value="clear metadata value \"healer\" of player")})
@Since(value={"2.2-dev36, 2.10 (add, remove)"})
public class ExprMetadata<T>
extends SimpleExpression<T> {
    private final ExprMetadata<?> source;
    private final Class<? extends T>[] types;
    private final Class<T> superType;
    private @UnknownNullability Expression<String> keys;
    private @UnknownNullability Expression<Metadatable> holders;

    public ExprMetadata() {
        this(null, Object.class);
    }

    @SafeVarargs
    private ExprMetadata(ExprMetadata<?> source, Class<? extends T> ... types) {
        this.source = source;
        if (source != null) {
            this.keys = source.keys;
            this.holders = source.holders;
        }
        this.types = types;
        this.superType = Utils.getSuperType(types);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.holders = exprs[matchedPattern ^ 1];
        this.keys = exprs[matchedPattern];
        return true;
    }

    @Override
    protected T @Nullable [] get(Event event) {
        ArrayList<Object> values = new ArrayList<Object>();
        String[] keys = this.keys.getArray(event);
        for (Metadatable holder : this.holders.getArray(event)) {
            for (String key : keys) {
                List metadata = holder.getMetadata(key);
                if (metadata.isEmpty()) continue;
                values.add(((MetadataValue)metadata.get(metadata.size() - 1)).value());
            }
        }
        try {
            return Converters.convert(values.toArray(), this.types, this.superType);
        }
        catch (ClassCastException exception) {
            return (Object[])Array.newInstance(this.superType, 0);
        }
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.DELETE -> CollectionUtils.array(Object.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        String[] keys = this.keys.getArray(event);
        for (Metadatable holder : this.holders.getArray(event)) {
            block6: for (String key : keys) {
                switch (mode) {
                    case SET: {
                        holder.setMetadata(key, (MetadataValue)new FixedMetadataValue((Plugin)Skript.getInstance(), delta[0]));
                        continue block6;
                    }
                    case ADD: 
                    case REMOVE: {
                        OperationInfo<?, ?, ?> info;
                        Object value;
                        assert (delta != null);
                        Operator operator = mode == Changer.ChangeMode.ADD ? Operator.ADDITION : Operator.SUBTRACTION;
                        List metadata = holder.getMetadata(key);
                        Object object = value = metadata.isEmpty() ? null : ((MetadataValue)metadata.get(metadata.size() - 1)).value();
                        if (value != null ? (info = Arithmetics.getOperationInfo(operator, value.getClass(), delta[0].getClass())) == null : (info = Arithmetics.getOperationInfo(operator, delta[0].getClass(), delta[0].getClass())) == null || (value = Arithmetics.getDefaultValue(info.left())) == null) continue block6;
                        Object newValue = info.operation().calculate(value, delta[0]);
                        holder.setMetadata(key, (MetadataValue)new FixedMetadataValue((Plugin)Skript.getInstance(), newValue));
                        continue block6;
                    }
                    case DELETE: {
                        holder.removeMetadata(key, (Plugin)Skript.getInstance());
                    }
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.holders.isSingle() && this.keys.isSingle();
    }

    @Override
    public Class<? extends T> getReturnType() {
        return this.superType;
    }

    @Override
    public Class<? extends T>[] possibleReturnTypes() {
        return Arrays.copyOf(this.types, this.types.length);
    }

    @Override
    @SafeVarargs
    public final <R> Expression<? extends R> getConvertedExpression(Class<R> ... to) {
        return new ExprMetadata<R>(this, to);
    }

    @Override
    public Expression<?> getSource() {
        return this.source == null ? this : this.source;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "metadata values " + this.keys.toString(event, debug) + " of " + this.holders.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprMetadata.class, Object.class, ExpressionType.PROPERTY, "metadata [(value|tag)[s]] %strings% of %metadataholders%", "%metadataholders%'[s] metadata [(value|tag)[s]] %string%");
    }
}

