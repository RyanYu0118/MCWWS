/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.util.coll;

import ch.njol.util.coll.BidiMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.10.0", forRemoval=true)
public class BidiHashMap<T1, T2>
extends HashMap<T1, T2>
implements BidiMap<T1, T2> {
    private static final long serialVersionUID = -9011678701069901061L;
    private final BidiHashMap<T2, T1> other;

    private BidiHashMap(BidiHashMap<T2, T1> other) {
        this.other = other;
    }

    public BidiHashMap() {
        this.other = new BidiHashMap<T1, T2>(this);
    }

    public BidiHashMap(Map<? extends T1, ? extends T2> values) {
        this.other = new BidiHashMap<T1, T2>(this);
        this.putAll(values);
    }

    @Override
    public BidiHashMap<T2, T1> getReverseView() {
        return this.other;
    }

    @Override
    @Nullable
    public T1 getKey(T2 value) {
        return (T1)this.other.get(value);
    }

    @Override
    @Nullable
    public T2 getValue(@Nullable T1 key) {
        return (T2)this.get(key);
    }

    @Nullable
    private T2 putDirect(@Nullable T1 key, @Nullable T2 value) {
        return super.put(key, value);
    }

    @Override
    @Nullable
    public T2 put(@Nullable T1 key, @Nullable T2 value) {
        if (key == null || value == null) {
            throw new NullPointerException("Can't store null in a BidiHashMap");
        }
        this.removeDirect(key);
        this.other.removeDirect(value);
        T2 oldValue = this.putDirect(key, value);
        this.other.putDirect(value, key);
        return oldValue;
    }

    @Override
    public void putAll(Map<? extends T1, ? extends T2> m) {
        for (Map.Entry<T1, T2> e : m.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    @Nullable
    private T2 removeDirect(@Nullable Object key) {
        return (T2)super.remove(key);
    }

    @Override
    @Nullable
    public T2 remove(@Nullable Object key) {
        T2 oldValue = this.removeDirect(key);
        if (oldValue != null) {
            this.other.removeDirect(oldValue);
        }
        return oldValue;
    }

    private void clearDirect() {
        super.clear();
    }

    @Override
    public void clear() {
        this.clearDirect();
        this.other.clearDirect();
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return this.other.containsKey(value);
    }

    @Override
    public Set<Map.Entry<T1, T2>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet());
    }

    @Override
    public Set<T1> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public Set<T2> values() {
        return this.valueSet();
    }

    @Override
    public Set<T2> valueSet() {
        return Collections.unmodifiableSet(this.other.keySet());
    }

    @Override
    public BidiHashMap<T1, T2> clone() {
        return new BidiHashMap<T1, T2>(this);
    }
}

