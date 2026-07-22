/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.log;

import ch.njol.skript.config.Node;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.test.runner.EvtTestCase;
import ch.njol.skript.test.runner.TestTracker;
import java.util.logging.Level;
import org.skriptlang.skript.lang.structure.Structure;

public class TestingLogHandler
extends LogHandler {
    private final int minimum;
    private int count;
    private final ParserInstance parser;

    public TestingLogHandler(Level minimum) {
        this.minimum = minimum.intValue();
        this.parser = ParserInstance.get();
    }

    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        if (entry.level.intValue() >= this.minimum) {
            String string;
            ++this.count;
            Structure struct = this.parser.getCurrentStructure();
            Node node = this.parser.getNode();
            if (struct instanceof EvtTestCase) {
                EvtTestCase test = (EvtTestCase)struct;
                string = test.getTestName();
            } else {
                string = struct != null ? struct.getSyntaxTypeName() : null;
            }
            String name = string;
            TestTracker.parsingStarted(name);
            if (node != null) {
                TestTracker.testFailed(entry.getMessage(), this.parser.getCurrentScript(), node.getLine());
            } else {
                TestTracker.testFailed(entry.getMessage());
            }
        }
        return LogHandler.LogResult.LOG;
    }

    @Override
    public TestingLogHandler start() {
        SkriptLogger.startLogHandler(this);
        return this;
    }

    public int getCount() {
        return this.count;
    }
}

