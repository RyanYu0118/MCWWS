/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.classes.registry;

import ch.njol.skript.classes.Serializer;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;

public class RegistrySerializer<R extends Keyed>
extends Serializer<R> {
    private final Registry<R> registry;

    public RegistrySerializer(Registry<R> registry) {
        this.registry = registry;
    }

    @Override
    @NotNull
    public Fields serialize(R o) {
        Fields fields = new Fields();
        fields.putObject("name", o.getKey().toString());
        return fields;
    }

    @Override
    protected R deserialize(Fields fields) throws StreamCorruptedException {
        String name = fields.getAndRemoveObject("name", String.class);
        assert (name != null);
        NamespacedKey namespacedKey = !name.contains(":") ? NamespacedKey.minecraft((String)name) : NamespacedKey.fromString((String)name);
        if (namespacedKey == null) {
            throw new StreamCorruptedException("Invalid namespacedkey: " + name);
        }
        Keyed object = this.registry.get(namespacedKey);
        if (object == null) {
            throw new StreamCorruptedException("Invalid object from registry: " + String.valueOf(namespacedKey));
        }
        return (R)object;
    }

    @Override
    public boolean mustSyncDeserialization() {
        return false;
    }

    @Override
    protected boolean canBeInstantiated() {
        return false;
    }

    @Override
    public void deserialize(R o, Fields f) {
        throw new UnsupportedOperationException();
    }
}

