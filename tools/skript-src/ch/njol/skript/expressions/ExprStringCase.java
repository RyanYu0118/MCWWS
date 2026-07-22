/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.WordUtils
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import org.apache.commons.lang.WordUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Case Text")
@Description(value={"Copy of given text in Lowercase, Uppercase, Proper Case, camelCase, PascalCase, Snake_Case, and Kebab-Case"})
@Example.Examples(value={@Example(value="\"Oops!\" in lowercase # oops!"), @Example(value="\"oops!\" in uppercase # OOPS!"), @Example(value="\"hellO i'm steve!\" in proper case # HellO I'm Steve!"), @Example(value="\"hellO i'm steve!\" in strict proper case # Hello I'm Steve!"), @Example(value="\"spAwn neW boSs ()\" in camel case # spAwnNeWBoSs()"), @Example(value="\"spAwn neW boSs ()\" in strict camel case # spawnNewBoss()"), @Example(value="\"geneRate ranDom numBer ()\" in pascal case # GeneRateRanDomNumBer()"), @Example(value="\"geneRate ranDom numBer ()\" in strict pascal case # GenerateRandomNumber()"), @Example(value="\"Hello Player!\" in snake case # Hello_Player!"), @Example(value="\"Hello Player!\" in lower snake case # hello_player!"), @Example(value="\"Hello Player!\" in upper snake case # HELLO_PLAYER!"), @Example(value="\"What is your name?\" in kebab case # What-is-your-name?"), @Example(value="\"What is your name?\" in lower kebab case # what-is-your-name?"), @Example(value="\"What is your name?\" in upper kebab case # WHAT-IS-YOUR-NAME?")})
@Since(value={"2.2-dev16 (lowercase and uppercase), 2.5 (advanced cases)"})
public class ExprStringCase
extends SimpleExpression<String> {
    private Expression<String> expr;
    private int casemode = 0;
    private int type = 0;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.expr = exprs[0];
        if (matchedPattern <= 1) {
            this.casemode = parseResult.mark == 0 ? 1 : 2;
        } else if (matchedPattern == 2) {
            this.casemode = 1;
        } else if (matchedPattern <= 4) {
            this.type = 1;
            if (parseResult.mark != 0) {
                this.casemode = 3;
            }
        } else if (matchedPattern <= 6) {
            this.type = 2;
            if (parseResult.mark != 0) {
                this.casemode = 3;
            }
        } else if (matchedPattern <= 8) {
            this.type = 3;
            if (parseResult.mark != 0) {
                this.casemode = 3;
            }
        } else if (matchedPattern <= 10) {
            this.type = 4;
            if (parseResult.mark != 0) {
                this.casemode = parseResult.mark == 1 ? 2 : 1;
            }
        } else if (matchedPattern <= 12) {
            this.type = 5;
            if (parseResult.mark != 0) {
                this.casemode = parseResult.mark == 1 ? 2 : 1;
            }
        }
        return true;
    }

    @Nullable
    protected String[] get(Event e) {
        String[] strs = this.expr.getArray(e);
        block8: for (int i = 0; i < strs.length; ++i) {
            if (strs[i] == null) continue;
            switch (this.type) {
                case 0: {
                    strs[i] = this.casemode == 1 ? strs[i].toUpperCase() : strs[i].toLowerCase();
                    continue block8;
                }
                case 1: {
                    strs[i] = this.casemode == 3 ? WordUtils.capitalizeFully((String)strs[i]) : WordUtils.capitalize((String)strs[i]);
                    continue block8;
                }
                case 2: {
                    strs[i] = ExprStringCase.toCamelCase(strs[i], this.casemode == 3);
                    continue block8;
                }
                case 3: {
                    strs[i] = ExprStringCase.toPascalCase(strs[i], this.casemode == 3);
                    continue block8;
                }
                case 4: {
                    strs[i] = ExprStringCase.toSnakeCase(strs[i], this.casemode);
                    continue block8;
                }
                case 5: {
                    strs[i] = ExprStringCase.toKebabCase(strs[i], this.casemode);
                }
            }
        }
        return strs;
    }

    @Override
    public boolean isSingle() {
        return this.expr.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Expression<? extends String> simplify() {
        if (this.expr instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object mode = "";
        switch (this.type) {
            case 0: {
                mode = this.casemode == 1 ? "uppercase" : "lowercase";
                break;
            }
            case 1: {
                mode = (this.casemode == 3 ? "strict" : "lenient") + " proper case";
                break;
            }
            case 2: {
                mode = (this.casemode == 3 ? "strict" : "lenient") + " camel case";
                break;
            }
            case 3: {
                mode = (this.casemode == 3 ? "strict" : "lenient") + " pascal case";
                break;
            }
            case 4: {
                mode = (this.casemode == 0 ? "" : (this.casemode == 1 ? "upper " : "lower ")) + "snake case";
                break;
            }
            case 5: {
                mode = (this.casemode == 0 ? "" : (this.casemode == 1 ? "upper " : "lower ")) + "kebab case";
            }
        }
        return (String)mode + " " + this.expr.toString(event, debug);
    }

    private static String toCamelCase(String str, boolean strict) {
        String[] words = str.split(" ");
        Object buf = words.length > 0 ? (strict ? words[0].toLowerCase() : WordUtils.uncapitalize((String)words[0])) : "";
        for (int i = 1; i < words.length; ++i) {
            buf = (String)buf + (strict ? WordUtils.capitalizeFully((String)words[i]) : WordUtils.capitalize((String)words[i]));
        }
        return buf;
    }

    private static String toPascalCase(String str, boolean strict) {
        String[] words = str.split(" ");
        Object buf = "";
        for (int i = 0; i < words.length; ++i) {
            buf = (String)buf + (strict ? WordUtils.capitalizeFully((String)words[i]) : WordUtils.capitalize((String)words[i]));
        }
        return buf;
    }

    private static String toSnakeCase(String str, int mode) {
        if (mode == 0) {
            return str.replace(' ', '_');
        }
        StringBuilder sb = new StringBuilder();
        Iterator iterator = ((Iterable)str.codePoints()::iterator).iterator();
        while (iterator.hasNext()) {
            int c = (Integer)iterator.next();
            sb.appendCodePoint(c == 32 ? 95 : (mode == 1 ? Character.toUpperCase(c) : Character.toLowerCase(c)));
        }
        return sb.toString();
    }

    private static String toKebabCase(String str, int mode) {
        if (mode == 0) {
            return str.replace(' ', '-');
        }
        StringBuilder sb = new StringBuilder();
        Iterator iterator = ((Iterable)str.codePoints()::iterator).iterator();
        while (iterator.hasNext()) {
            int c = (Integer)iterator.next();
            sb.appendCodePoint(c == 32 ? 45 : (mode == 1 ? Character.toUpperCase(c) : Character.toLowerCase(c)));
        }
        return sb.toString();
    }

    static {
        Skript.registerExpression(ExprStringCase.class, String.class, ExpressionType.SIMPLE, "%strings% in (0\u00a6upper|1\u00a6lower)[ ]case", "(0\u00a6upper|1\u00a6lower)[ ]case %strings%", "capitali(s|z)ed %strings%", "%strings% in [(0\u00a6lenient|1\u00a6strict) ](proper|title)[ ]case", "[(0\u00a6lenient|1\u00a6strict) ](proper|title)[ ]case %strings%", "%strings% in [(0\u00a6lenient|1\u00a6strict) ]camel[ ]case", "[(0\u00a6lenient|1\u00a6strict) ]camel[ ]case %strings%", "%strings% in [(0\u00a6lenient|1\u00a6strict) ]pascal[ ]case", "[(0\u00a6lenient|1\u00a6strict) ]pascal[ ]case %strings%", "%strings% in [(1\u00a6lower|2\u00a6upper|3\u00a6capital|4\u00a6screaming)[ ]]snake[ ]case", "[(1\u00a6lower|2\u00a6upper|3\u00a6capital|4\u00a6screaming)[ ]]snake[ ]case %strings%", "%strings% in [(1\u00a6lower|2\u00a6upper|3\u00a6capital)[ ]]kebab[ ]case", "[(1\u00a6lower|2\u00a6upper|3\u00a6capital)[ ]]kebab[ ]case %strings%");
    }
}

