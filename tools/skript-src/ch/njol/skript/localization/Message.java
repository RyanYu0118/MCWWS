/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Language;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public class Message {
    private static final Collection<Message> messages = new ArrayList<Message>(50);
    private static boolean firstChange = true;
    public final String key;
    @Nullable
    private String value;
    boolean revalidate = true;

    public Message(String key) {
        this.key = key.toLowerCase(Locale.ENGLISH);
        messages.add(this);
        if (Skript.testing() && Language.isInitialized() && !Language.keyExists(this.key)) {
            Language.missingEntryError(this.key);
        }
    }

    public String toString() {
        this.validate();
        return this.value == null ? this.key : this.value;
    }

    @Nullable
    public final String getValue() {
        this.validate();
        return this.value;
    }

    public final String getValueOrDefault(String defaultValue) {
        this.validate();
        return this.value == null ? defaultValue : this.value;
    }

    public final boolean isSet() {
        this.validate();
        return this.value != null;
    }

    protected synchronized void validate() {
        if (this.revalidate) {
            this.revalidate = false;
            this.value = Language.get_(this.key);
            this.onValueChange();
        }
    }

    protected void onValueChange() {
    }

    static {
        Language.addListener(() -> {
            Iterator<Message> i$ = messages.iterator();
            while (i$.hasNext()) {
                Message m;
                Message message = m = i$.next();
                synchronized (message) {
                    m.revalidate = true;
                }
                if (!firstChange || !Skript.testing() || Language.keyExists(m.key)) continue;
                Language.missingEntryError(m.key);
            }
            firstChange = false;
        });
    }
}

