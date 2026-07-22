/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.properties;

import ch.njol.skript.Skript;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.util.Registry;

@ApiStatus.Experimental
public class PropertyRegistry
implements Registry<Property<?>> {
    private final Map<String, Property<?>> properties;
    private final Skript skript;

    public PropertyRegistry(Skript skript) {
        this.skript = skript;
        this.properties = new HashMap();
    }

    public boolean register(@NotNull Property<?> property) {
        String name = property.name();
        if (this.properties.containsKey(name)) {
            Skript.error("Property '" + name + "' is already registered by " + this.properties.get(name).provider().name() + ".");
            return false;
        }
        this.properties.put(name, property);
        Skript.debug("Registered property '" + name + "' provided by " + property.provider().name() + ".");
        return true;
    }

    public boolean unregister(@NotNull Property<?> property) {
        String name = property.name();
        return this.unregister(name);
    }

    public boolean unregister(String name) {
        if (!this.properties.containsKey(name = name.toLowerCase(Locale.ENGLISH))) {
            Skript.error("Property '" + name + "' is not registered and cannot be unregistered.");
            return false;
        }
        this.properties.remove(name);
        Skript.debug("Unregistered property '" + name + "'.");
        return true;
    }

    @Override
    public @Unmodifiable Collection<Property<?>> elements() {
        return Collections.unmodifiableCollection(this.properties.values());
    }

    public Property<?> get(String name) {
        return this.properties.get(name);
    }

    public boolean isRegistered(@NotNull Property<?> property) {
        return this.isRegistered(property.name());
    }

    public boolean isRegistered(@NotNull String name) {
        return this.properties.containsKey(name.toLowerCase(Locale.ENGLISH));
    }
}

