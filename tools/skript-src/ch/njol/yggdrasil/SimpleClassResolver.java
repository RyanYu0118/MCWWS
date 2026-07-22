/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.BiMap
 *  com.google.common.collect.HashBiMap
 *  javax.annotation.concurrent.NotThreadSafe
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.ClassResolver;
import ch.njol.yggdrasil.YggdrasilException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.jetbrains.annotations.Nullable;

@NotThreadSafe
public class SimpleClassResolver
implements ClassResolver {
    private final BiMap<Class<?>, String> classes = HashBiMap.create();

    public void registerClass(Class<?> type, String id) {
        String oldId = (String)this.classes.put(type, (Object)id);
        if (oldId != null && !oldId.equals(id)) {
            throw new YggdrasilException("Changed id of " + String.valueOf(type) + " from " + oldId + " to " + id);
        }
    }

    @Override
    @Nullable
    public Class<?> getClass(String id) {
        return (Class)this.classes.inverse().get((Object)id);
    }

    @Override
    @Nullable
    public String getID(Class<?> type) {
        if (this.classes.containsKey(type)) {
            return (String)this.classes.get(type);
        }
        Class<?> closestClass = null;
        String closestId = null;
        for (Map.Entry entry : this.classes.entrySet()) {
            Class<?> current = entry.getClass();
            if (!current.isAssignableFrom(type) || closestClass != null && !closestClass.isAssignableFrom(current)) continue;
            closestClass = current;
            closestId = (String)entry.getValue();
        }
        return closestId;
    }
}

