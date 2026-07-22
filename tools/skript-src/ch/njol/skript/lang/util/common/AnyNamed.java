/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.lang.util.common;

import ch.njol.skript.lang.util.common.AnyProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.bukkit.text.TextComponentParser;

@FunctionalInterface
@Deprecated(since="2.13", forRemoval=true)
public interface AnyNamed
extends AnyProvider {
    public @UnknownNullability String name();

    default public @UnknownNullability Component nameComponent() {
        String name = this.name();
        return name == null ? null : Component.text((String)name);
    }

    default public boolean supportsNameChange() {
        return false;
    }

    default public void setName(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    default public void setName(Component name) throws UnsupportedOperationException {
        this.setName(TextComponentParser.instance().toString(name));
    }
}

