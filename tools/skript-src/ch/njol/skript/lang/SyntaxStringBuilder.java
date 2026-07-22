/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Debuggable;
import com.google.common.base.Preconditions;
import java.util.StringJoiner;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SyntaxStringBuilder {
    private final boolean debug;
    @Nullable
    private final Event event;
    private final StringJoiner joiner = new StringJoiner(" ");

    public SyntaxStringBuilder(@Nullable Event event, boolean debug) {
        this.event = event;
        this.debug = debug;
    }

    public SyntaxStringBuilder append(@NotNull Object object) {
        Preconditions.checkNotNull((Object)object);
        if (object instanceof Debuggable) {
            Debuggable debuggable = (Debuggable)object;
            this.joiner.add(debuggable.toString(this.event, this.debug));
        } else {
            this.joiner.add(object.toString());
        }
        return this;
    }

    public SyntaxStringBuilder append(Object ... objects) {
        for (Object object : objects) {
            this.append(object);
        }
        return this;
    }

    public SyntaxStringBuilder appendIf(boolean condition, Object object) {
        if (condition) {
            this.append(object);
        }
        return this;
    }

    public SyntaxStringBuilder appendIf(boolean condition, Object ... objects) {
        if (condition) {
            this.append(objects);
        }
        return this;
    }

    public String toString() {
        return this.joiner.toString();
    }
}

