/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.collect.ImmutableSet
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.util;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.util.Priority;

class PriorityImpl
implements Priority {
    private final Set<Priority> after;
    private final Set<Priority> before;

    PriorityImpl() {
        this.after = ImmutableSet.of();
        this.before = ImmutableSet.of();
    }

    PriorityImpl(Priority priority, boolean isBefore) {
        HashSet<Priority> after = new HashSet<Priority>();
        HashSet<Priority> before = new HashSet<Priority>();
        if (isBefore) {
            before.add(priority);
        } else {
            after.add(priority);
        }
        after.addAll(priority.after());
        before.addAll(priority.before());
        this.after = ImmutableSet.copyOf(after);
        this.before = ImmutableSet.copyOf(before);
    }

    @Override
    public int compareTo(@NotNull Priority other) {
        if (this == other) {
            return 0;
        }
        Collection<Priority> ourBefore = this.before();
        Collection<Priority> otherAfter = other.after();
        if (ourBefore.contains(other) || otherAfter.contains(this)) {
            return -1;
        }
        Collection<Priority> ourAfter = this.after();
        Collection<Priority> otherBefore = other.before();
        if (ourAfter.contains(other) || otherBefore.contains(this)) {
            return 1;
        }
        if (ourBefore.stream().anyMatch(otherAfter::contains)) {
            return -1;
        }
        if (ourAfter.stream().anyMatch(otherBefore::contains)) {
            return 1;
        }
        return other instanceof PriorityImpl ? 0 : other.compareTo(this) * -1;
    }

    @Override
    public Collection<Priority> after() {
        return this.after;
    }

    @Override
    public Collection<Priority> before() {
        return this.before;
    }

    public int hashCode() {
        return Objects.hash(this.after, this.before);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Priority)) {
            return false;
        }
        Priority priority = (Priority)obj;
        return this.compareTo(priority) == 0;
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("after", this.after).add("before", this.before).toString();
    }
}

