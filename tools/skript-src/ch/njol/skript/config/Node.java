/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.NodeNavigator;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.VoidNode;
import ch.njol.skript.lang.util.common.AnyNamed;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.util.Validated;

public abstract class Node
implements AnyNamed,
Validated,
NodeNavigator {
    @Nullable
    protected String key;
    protected String comment = "";
    protected final int lineNum;
    private final boolean debug;
    @Nullable
    protected SectionNode parent;
    protected Config config;

    protected Node(Config c) {
        this.key = null;
        this.debug = false;
        this.lineNum = -1;
        this.config = c;
        SkriptLogger.setNode(this);
    }

    protected Node(String key, SectionNode parent) {
        this.key = key;
        this.debug = false;
        this.lineNum = -1;
        this.parent = parent;
        this.config = parent.getConfig();
        SkriptLogger.setNode(this);
    }

    protected Node(String key, String comment, SectionNode parent, int lineNum) {
        this.key = key;
        this.comment = comment;
        this.debug = comment.equals("#DEBUG#");
        this.lineNum = lineNum;
        this.parent = parent;
        this.config = parent.getConfig();
        SkriptLogger.setNode(this);
    }

    @Nullable
    public String getKey() {
        return this.key;
    }

    public final Config getConfig() {
        return this.config;
    }

    public void rename(String newname) {
        if (this.key == null) {
            throw new IllegalStateException("can't rename an anonymous node");
        }
        String oldKey = this.key;
        this.key = newname;
        if (this.parent != null) {
            this.parent.renamed(this, oldKey);
        }
    }

    public void move(SectionNode newParent) {
        SectionNode p = this.parent;
        if (p == null) {
            throw new IllegalStateException("can't move the main node");
        }
        p.remove(this);
        newParent.add(this);
    }

    public static NonNullPair<String, String> splitLine(String line) {
        return Node.splitLine(line, new AtomicBoolean(false));
    }

    public static NonNullPair<String, String> splitLine(String line, AtomicBoolean inBlockComment) {
        String trimmed = line.trim();
        if (trimmed.equals("###")) {
            inBlockComment.set(!inBlockComment.get());
            return new NonNullPair<String, String>("", line);
        }
        if (trimmed.startsWith("#")) {
            return new NonNullPair<String, String>("", line.substring(line.indexOf(35)));
        }
        if (inBlockComment.get()) {
            return new NonNullPair<String, String>("", line);
        }
        int length = line.length();
        StringBuilder finalLine = new StringBuilder(line);
        int numRemoved = 0;
        SplitLineState state = SplitLineState.CODE;
        SplitLineState previousState = SplitLineState.CODE;
        for (int i = 0; i < length; ++i) {
            char c = line.charAt(i);
            if (c != '%' && c != '\"' && c != '#') continue;
            if ((c != '#' || state != SplitLineState.STRING) && i + 1 < length && line.charAt(i + 1) == c) {
                if (c == '#') {
                    finalLine.deleteCharAt(i - numRemoved);
                    ++numRemoved;
                }
                ++i;
                continue;
            }
            SplitLineState tmp = state;
            if ((state = SplitLineState.update(c, state, previousState)) == SplitLineState.HALT) {
                return new NonNullPair<String, String>(finalLine.substring(0, i - numRemoved), line.substring(i));
            }
            if (c != '%' || state != SplitLineState.CODE) continue;
            previousState = tmp;
        }
        return new NonNullPair<String, String>(finalLine.toString(), "");
    }

    static void handleNodeStackOverflow(StackOverflowError e, String line) {
        Node n = SkriptLogger.getNode();
        SkriptLogger.setNode(null);
        Skript.error("There was a StackOverFlowError occurred when loading a node. This maybe from your scripts, aliases or Skript configuration.");
        Skript.error("Please make your script lines shorter! Do NOT report this to SkriptLang unless it occurs with a short script line or built-in aliases!");
        Skript.error("");
        Skript.error("Updating your Java and/or using respective 64-bit versions for your operating system may also help and is always a good practice.");
        Skript.error("If it is still not fixed, try moderately increasing the thread stack size (-Xss flag) in your startup script.");
        Skript.error("");
        Skript.error("Using a different Java Virtual Machine (JVM) like OpenJ9 or GraalVM may also help; though be aware that not all plugins may support them.");
        Skript.error("");
        Skript.error("Line that caused the issue:");
        Skript.error(line);
        if (Skript.testing()) {
            Skript.exception((Throwable)e, new String[0]);
        }
        SkriptLogger.setNode(n);
    }

    @Nullable
    protected String getComment() {
        return this.comment;
    }

    int getLevel() {
        int l = 0;
        Node n = this;
        while ((n = n.parent) != null) {
            ++l;
        }
        return Math.max(0, l - 1);
    }

    protected String getIndentation() {
        return StringUtils.multiply(this.config.getIndentation(), this.getLevel());
    }

    abstract String save_i();

    public final String save() {
        return this.getIndentation() + Node.escapeUnquotedHashtags(this.save_i()) + this.comment;
    }

    public void save(PrintWriter w) {
        w.println(this.save());
    }

    private static String escapeUnquotedHashtags(String input) {
        int length = input.length();
        StringBuilder output = new StringBuilder(input);
        int numAdded = 0;
        SplitLineState state = SplitLineState.CODE;
        SplitLineState previousState = SplitLineState.CODE;
        for (int i = 0; i < length; ++i) {
            char c = input.charAt(i);
            if (c != '%' && c != '\"' && c != '#') continue;
            if (c == '#' && state != SplitLineState.STRING) {
                output.insert(i + numAdded, "#");
                ++numAdded;
                continue;
            }
            if (i + 1 < length && input.charAt(i + 1) == c) {
                ++i;
                continue;
            }
            SplitLineState tmp = state;
            state = SplitLineState.update(c, state, previousState);
            previousState = tmp;
        }
        return output.toString();
    }

    @Nullable
    public SectionNode getParent() {
        return this.parent;
    }

    public void remove() {
        SectionNode p = this.parent;
        if (p == null) {
            return;
        }
        p.remove(this);
    }

    public int getLine() {
        return this.lineNum;
    }

    public boolean isVoid() {
        return this instanceof VoidNode;
    }

    public String toString() {
        if (this.parent == null) {
            return this.config.getFileName();
        }
        return this.save_i() + (String)(this.comment.isEmpty() ? "" : " " + this.comment) + " (" + this.config.getFileName() + ", " + (String)(this.lineNum == -1 ? "unknown line" : "line " + this.lineNum) + ")";
    }

    public boolean debug() {
        return this.debug;
    }

    @Override
    public void invalidate() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean valid() {
        return this.config != null && this.config.valid();
    }

    @Nullable
    public String getPath() {
        if (this.key == null) {
            return null;
        }
        if (this.parent == null) {
            return this.key;
        }
        @Nullable String path = this.parent.getPath();
        if (path == null) {
            return this.key;
        }
        return path + "." + this.key;
    }

    @Override
    @NotNull
    public Node getCurrentNode() {
        return this;
    }

    int getIndex() {
        if (this.parent == null) {
            return -1;
        }
        int index = 0;
        Iterator<Node> iterator = this.parent.fullIterator();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (node == this) {
                return index;
            }
            ++index;
        }
        return -1;
    }

    @NotNull
    public String[] getPathSteps() {
        ArrayList<String> path = new ArrayList<String>();
        for (Node node = this; node != null && node.getKey() != null && !node.getKey().isEmpty(); node = node.getParent()) {
            path.add(0, node.getKey());
        }
        if (path.isEmpty()) {
            return new String[0];
        }
        return path.toArray(new String[0]);
    }

    @Override
    @Nullable
    public String name() {
        return this.getKey();
    }

    public boolean equals(Object object) {
        if (!(object instanceof Node)) {
            return false;
        }
        Node other = (Node)object;
        return Arrays.equals(this.getPathSteps(), other.getPathSteps()) && Objects.equals(this.comment, other.comment);
    }

    public int hashCode() {
        return Objects.hash(Arrays.hashCode(this.getPathSteps()), this.comment);
    }

    private static enum SplitLineState {
        HALT,
        CODE,
        STRING,
        VARIABLE;


        private static SplitLineState update(char c, SplitLineState state, SplitLineState previousState) {
            if (state == HALT) {
                return HALT;
            }
            switch (c) {
                case '%': {
                    if (state == CODE) {
                        return previousState;
                    }
                    return CODE;
                }
                case '\"': {
                    switch (state.ordinal()) {
                        case 1: {
                            return STRING;
                        }
                        case 2: {
                            return CODE;
                        }
                    }
                    return state;
                }
                case '{': {
                    if (state == STRING) {
                        return STRING;
                    }
                    return VARIABLE;
                }
                case '}': {
                    if (state == STRING) {
                        return STRING;
                    }
                    return CODE;
                }
                case '#': {
                    if (state == STRING) {
                        return STRING;
                    }
                    return HALT;
                }
            }
            return state;
        }
    }
}

