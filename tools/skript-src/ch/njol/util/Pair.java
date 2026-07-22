/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Deprecated(since="2.10.0", forRemoval=true)
public class Pair<T1, T2>
implements Map.Entry<T1, T2>,
Cloneable,
Serializable {
    private static final long serialVersionUID = 8296563685697678334L;
    protected @UnknownNullability T1 first;
    protected @UnknownNullability T2 second;

    public Pair() {
        this.first = null;
        this.second = null;
    }

    public Pair(@Nullable T1 first, @Nullable T2 second) {
        this.first = first;
        this.second = second;
    }

    public Pair(@NotNull Map.Entry<T1, T2> entry) {
        this.first = entry.getKey();
        this.second = entry.getValue();
    }

    public @UnknownNullability T1 getFirst() {
        return this.first;
    }

    public void setFirst(@Nullable T1 first) {
        this.first = first;
    }

    public @UnknownNullability T2 getSecond() {
        return this.second;
    }

    public void setSecond(@Nullable T2 second) {
        this.second = second;
    }

    public String toString() {
        return String.valueOf(this.first) + "," + String.valueOf(this.second);
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map.Entry)) {
            return false;
        }
        Map.Entry entry = (Map.Entry)obj;
        T1 first = this.first;
        T2 second = this.second;
        return (first == null ? entry.getKey() == null : first.equals(entry.getKey())) && (second == null ? entry.getValue() == null : second.equals(entry.getValue()));
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.first, this.second);
    }

    @Override
    public @UnknownNullability T1 getKey() {
        return this.first;
    }

    @Override
    public @UnknownNullability T2 getValue() {
        return this.second;
    }

    @Override
    public @UnknownNullability T2 setValue(@Nullable T2 value) {
        T2 old = this.second;
        this.second = value;
        return old;
    }

    public Pair<T1, T2> clone() {
        return new Pair<T1, T2>(this);
    }
}

