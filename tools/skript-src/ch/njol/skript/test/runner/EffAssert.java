/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.VerboseAssert;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.test.runner.SkriptJUnitTest;
import ch.njol.skript.test.runner.TestMode;
import ch.njol.skript.test.runner.TestTracker;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Assert")
@Description(value={"Assert that condition is true. Test fails when it is not."})
@NoDoc
public class EffAssert
extends Effect {
    private static final String DEFAULT_ERROR = "Assertion failed.";
    @Nullable
    private Condition condition;
    private Script script;
    private int line;
    @Nullable
    private Expression<String> errorMsg;
    @Nullable
    private Expression<?> expected;
    @Nullable
    private Expression<?> got;
    private boolean shouldFail;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!(isDelayed != Kleenean.TRUE || parseResult.hasTag("unsafely") || TestMode.JUNIT || TestMode.DEV_MODE)) {
            Skript.error("Assertions cannot be delayed");
            return false;
        }
        String conditionString = parseResult.regexes.get(0).group();
        if (matchedPattern < 2) {
            this.errorMsg = exprs[0];
        }
        boolean canInit = true;
        if (exprs.length > 1) {
            this.expected = LiteralUtils.defendExpression(exprs[1]);
            this.got = LiteralUtils.defendExpression(exprs[2]);
            canInit = LiteralUtils.canInitSafely(this.expected, this.got);
        }
        this.shouldFail = parseResult.mark != 0;
        this.script = this.getParser().getCurrentScript();
        Node node = this.getParser().getNode();
        this.line = node != null ? node.getLine() : -1;
        try (ParseLogHandler logHandler = SkriptLogger.startParseLogHandler();){
            this.condition = Condition.parse(conditionString, "Can't understand this condition: " + conditionString);
            if (this.shouldFail) {
                boolean bl = true;
                return bl;
            }
            if (this.condition == null) {
                logHandler.printError();
            } else {
                logHandler.printLog();
            }
        }
        return this.condition != null && canInit;
    }

    @Override
    protected void execute(Event event) {
    }

    @Override
    @Nullable
    public TriggerItem walk(Event event) {
        if (this.shouldFail && this.condition == null) {
            return this.getNext();
        }
        if (this.condition.check(event) == this.shouldFail) {
            Object message = this.errorMsg != null ? this.errorMsg.getOptionalSingle(event).orElse(DEFAULT_ERROR) : DEFAULT_ERROR;
            String expectedMessage = "";
            String gotMessage = "";
            if (this.expected != null) {
                expectedMessage = VerboseAssert.getExpressionValue(this.expected, event);
            }
            if (this.got != null) {
                gotMessage = VerboseAssert.getExpressionValue(this.got, event);
            }
            if (this.condition instanceof VerboseAssert) {
                if (expectedMessage.isEmpty()) {
                    expectedMessage = ((VerboseAssert)((Object)this.condition)).getExpectedMessage(event);
                }
                if (gotMessage.isEmpty()) {
                    gotMessage = ((VerboseAssert)((Object)this.condition)).getReceivedMessage(event);
                }
            }
            if (!expectedMessage.isEmpty() && !gotMessage.isEmpty()) {
                message = (String)message + " (Expected " + expectedMessage + ", but got " + gotMessage + ")";
            }
            if (SkriptJUnitTest.getCurrentJUnitTest() != null) {
                TestTracker.junitTestFailed(SkriptJUnitTest.getCurrentJUnitTest(), (String)message);
            } else if (this.line >= 0) {
                TestTracker.testFailed((String)message, this.script, this.line);
            } else {
                TestTracker.testFailed((String)message, this.script);
            }
            return null;
        }
        return this.getNext();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.condition == null) {
            return "assertion";
        }
        return "assert " + this.condition.toString(event, debug);
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerEffect(EffAssert.class, "assert [:unsafely] <.+> [(1:to fail)] with [error] %string%", "assert [:unsafely] <.+> [(1:to fail)] with [error] %string%, expected [value] %object%, [and] (received|got) [value] %object%", "assert [:unsafely] <.+> [(1:to fail)]");
        }
    }
}

