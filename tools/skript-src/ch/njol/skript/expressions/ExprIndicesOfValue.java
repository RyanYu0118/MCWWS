/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.ArrayUtils
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
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyedValue;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Indices of Value")
@Description(value={"Get the first, last or all positions of a character (or text) in another text using 'positions of %texts% in %text%'. Nothing is returned when the value does not occur in the text. Positions range from 1 to the <a href='#ExprIndicesOf'>length</a> of the text (inclusive).", "", "Using 'indices/positions of %objects% in %objects%', you can get the indices or positions of a list where the value at that index is the provided value. Indices are only supported for keyed expressions (e.g. variable lists) and will return the string indices of the given value. Positions can be used with any list and will return the numerical position of the value in the list, counting up from 1. Additionally, nothing is returned if the value is not found in the list.", "", "Whether string comparison is case-sensitive or not can be configured in Skript's config file."})
@Example.Examples(value={@Example(value="set {_first} to the first position of \"@\" in the text argument\nif {_s} contains \"abc\":\n\tset {_s} to the first (position of \"abc\" in {_s} + 3) characters of {_s}\n\t# removes everything after the first \"abc\" from {_s}\n"), @Example(value="set {_list::*} to 1, 2, 3, 1, 2, 3\nset {_indices::*} to indices of the value 1 in {_list::*}\n# {_indices::*} is now \"1\" and \"4\"\n\nset {_indices::*} to all indices of the value 2 in {_list::*}\n# {_indices::*} is now \"2\" and \"5\"\n\nset {_positions::*} to all positions of the value 3 in {_list::*}\n# {_positions::*} is now 3 and 6\n"), @Example(value="set {_otherlist::bar} to 100\nset {_otherlist::hello} to \"hi\"\nset {_otherlist::burb} to 100\nset {_otherlist::tud} to \"hi\"\nset {_otherlist::foo} to 100\n\nset {_indices::*} to the first index of the value 100 in {_otherlist::*}\n# {_indices::*} is now \"bar\"\n\nset {_indices::*} to the last index of the value 100 in {_otherlist::*}\n# {_indices::*} is now \"foo\"\n\nset {_positions::*} to all positions of the value 100 in {_otherlist::*}\n# {_positions::*} is now 1, 3 and 5\n\nset {_positions::*} to all positions of the value \"hi\" in {_otherlist::*}\n# {_positions::*} is now 2 and 4\n")})
@Since(value={"2.1, 2.12 (indices, positions of list)"})
public class ExprIndicesOfValue
extends SimpleExpression<Object> {
    private IndexType indexType;
    private boolean position;
    private boolean string;
    private Expression<?> needle;
    private Expression<?> haystack;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (exprs[1].isSingle() && matchedPattern > 0) {
            Skript.error("'" + String.valueOf(exprs[1]) + "' can only ever have one value at most, thus the 'indices of x in list' expression has no effect.");
            return false;
        }
        if (!KeyProviderExpression.canReturnKeys(exprs[1]) && matchedPattern == 2) {
            Skript.error("'" + String.valueOf(exprs[1]) + "' is not a keyed expression. You can only get the indices of a keyed expression.");
            return false;
        }
        this.indexType = IndexType.values()[parseResult.mark == 0 ? 0 : parseResult.mark - 1];
        if (parseResult.mark == 0 && parseResult.hasTag("mult")) {
            this.indexType = IndexType.ALL;
        }
        this.position = matchedPattern <= 1;
        this.string = matchedPattern == 0;
        this.needle = LiteralUtils.defendExpression(exprs[0]);
        this.haystack = exprs[1];
        return LiteralUtils.canInitSafely(this.needle);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        Object[] needle = this.needle.getAll(event);
        if (needle.length == 0) {
            return this.position ? new Long[]{} : new String[]{};
        }
        if (!this.position) {
            assert (this.haystack instanceof KeyProviderExpression);
            return this.getIndices((KeyProviderExpression)this.haystack, needle, event);
        }
        if (!this.string) {
            return this.getListPositions(this.haystack, needle, event);
        }
        String haystack = (String)this.haystack.getSingle(event);
        if (haystack == null) {
            return new Long[0];
        }
        return this.getStringPositions(haystack, (String[])needle);
    }

    private Long[] getStringPositions(String haystack, String[] needles) {
        boolean caseSensitive = SkriptConfig.caseSensitive.value();
        ArrayList<Long> positions = new ArrayList<Long>();
        block5: for (String needle : needles) {
            long position = StringUtils.indexOf(haystack, needle, caseSensitive);
            if (position == -1L) continue;
            switch (this.indexType.ordinal()) {
                case 0: {
                    positions.add(position + 1L);
                    continue block5;
                }
                case 1: {
                    positions.add(Long.valueOf(StringUtils.lastIndexOf(haystack, needle, caseSensitive)));
                    continue block5;
                }
                case 2: {
                    do {
                        positions.add(position + 1L);
                    } while ((position = (long)StringUtils.indexOf(haystack, needle, (int)position + 1, caseSensitive)) != -1L);
                }
            }
        }
        return (Long[])positions.toArray(Long[]::new);
    }

    private <Item, Index, Value> Index[] getMatches(Iterator<Item> haystackIterator, Value[] needles, Function<Item, Value> valueMapper, BiFunction<Item, Long, Index> indexMapper, IntFunction<Index[]> arrayFactory) {
        boolean caseSensitive = SkriptConfig.caseSensitive.value();
        Object[] results = new List[needles.length];
        long index = 1L;
        boolean shouldBreak = false;
        while (haystackIterator.hasNext()) {
            Item item = haystackIterator.next();
            block5: for (int i = 0; i < needles.length; ++i) {
                Value needle = needles[i];
                if (!this.equals(valueMapper.apply(item), needle, caseSensitive)) continue;
                Index mappedIndex = indexMapper.apply(item, index);
                switch (this.indexType.ordinal()) {
                    case 0: 
                    case 1: {
                        results[i] = Collections.singletonList(mappedIndex);
                        continue block5;
                    }
                    case 2: {
                        if (results[i] == null) {
                            results[i] = new ArrayList();
                        }
                        results[i].add(mappedIndex);
                    }
                }
            }
            if (this.indexType == IndexType.FIRST && !ArrayUtils.contains((Object[])results, null)) break;
            ++index;
        }
        return Arrays.stream(results).filter(Objects::nonNull).flatMap(Collection::stream).toArray(arrayFactory);
    }

    private Long[] getListPositions(Expression<?> haystack, Object[] needles, Event event) {
        Iterator haystackIterator = haystack.iterator(event);
        if (haystackIterator == null) {
            return new Long[0];
        }
        return this.getMatches(haystackIterator, needles, item -> item, (item, index) -> index, Long[]::new);
    }

    private String[] getIndices(KeyProviderExpression<?> haystack, Object[] needles, Event event) {
        Iterator<KeyedValue<?>> haystackIterator = haystack.keyedIterator(event);
        if (haystackIterator == null) {
            return new String[0];
        }
        return this.getMatches(haystackIterator, needles, KeyedValue::value, (item, index) -> item.key(), String[]::new);
    }

    private boolean equals(Object key, Object value, boolean caseSensitive) {
        if (key instanceof String) {
            String keyString = (String)key;
            if (value instanceof String) {
                String valueString = (String)value;
                return StringUtils.equals(keyString, valueString, caseSensitive);
            }
        }
        return key.equals(value);
    }

    @Override
    public boolean isSingle() {
        return (this.indexType == IndexType.FIRST || this.indexType == IndexType.LAST) && this.needle.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        if (this.position) {
            return Long.class;
        }
        return String.class;
    }

    @Override
    public Expression<?> simplify() {
        if (this.position && this.string && this.needle instanceof Literal && this.haystack instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append((Object)this.indexType.name().toLowerCase(Locale.ENGLISH));
        if (this.position) {
            builder.append((Object)"positions");
        } else {
            builder.append((Object)"indices");
        }
        builder.append("of value", this.needle, "in", this.haystack);
        return builder.toString();
    }

    static {
        Skript.registerExpression(ExprIndicesOfValue.class, Object.class, ExpressionType.COMBINED, "[the] [1:first|2:last|3:all] (position[mult:s]|mult:indices|index[mult:es]) of [[the] value] %strings% in %string%", "[the] [1:first|2:last|3:all] position[mult:s] of [[the] value] %objects% in %~objects%", "[the] [1:first|2:last|3:all] (mult:indices|index[mult:es]) of [[the] value] %objects% in %~objects%");
    }

    private static enum IndexType {
        FIRST,
        LAST,
        ALL;

    }
}

