/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 */
package ch.njol.skript.patterns;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.patterns.SkriptPattern;
import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatchResult {
    SkriptPattern source;
    int exprOffset;
    Expression<?>[] expressions = new Expression[0];
    String expr;
    int mark;
    List<String> tags = new ArrayList<String>();
    List<java.util.regex.MatchResult> regexResults = new ArrayList<java.util.regex.MatchResult>();
    ParseContext parseContext = ParseContext.DEFAULT;
    int flags;

    public MatchResult copy() {
        MatchResult matchResult = new MatchResult();
        matchResult.source = this.source;
        matchResult.exprOffset = this.exprOffset;
        matchResult.expressions = (Expression[])this.expressions.clone();
        matchResult.expr = this.expr;
        matchResult.mark = this.mark;
        matchResult.tags = new ArrayList<String>(this.tags);
        matchResult.regexResults = new ArrayList<java.util.regex.MatchResult>(this.regexResults);
        matchResult.parseContext = this.parseContext;
        matchResult.flags = this.flags;
        return matchResult;
    }

    public SkriptParser.ParseResult toParseResult() {
        SkriptParser.ParseResult parseResult = new SkriptParser.ParseResult(this.expr, this.expressions);
        parseResult.source = this.source;
        parseResult.regexes.addAll(this.regexResults);
        parseResult.mark = this.mark;
        parseResult.tags.addAll(this.tags);
        return parseResult;
    }

    public Expression<?>[] getExpressions() {
        return this.expressions;
    }

    public String getExpr() {
        return this.expr;
    }

    public int getMark() {
        return this.mark;
    }

    public List<String> getTags() {
        return this.tags;
    }

    public List<java.util.regex.MatchResult> getRegexResults() {
        return this.regexResults;
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("source", (Object)this.source).add("exprOffset", this.exprOffset).add("expressions", (Object)Arrays.toString(this.expressions)).add("expr", (Object)this.expr).add("mark", this.mark).add("tags", this.tags).add("regexResults", this.regexResults).add("parseContext", (Object)this.parseContext).add("flags", this.flags).toString();
    }
}

