/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang.parser;

import ch.njol.skript.classes.ClassInfo;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class ExpressionParseCache {
    private final Deque<Set<Failure>> stack = new ArrayDeque<Set<Failure>>();

    public void push() {
        this.stack.push(new HashSet());
    }

    public void pop() {
        this.stack.poll();
    }

    public boolean contains(Failure failure) {
        Set<Failure> current = this.stack.peek();
        return current != null && current.contains(failure);
    }

    public void add(Failure failure) {
        Set<Failure> current = this.stack.peek();
        if (current != null) {
            current.add(failure);
        }
    }

    public void clear() {
        this.stack.clear();
    }

    public record Failure(String substring, int effectiveFlags, ClassInfo<?>[] classes, boolean[] isPlural, boolean isNullable, int time) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Failure)) {
                return false;
            }
            Failure other = (Failure)obj;
            return this.effectiveFlags == other.effectiveFlags && this.isNullable == other.isNullable && this.time == other.time && this.substring.equals(other.substring) && Arrays.equals(this.classes, other.classes) && Arrays.equals(this.isPlural, other.isPlural);
        }

        @Override
        public int hashCode() {
            int hash = this.substring.hashCode() * 31 + this.effectiveFlags;
            hash = hash * 31 + Arrays.hashCode(this.classes);
            hash = hash * 31 + Arrays.hashCode(this.isPlural);
            hash = hash * 31 + Boolean.hashCode(this.isNullable);
            hash = hash * 31 + this.time;
            return hash;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("Failure{\"").append(this.substring).append("\" as ");
            for (int i = 0; i < this.classes.length; ++i) {
                if (i > 0) {
                    result.append('/');
                }
                result.append(this.classes[i].getCodeName());
                if (!this.isPlural[i]) continue;
                result.append('s');
            }
            if (this.isNullable) {
                result.append(" (nullable)");
            }
            if (this.time != 0) {
                result.append(" @").append(this.time);
            }
            result.append(" flags=").append(this.effectiveFlags).append('}');
            return result.toString();
        }
    }
}

