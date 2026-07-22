/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Matches")
@Description(value={"Checks whether the defined strings match the input regexes (Regular expressions)."})
@Example(value="on chat:\n\tif message partially matches \"\\d\":\n\t\tsend \"Message contains a digit!\"\n\tif message doesn't match \"[A-Za-z]+\":\n\t\tsend \"Message doesn't only contain letters!\"\n")
@Since(value={"2.5.2"})
public class CondMatches
extends Condition {
    Expression<String> strings;
    Expression<String> regex;
    boolean partial;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.strings = exprs[0];
        this.regex = exprs[1];
        this.partial = matchedPattern == 1;
        this.setNegated(parseResult.mark == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        String[] txt = this.strings.getAll(e);
        String[] regexes = this.regex.getAll(e);
        if (txt.length < 1 || regexes.length < 1) {
            return false;
        }
        boolean stringAnd = this.strings.getAnd();
        boolean regexAnd = this.regex.getAnd();
        boolean result = stringAnd ? (regexAnd ? Arrays.stream(txt).allMatch(str -> ((Stream)Arrays.stream(regexes).parallel()).map(Pattern::compile).allMatch(pattern -> this.matches((String)str, (Pattern)pattern))) : Arrays.stream(txt).allMatch(str -> ((Stream)Arrays.stream(regexes).parallel()).map(Pattern::compile).anyMatch(pattern -> this.matches((String)str, (Pattern)pattern)))) : (regexAnd ? Arrays.stream(txt).anyMatch(str -> ((Stream)Arrays.stream(regexes).parallel()).map(Pattern::compile).allMatch(pattern -> this.matches((String)str, (Pattern)pattern))) : Arrays.stream(txt).anyMatch(str -> ((Stream)Arrays.stream(regexes).parallel()).map(Pattern::compile).anyMatch(pattern -> this.matches((String)str, (Pattern)pattern))));
        return result == this.isNegated();
    }

    public boolean matches(String str, Pattern pattern) {
        return this.partial ? pattern.matcher(str).find() : str.matches(pattern.pattern());
    }

    @Override
    public Condition simplify() {
        if (this.strings instanceof Literal && this.regex instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return this.strings.toString(e, debug) + " " + (this.isNegated() ? "doesn't match" : "matches") + " " + this.regex.toString(e, debug);
    }

    static {
        Skript.registerCondition(CondMatches.class, "%strings% (1\u00a6match[es]|2\u00a6do[es](n't| not) match) %strings%", "%strings% (1\u00a6partially match[es]|2\u00a6do[es](n't| not) partially match) %strings%");
    }
}

