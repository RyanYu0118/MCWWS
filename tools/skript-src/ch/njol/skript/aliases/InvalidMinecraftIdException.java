/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.aliases;

public class InvalidMinecraftIdException
extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    private final String id;

    public InvalidMinecraftIdException(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
}

