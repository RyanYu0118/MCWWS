/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.update;

import ch.njol.skript.update.ReleaseChannel;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.UpdateManifest;
import java.util.concurrent.CompletableFuture;

public interface UpdateChecker {
    public CompletableFuture<UpdateManifest> check(ReleaseManifest var1, ReleaseChannel var2);
}

