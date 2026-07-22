/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.classes;

import ch.njol.util.Checker;
import java.util.function.Predicate;

@FunctionalInterface
@Deprecated(since="2.10.0", forRemoval=true)
public interface SerializableChecker<T>
extends Checker<T>,
Predicate<T> {
}

