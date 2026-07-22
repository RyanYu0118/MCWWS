/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import java.util.logging.Logger;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Debug")
@Description(value={"Debug an expression by printing data of it to console.", "Can also debug what class an input is parsed as for Conditions and Effects.", "Useful for dealing with conflict debugging."})
@Since(value={"2.7"})
@NoDoc
public class EffDebug
extends Effect {
    private Expression<?> expressions;
    private boolean debug = Skript.debug();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 1) {
            String string = parseResult.regexes.get(0).group();
            ParseLogHandler logHandler = SkriptLogger.startParseLogHandler();
            try {
                Effect effect = Effect.parse(string, "Can't understand this effect: " + string);
                if (effect == null) {
                    logHandler.printError();
                } else {
                    Logger logger = Skript.getInstance().getLogger();
                    logger.info("--------------------");
                    logger.info("Parsed '" + string + "' to be Effect " + effect.getClass().getName());
                    logger.info("--------------------");
                    logHandler.printLog();
                }
            }
            finally {
                logHandler.stop();
            }
            return true;
        }
        if (matchedPattern == 2) {
            String string = parseResult.regexes.get(0).group();
            ParseLogHandler logHandler = SkriptLogger.startParseLogHandler();
            try {
                Condition conditon = Condition.parse(string, "Can't understand this conditon: " + string);
                if (conditon == null) {
                    logHandler.printError();
                } else {
                    Logger logger = Skript.getInstance().getLogger();
                    logger.info("--------------------");
                    logger.info("Parsed '" + string + "' to be Condition " + conditon.getClass().getName());
                    logger.info("--------------------");
                    logHandler.printLog();
                }
            }
            finally {
                logHandler.stop();
            }
            return true;
        }
        this.expressions = exprs[0];
        if (LiteralUtils.canInitSafely(this.expressions)) {
            this.expressions = LiteralUtils.defendExpression(this.expressions);
        }
        if (parseResult.hasTag("verbose")) {
            this.debug = true;
        }
        this.print();
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (this.expressions == null) {
            return;
        }
        this.print(event, this.debug);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "debug " + this.expressions.toString(event, debug);
    }

    private void print() {
        this.print(null, this.debug);
    }

    private void print(@Nullable Event event, boolean debug) {
        Skript.info("--------------------");
        Skript.info(event == null ? "PARSE TIME" : "RUNTIME");
        Skript.info("\tExpression " + this.expressions.getClass().getName());
        Skript.info("\ttoString: " + this.expressions.toString(event, debug));
        if (LiteralUtils.hasUnparsedLiteral(this.expressions)) {
            Skript.info("EXPRESSION WAS UNPARSED LITERAL");
            Skript.info("--------------------");
            return;
        }
        Skript.info("\tChangers: " + Arrays.toString(this.expressions.getAcceptedChangeModes().entrySet().stream().map(entry -> ((Class[])entry.getValue()).getClass().getSimpleName() + ":" + ((Changer.ChangeMode)((Object)((Object)entry.getKey()))).name()).toArray(String[]::new)));
        Skript.info("\tAnd: " + this.expressions.getAnd());
        Skript.info("\tReturn type: " + String.valueOf(this.expressions.getReturnType()));
        Skript.info("\tisDefault: " + this.expressions.isDefault());
        Skript.info("\tisSingle: " + this.expressions.isSingle());
        Skript.info("--------------------");
        if (this.expressions instanceof Literal) {
            Skript.info("Literal Values: " + Arrays.toString(((Literal)this.expressions).getArray()));
            Skript.info("--------------------");
        } else if (event != null) {
            Skript.info("Values: " + Arrays.toString(this.expressions.getArray(event)));
            Skript.info("--------------------");
        }
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerEffect(EffDebug.class, "debug [:verbose] %objects%", "debug-effect <.+>", "debug-condition <.+>");
        }
    }
}

