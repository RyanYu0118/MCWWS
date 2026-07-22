/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.yggdrasil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface YggdrasilID {
    public String value();
}

