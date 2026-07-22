/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.config.validate;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.validate.EntryValidator;
import ch.njol.skript.config.validate.NodeValidator;
import ch.njol.skript.config.validate.ParsedEntryValidator;
import ch.njol.skript.log.SkriptLogger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class SectionValidator
implements NodeValidator {
    private final HashMap<String, NodeInfo> nodes = new HashMap();
    private boolean allowUndefinedSections = false;
    private boolean allowUndefinedEntries = false;

    public SectionValidator addNode(String name, NodeValidator v, boolean optional) {
        assert (name != null);
        assert (v != null);
        this.nodes.put(name.toLowerCase(Locale.ENGLISH), new NodeInfo(v, optional));
        return this;
    }

    public SectionValidator addEntry(String name, boolean optional) {
        this.addNode(name, new EntryValidator(), optional);
        return this;
    }

    public SectionValidator addEntry(String name, Consumer<String> setter, boolean optional) {
        this.addNode(name, new EntryValidator(setter), optional);
        return this;
    }

    public <T> SectionValidator addEntry(String name, Parser<? extends T> parser, Consumer<T> setter, boolean optional) {
        this.addNode(name, new ParsedEntryValidator<T>(parser, setter), optional);
        return this;
    }

    public SectionValidator addSection(String name, boolean optional) {
        this.addNode(name, new SectionValidator().setAllowUndefinedEntries(true).setAllowUndefinedSections(true), optional);
        return this;
    }

    @Override
    public boolean validate(Node node) {
        if (!(node instanceof SectionNode)) {
            SectionValidator.notASectionError(node);
            return false;
        }
        boolean ok = true;
        for (Map.Entry<String, NodeInfo> e : this.nodes.entrySet()) {
            Node n = ((SectionNode)node).get(e.getKey());
            if (n == null && !e.getValue().optional) {
                Skript.error("Required entry '" + e.getKey() + "' is missing in " + (String)(node.getParent() == null ? node.getConfig().getFileName() : "'" + node.getKey() + "' (" + node.getConfig().getFileName() + ", starting at line " + node.getLine() + ")"));
                ok = false;
                continue;
            }
            if (n == null) continue;
            ok &= e.getValue().v.validate(n);
        }
        SkriptLogger.setNode(null);
        if (this.allowUndefinedSections && this.allowUndefinedEntries) {
            return ok;
        }
        for (Node n : (SectionNode)node) {
            String key = n.getKey();
            assert (key != null);
            if (this.nodes.containsKey(key.toLowerCase(Locale.ENGLISH)) || n instanceof SectionNode && this.allowUndefinedSections || n instanceof EntryNode && this.allowUndefinedEntries) continue;
            SkriptLogger.setNode(n);
            Skript.error("Unexpected entry '" + n.getKey() + "'. Check whether it's spelled correctly or remove it.");
            ok = false;
        }
        SkriptLogger.setNode(null);
        return ok;
    }

    public static void notASectionError(Node node) {
        SkriptLogger.setNode(node);
        Skript.error("'" + node.getKey() + "' is not a section (like 'name:', followed by one or more indented lines)");
    }

    public SectionValidator setAllowUndefinedSections(boolean b) {
        this.allowUndefinedSections = b;
        return this;
    }

    public SectionValidator setAllowUndefinedEntries(boolean b) {
        this.allowUndefinedEntries = b;
        return this;
    }

    private static final class NodeInfo {
        public NodeValidator v;
        public boolean optional;

        public NodeInfo(NodeValidator v, boolean optional) {
            this.v = v;
            this.optional = optional;
        }
    }
}

