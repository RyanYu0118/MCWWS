/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Message;
import java.util.IllegalFormatException;

public final class FormattedMessage
extends Message {
    private final Object[] args;

    public FormattedMessage(String key, Object ... args) {
        super(key);
        assert (args.length > 0);
        this.args = args;
    }

    @Override
    public String toString() {
        try {
            String val = this.getValue();
            return val == null ? this.key : String.format(val, this.args);
        }
        catch (IllegalFormatException e) {
            String m = "The formatted message '" + this.key + "' uses an illegal format: " + e.getLocalizedMessage();
            Skript.adminBroadcast("<red>" + m);
            System.err.println("[Skript] " + m);
            e.printStackTrace();
            return "[ERROR]";
        }
    }
}

