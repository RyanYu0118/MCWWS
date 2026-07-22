/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil.block;

import ch.njol.skript.aliases.MatchQuality;
import ch.njol.yggdrasil.YggdrasilSerializable;
import org.jetbrains.annotations.Nullable;

public abstract class BlockValues
implements YggdrasilSerializable.YggdrasilExtendedSerializable {
    public abstract boolean isDefault();

    public abstract MatchQuality match(BlockValues var1);

    public abstract boolean equals(@Nullable Object var1);

    public abstract int hashCode();
}

