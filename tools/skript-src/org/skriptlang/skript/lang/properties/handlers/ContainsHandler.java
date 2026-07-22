/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 */
package org.skriptlang.skript.lang.properties.handlers;

import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Experimental
public interface ContainsHandler<Container, Element>
extends PropertyHandler<Container> {
    public boolean contains(Container var1, Element var2);

    public Class<? extends Element>[] elementTypes();

    default public boolean canContain(Class<?> type) {
        for (Class<Element> elementType : this.elementTypes()) {
            if (!elementType.isAssignableFrom(type)) continue;
            return true;
        }
        return false;
    }
}

