/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Repeatable(value=Examples.class)
@Documented
public @interface Example {
    public String value();

    public boolean inTrigger() default true;

    @Target(value={ElementType.TYPE})
    @Retention(value=RetentionPolicy.RUNTIME)
    @Documented
    public static @interface Examples {
        public Example[] value();
    }
}

