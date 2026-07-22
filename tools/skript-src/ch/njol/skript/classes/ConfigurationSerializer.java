/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.InvalidConfigurationException
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.configuration.serialization.ConfigurationSerializable
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.yggdrasil.Fields;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.Nullable;

public class ConfigurationSerializer<T extends ConfigurationSerializable>
extends Serializer<T> {
    @Override
    public Fields serialize(T o) throws NotSerializableException {
        Fields f = new Fields();
        f.putObject("value", ConfigurationSerializer.serializeCS(o));
        return f;
    }

    @Override
    public boolean mustSyncDeserialization() {
        return false;
    }

    @Override
    public boolean canBeInstantiated() {
        return false;
    }

    @Override
    protected T deserialize(Fields fields) throws StreamCorruptedException {
        String val = fields.getObject("value", String.class);
        if (val == null) {
            throw new StreamCorruptedException();
        }
        ClassInfo info = this.info;
        assert (info != null);
        Object t = ConfigurationSerializer.deserializeCS(val, info.getC());
        if (t == null) {
            throw new StreamCorruptedException();
        }
        return t;
    }

    public static String serializeCS(ConfigurationSerializable o) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("value", (Object)o);
        return y.saveToString();
    }

    @Nullable
    public static <T extends ConfigurationSerializable> T deserializeCS(String s, Class<T> c) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(s);
        }
        catch (InvalidConfigurationException e) {
            return null;
        }
        Object o = y.get("value");
        if (!c.isInstance(o)) {
            return null;
        }
        return (T)((ConfigurationSerializable)o);
    }

    @Override
    @Nullable
    public <E extends T> E newInstance(Class<E> c) {
        assert (false);
        return null;
    }

    @Override
    public void deserialize(T o, Fields fields) throws StreamCorruptedException {
        assert (false);
    }

    @Override
    @Deprecated(since="2.3.0", forRemoval=true)
    @Nullable
    public T deserialize(String s) {
        ClassInfo info = this.info;
        assert (info != null);
        return ConfigurationSerializer.deserializeCSOld(s, info.getC());
    }

    @Deprecated(since="2.3.0", forRemoval=true)
    @Nullable
    public static <T extends ConfigurationSerializable> T deserializeCSOld(String s, Class<T> c) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(s.replace("\ufeff", "\n"));
        }
        catch (InvalidConfigurationException e) {
            return null;
        }
        Object o = y.get("value");
        if (!c.isInstance(o)) {
            return null;
        }
        return (T)((ConfigurationSerializable)o);
    }
}

