/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang.parser;

import ch.njol.skript.lang.parser.ParsingStack;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ParseStackOverflowException
extends RuntimeException {
    protected final ParsingStack parsingStack;

    public ParseStackOverflowException(StackOverflowError cause, ParsingStack parsingStack) {
        super(ParseStackOverflowException.createMessage(parsingStack), cause);
        this.parsingStack = parsingStack;
    }

    private static String createMessage(ParsingStack stack) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(stream);
        stack.print(printStream);
        return stream.toString();
    }
}

