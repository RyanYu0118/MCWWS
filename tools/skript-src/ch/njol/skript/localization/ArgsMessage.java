/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Message;
import java.util.IllegalFormatException;

public final class ArgsMessage
extends Message {
    public ArgsMessage(String key) {
        super(key);
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    public String toString(Object ... args) {
        try {
            String val = this.getValue();
            return val == null ? this.key : String.format(val, args);
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

