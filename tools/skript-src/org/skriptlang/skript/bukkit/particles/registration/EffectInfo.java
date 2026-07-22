/*
 * Decompiled with CFR 0.152.
 */
package org.skriptlang.skript.bukkit.particles.registration;

import org.skriptlang.skript.bukkit.particles.registration.DataSupplier;
import org.skriptlang.skript.bukkit.particles.registration.ToString;

public record EffectInfo<E, D>(E effect, String pattern, DataSupplier<D> dataSupplier, ToString toStringFunction) {
}

