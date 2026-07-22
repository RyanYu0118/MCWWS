/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.properties;

import java.util.HashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Experimental
public class PropertyMap<Handler extends PropertyHandler<?>>
extends HashMap<Class<?>, Property.PropertyInfo<Handler>> {
    @Nullable
    public Handler getHandler(Class<?> inputClass) {
        Property.PropertyInfo<Handler> propertyInfo = this.get(inputClass);
        if (propertyInfo == null) {
            return null;
        }
        return propertyInfo.handler();
    }

    public Property.PropertyInfo<Handler> get(Class<?> actualClass) {
        if (super.containsKey(actualClass)) {
            return (Property.PropertyInfo)super.get(actualClass);
        }
        Class closestClass = null;
        for (Class candidateClass : this.keySet()) {
            if (!candidateClass.isAssignableFrom(actualClass) || closestClass != null && !closestClass.isAssignableFrom(candidateClass)) continue;
            closestClass = candidateClass;
        }
        Property.PropertyInfo propertyInfo = (Property.PropertyInfo)super.get(closestClass);
        this.put(actualClass, propertyInfo);
        return propertyInfo;
    }
}

