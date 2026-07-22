/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.chat;

import ch.njol.skript.util.chat.MessageComponent;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.15", forRemoval=true)
public interface ChatCode {
    public void updateComponent(MessageComponent var1, String var2);

    public boolean hasParam();

    @Nullable
    public String getColorCode();

    @Nullable
    public String getLangName();

    default public boolean isLocalized() {
        return false;
    }

    public char getColorChar();
}

