/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.update;

import ch.njol.skript.update.ReleaseChannel;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.UpdateChecker;
import ch.njol.skript.update.UpdateManifest;
import java.util.concurrent.CompletableFuture;

public class NoUpdateChecker
implements UpdateChecker {
    @Override
    public CompletableFuture<UpdateManifest> check(ReleaseManifest manifest, ReleaseChannel channel) {
        return CompletableFuture.completedFuture(null);
    }
}

