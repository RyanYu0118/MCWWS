/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang.parser;

import ch.njol.skript.lang.SyntaxElement;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import org.skriptlang.skript.registration.SyntaxInfo;

public class ParsingStack
implements Iterable<Element> {
    private final LinkedList<Element> stack;

    public ParsingStack() {
        this.stack = new LinkedList();
    }

    public ParsingStack(ParsingStack parsingStack) {
        this.stack = new LinkedList<Element>(parsingStack.stack);
    }

    public Element pop() throws IllegalStateException {
        if (this.stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return this.stack.pop();
    }

    public Element peek(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= this.size()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        return this.stack.get(index);
    }

    public Element peek() throws IllegalStateException {
        if (this.stack.isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return this.stack.peek();
    }

    public void push(Element element) {
        this.stack.push(element);
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public int size() {
        return this.stack.size();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void print(PrintStream printStream) {
        PrintStream printStream2 = printStream;
        synchronized (printStream2) {
            printStream.println("Stack:");
            if (this.stack.isEmpty()) {
                printStream.println("<empty>");
            } else {
                for (Element element : this.stack) {
                    printStream.println("\t" + element.getSyntaxElementClass().getName() + " @ " + element.patternIndex());
                }
            }
        }
    }

    @Override
    public Iterator<Element> iterator() {
        return Collections.unmodifiableList(this.stack).iterator();
    }

    public record Element(SyntaxInfo<?> syntaxElementInfo, int patternIndex) {
        private final SyntaxInfo<?> syntaxElementInfo;

        public Element {
            assert (patternIndex >= 0 && patternIndex < syntaxElementInfo.patterns().size());
        }

        public SyntaxInfo<?> syntaxElementInfo() {
            return this.syntaxInfo();
        }

        public SyntaxInfo<?> syntaxInfo() {
            return this.syntaxElementInfo;
        }

        public Class<? extends SyntaxElement> getSyntaxElementClass() {
            return this.syntaxElementInfo.type();
        }

        public String getPattern() {
            return this.getPatterns()[this.patternIndex];
        }

        public String[] getPatterns() {
            return this.syntaxElementInfo.patterns().toArray(new String[0]);
        }
    }
}

