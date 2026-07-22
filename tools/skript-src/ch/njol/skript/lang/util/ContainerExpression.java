/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.util;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Container;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ContainerExpression
extends SimpleExpression<Object> {
    final Expression<? extends Container<?>> expr;
    private final Class<?> type;

    public ContainerExpression(Expression<? extends Container<?>> expr, Class<?> type) {
        this.expr = expr;
        this.type = type;
    }

    @Override
    protected Object[] get(Event e) {
        throw new UnsupportedOperationException("ContainerExpression must only be used by Loops");
    }

    @Override
    @Nullable
    public Iterator<Object> iterator(Event event) {
        final Iterator iterator = this.expr.iterator(event);
        if (iterator == null) {
            return null;
        }
        return new Iterator<Object>(this){
            @Nullable
            private Iterator<?> current;
            final /* synthetic */ ContainerExpression this$0;
            {
                this.this$0 = this$0;
            }

            @Override
            public boolean hasNext() {
                Iterator<Object> current = this.current;
                while (iterator.hasNext() && (current == null || !current.hasNext())) {
                    current = ((Container)iterator.next()).containerIterator();
                    this.current = current;
                }
                return current != null && current.hasNext();
            }

            @Override
            public Object next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                Iterator<?> current = this.current;
                if (current == null) {
                    throw new NoSuchElementException();
                }
                Object value = current.next();
                assert (value != null) : String.valueOf(this.current) + "; " + String.valueOf(this.this$0.expr);
                return value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<?> getReturnType() {
        return this.type;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.expr.toString(event, debug);
    }
}

