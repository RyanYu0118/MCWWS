/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Objects;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface ExecutionIntent
extends Comparable<ExecutionIntent> {
    @Contract(value=" -> new", pure=true)
    public static StopTrigger stopTrigger() {
        return new StopTrigger();
    }

    @Contract(value="_ -> new", pure=true)
    public static StopSections stopSections(int levels) {
        Preconditions.checkArgument((levels > 0 ? 1 : 0) != 0, (Object)"Depth must be at least 1");
        return new StopSections(levels);
    }

    @Contract(value=" -> new", pure=true)
    public static StopSections stopSection() {
        return new StopSections(1);
    }

    @Nullable
    public ExecutionIntent use();

    public static final class StopTrigger
    implements ExecutionIntent {
        private StopTrigger() {
        }

        @Override
        public StopTrigger use() {
            return new StopTrigger();
        }

        @Override
        public int compareTo(@NotNull ExecutionIntent other) {
            return other instanceof StopTrigger ? 0 : 1;
        }

        public String toString() {
            return "StopTrigger";
        }
    }

    public static final class StopSections
    implements ExecutionIntent {
        private final int levels;

        private StopSections(int levels) {
            this.levels = levels;
        }

        @Override
        @Nullable
        public StopSections use() {
            return this.levels > 1 ? new StopSections(this.levels - 1) : null;
        }

        @Override
        public int compareTo(@NotNull ExecutionIntent other) {
            if (!(other instanceof StopSections)) {
                return other.compareTo(this) * -1;
            }
            int levels = ((StopSections)other).levels;
            return Integer.compare(this.levels, levels);
        }

        public int levels() {
            return this.levels;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            return this.levels == ((StopSections)obj).levels;
        }

        public int hashCode() {
            return Objects.hash(this.levels);
        }

        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("levels", this.levels).toString();
        }
    }
}

