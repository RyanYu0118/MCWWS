/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Substring")
@Description(value={"Extracts part of a text. You can either get the first &lt;x&gt; characters, the last &lt;x&gt; characters, the character at index &lt;x&gt;, or the characters between indices &lt;x&gt; and &lt;y&gt;. The indices &lt;x&gt; and &lt;y&gt; should be between 1 and the <a href='#ExprLength'>length</a> of the text (other values will be fit into this range)."})
@Keywords(value={"substring", "subtext"})
@Example.Examples(value={@Example(value="set {_s} to the first 5 characters of the text argument"), @Example(value="message \"%subtext of {_s} from characters 2 to (the length of {_s} - 1)%\" # removes the first and last character from {_s} and sends it to the player or console"), @Example(value="set {_characters::*} to characters at 1, 2 and 7 in player's display name"), @Example(value="send the last character of all players' names")})
@Since(value={"2.1, 2.5.2 (character at, multiple strings support)"})
public class ExprSubstring
extends SimpleExpression<String> {
    private Expression<String> string;
    @Nullable
    private Expression<Number> start;
    @Nullable
    private Expression<Number> end;
    private boolean usedSubstring;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.usedSubstring = matchedPattern == 0;
        this.string = exprs[this.usedSubstring ? 0 : 1];
        Object object = this.usedSubstring ? exprs[1] : (parseResult.mark == 1 ? null : (this.start = exprs[0] == null ? new SimpleLiteral<Integer>(1, false) : exprs[0]));
        Object object2 = this.usedSubstring ? exprs[2] : (parseResult.mark == 2 ? null : (this.end = exprs[0] == null ? new SimpleLiteral<Integer>(1, false) : exprs[0]));
        assert (this.end != null || this.start != null);
        return true;
    }

    @Nullable
    protected String[] get(Event e) {
        ArrayList<String> parts = new ArrayList<String>();
        String[] strings = this.string.getArray(e);
        if (strings == null) {
            return new String[0];
        }
        for (String string : strings) {
            int i2;
            int i1;
            Integer d2;
            if (this.start != null && !this.start.isSingle()) {
                Number[] i = this.start.getArray(e);
                if (i == null) {
                    return new String[0];
                }
                for (Number p : i) {
                    if (p.intValue() > string.length() || p.intValue() < 1) continue;
                    parts.add(string.substring(p.intValue() - 1, p.intValue()));
                }
                continue;
            }
            Integer d1 = this.start != null ? (Number)this.start.getSingle(e) : (Number)1;
            Number number = d2 = this.end != null ? (Number)this.end.getSingle(e) : (Number)string.length();
            if (d1 == null || d2 == null) continue;
            if (this.end == null) {
                d1 = string.length() - d1 + 1;
            }
            if ((i1 = Math.max(d1 - 1, 0)) >= (i2 = Math.min(d2, string.length()))) continue;
            parts.add(string.substring(i1, i2));
        }
        return parts.toArray(new String[parts.size()]);
    }

    @Override
    public boolean isSingle() {
        return this.string.isSingle() && (this.start == null || this.start.isSingle());
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Expression<? extends String> simplify() {
        if (this.string instanceof Literal && (this.start == null || this.start instanceof Literal) && (this.end == null || this.end instanceof Literal)) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        if (this.start == null) {
            assert (this.end != null);
            return "the first " + this.end.toString(e, debug) + " characters of " + this.string.toString(e, debug);
        }
        if (this.end == null) {
            assert (this.start != null);
            return "the last " + this.start.toString(e, debug) + " characters of " + this.string.toString(e, debug);
        }
        if (this.usedSubstring) {
            return "the substring of " + this.string.toString(e, debug) + " from index " + this.start.toString(e, debug) + " to " + this.end.toString(e, debug);
        }
        return "the character at " + (this.start.isSingle() ? "index " : "indexes ") + this.start.toString(e, debug) + " in " + this.string.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprSubstring.class, String.class, ExpressionType.COMBINED, "[the] (part|sub[ ](text|string)) of %strings% (between|from) (ind(ex|ices)|character[s]|) %number% (and|to) (index|character|) %number%", "[the] (1\u00a6first|2\u00a6last) [%-number%] character[s] of %strings%", "[the] %number% (1\u00a6first|2\u00a6last) characters of %strings%", "[the] character[s] at [(index|position|indexes|indices|positions)] %numbers% (in|of) %strings%");
    }
}

