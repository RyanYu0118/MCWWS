/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.yggdrasil;

public final class YggdrasilException
extends RuntimeException {
    private static final long serialVersionUID = -6130660396780458226L;

    public YggdrasilException(String message) {
        super(message);
    }

    public YggdrasilException(String message, Throwable cause) {
        super(message, cause);
    }

    public YggdrasilException(Throwable cause) {
        super(cause.getClass().getSimpleName() + (String)(cause.getMessage() == null ? "" : ": " + cause.getMessage()), cause);
    }
}

