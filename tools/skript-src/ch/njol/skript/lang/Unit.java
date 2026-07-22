/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.lang;

public interface Unit
extends Cloneable {
    public int getAmount();

    public void setAmount(double var1);

    public String toString();

    public String toString(int var1);

    public Unit clone();
}

