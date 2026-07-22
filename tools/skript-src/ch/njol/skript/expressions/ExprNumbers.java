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
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Numbers")
@Description(value={"All numbers between two given numbers, useful for looping.", "Use 'numbers' if your start is not an integer and you want to keep the fractional part of the start number constant, or use 'integers' if you only want to loop integers.", "You may also use 'decimals' if you want to use the decimal precision of the start number.", "You may want to use the 'times' expression instead, for instance 'loop 5 times:'"})
@Example.Examples(value={@Example(value="loop numbers from 2.5 to 5.5: # loops 2.5, 3.5, 4.5, 5.5"), @Example(value="loop integers from 2.9 to 5.1: # same as '3 to 5', i.e. loops 3, 4, 5"), @Example(value="loop decimals from 3.94 to 4: # loops 3.94, 3.95, 3.96, 3.97, 3.98, 3.99, 4")})
@Since(value={"1.4.6 (integers & numbers), 2.5.1 (decimals)"})
public class ExprNumbers
extends SimpleExpression<Number> {
    private Expression<Number> start;
    private Expression<Number> end;
    private int mode;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.start = exprs[0];
        this.end = exprs[1];
        this.mode = parseResult.mark;
        return true;
    }

    @Nullable
    protected Number[] get(Event event) {
        boolean reverse;
        Number s = this.start.getSingle(event);
        Number f = this.end.getSingle(event);
        if (s == null || f == null) {
            return null;
        }
        boolean bl = reverse = s.doubleValue() > f.doubleValue();
        if (reverse) {
            Number temp = s;
            s = f;
            f = temp;
        }
        ArrayList<Number> list = new ArrayList<Number>();
        if (this.mode == 0) {
            double amount = Math.floor(f.doubleValue() - s.doubleValue() + 1.0);
            int i = 0;
            while ((double)i < amount) {
                list.add(s.doubleValue() + (double)i);
                ++i;
            }
        } else if (this.mode == 1) {
            double amount = Math.floor(f.doubleValue()) - Math.ceil(s.doubleValue()) + 1.0;
            double low = Math.ceil(s.doubleValue());
            int i = 0;
            while ((double)i < amount) {
                list.add((long)low + (long)i);
                ++i;
            }
        } else if (this.mode == 2) {
            String[] split = (reverse ? (Number)f : (Number)s).toString().split("\\.");
            int numberAccuracy = SkriptConfig.numberAccuracy.value();
            int precision = Math.min(split.length > 1 ? split[1].length() : 0, numberAccuracy);
            double multiplier = Math.pow(10.0, precision);
            int i = (int)Math.ceil(s.doubleValue() * multiplier);
            while ((double)i <= Math.floor(f.doubleValue() * multiplier)) {
                list.add((double)i / multiplier);
                ++i;
            }
        }
        if (reverse) {
            Collections.reverse(list);
        }
        return list.toArray(new Number[0]);
    }

    @Override
    @Nullable
    public Iterator<Number> iterator(Event event) {
        boolean reverse;
        Number s = this.start.getSingle(event);
        Number f = this.end.getSingle(event);
        if (s == null || f == null) {
            return null;
        }
        boolean bl = reverse = s.doubleValue() > f.doubleValue();
        if (reverse) {
            Number temp = s;
            s = f;
            f = temp;
        }
        final Number starting = s;
        final Number finish = f;
        if (this.mode < 2) {
            return new Iterator<Number>(){
                double i;
                double max;
                final /* synthetic */ ExprNumbers this$0;
                {
                    this.this$0 = this$0;
                    this.i = this.this$0.mode == 1 ? Math.ceil(starting.doubleValue()) : starting.doubleValue();
                    this.max = this.this$0.mode == 1 ? Math.floor(finish.doubleValue()) : finish.doubleValue();
                }

                @Override
                public boolean hasNext() {
                    return this.i <= this.max;
                }

                @Override
                public Number next() {
                    double d;
                    if (!this.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    if (this.this$0.mode == 1) {
                        double d2;
                        if (reverse) {
                            double d3 = this.max;
                            d2 = d3;
                            this.max = d3 - 1.0;
                        } else {
                            double d4 = this.i;
                            d2 = d4;
                            this.i = d4 + 1.0;
                        }
                        return (long)d2;
                    }
                    if (reverse) {
                        double d5 = this.max;
                        d = d5;
                        this.max = d5 - 1.0;
                    } else {
                        double d6 = this.i;
                        d = d6;
                        this.i = d6 + 1.0;
                    }
                    return d;
                }
            };
        }
        return new Iterator<Number>(){
            final double min;
            final double max;
            final String[] split;
            final int numberAccuracy;
            final int precision;
            final double multiplier;
            final int intMax;
            final int intMin;
            int current;
            {
                this.min = starting.doubleValue();
                this.max = finish.doubleValue();
                this.split = (reverse ? (Number)finish : (Number)starting).toString().split("\\.");
                this.numberAccuracy = SkriptConfig.numberAccuracy.value();
                this.precision = Math.min(this.split.length > 1 ? this.split[1].length() : 0, this.numberAccuracy);
                this.multiplier = Math.pow(10.0, this.precision);
                this.intMax = (int)Math.floor(this.max * this.multiplier);
                this.intMin = (int)Math.ceil(this.min * this.multiplier);
                this.current = reverse ? this.intMax : this.intMin;
            }

            @Override
            public boolean hasNext() {
                return reverse ? this.current >= this.intMin : this.current <= this.intMax;
            }

            @Override
            public Number next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                double value = (double)this.current / this.multiplier;
                this.current += reverse ? -1 : 1;
                return value;
            }
        };
    }

    @Override
    public boolean isLoopOf(String s) {
        return this.mode == 1 && (s.equalsIgnoreCase("integer") || s.equalsIgnoreCase("int"));
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return this.mode == 1 ? Long.class : Double.class;
    }

    @Override
    public Expression<? extends Number> simplify() {
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        String modeString = this.mode == 0 ? "numbers" : (this.mode == 1 ? "integers" : "decimals");
        return modeString + " from " + this.start.toString(e, debug) + " to " + this.end.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprNumbers.class, Number.class, ExpressionType.COMBINED, "[(all [[of] the]|the)] (numbers|1\u00a6integers|2\u00a6decimals) (between|from) %number% (and|to) %number%");
    }
}

