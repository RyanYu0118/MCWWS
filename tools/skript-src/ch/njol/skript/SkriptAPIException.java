/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript;

public class SkriptAPIException
extends RuntimeException {
    private static final long serialVersionUID = -4556442222803379002L;

    public SkriptAPIException(String message) {
        super(message);
    }

    public SkriptAPIException(String message, Throwable cause) {
        super(message, cause);
    }

    public static void inaccessibleConstructor(Class<?> c, IllegalAccessException e) throws SkriptAPIException {
        throw new SkriptAPIException("The constructor of " + c.getName() + " and/or the class itself is/are not public", e);
    }

    public static void instantiationException(Class<?> c, InstantiationException e) throws SkriptAPIException {
        throw new SkriptAPIException(c.getName() + " can't be instantiated, likely because the class is abstract or has no nullary constructor", e);
    }

    public static void instantiationException(String desc, Class<?> c, InstantiationException e) throws SkriptAPIException {
        throw new SkriptAPIException(desc + " " + c.getName() + " can't be instantiated, likely because the class is abstract or has no nullary constructor", e);
    }
}

