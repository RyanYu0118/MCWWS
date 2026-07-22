/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.config.validate;

import ch.njol.skript.classes.Parser;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.validate.EntryValidator;
import ch.njol.skript.lang.ParseContext;
import java.util.function.Consumer;

public class ParsedEntryValidator<T>
extends EntryValidator {
    private final Parser<? extends T> parser;
    private final Consumer<T> setter;

    public ParsedEntryValidator(Parser<? extends T> parser, Consumer<T> setter) {
        assert (parser != null);
        assert (setter != null);
        this.parser = parser;
        this.setter = setter;
    }

    @Override
    public boolean validate(Node node) {
        if (!super.validate(node)) {
            return false;
        }
        T t = this.parser.parse(((EntryNode)node).getValue(), ParseContext.CONFIG);
        if (t == null) {
            return false;
        }
        this.setter.accept(t);
        return true;
    }
}

