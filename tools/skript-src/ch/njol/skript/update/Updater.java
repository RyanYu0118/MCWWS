/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.update;

import ch.njol.skript.Skript;
import ch.njol.skript.update.ReleaseChannel;
import ch.njol.skript.update.ReleaseManifest;
import ch.njol.skript.update.ReleaseStatus;
import ch.njol.skript.update.UpdateChecker;
import ch.njol.skript.update.UpdateManifest;
import ch.njol.skript.update.UpdaterState;
import ch.njol.skript.util.Task;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class Updater {
    private final ReleaseManifest currentRelease;
    private final UpdateChecker updateChecker;
    @Nullable
    private volatile ReleaseChannel releaseChannel;
    private volatile UpdaterState state;
    private volatile ReleaseStatus releaseStatus;
    private volatile long checkFrequency;
    @Nullable
    private volatile UpdateManifest updateManifest;
    private volatile boolean enabled;

    protected Updater(ReleaseManifest manifest) {
        this.currentRelease = manifest;
        this.updateChecker = manifest.createUpdateChecker();
        this.state = UpdaterState.NOT_STARTED;
        this.releaseStatus = ReleaseStatus.UNKNOWN;
        this.enabled = false;
    }

    public CompletableFuture<UpdateManifest> fetchUpdateManifest() {
        if (!this.enabled) {
            CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
            assert (future != null);
            return future;
        }
        ReleaseChannel channel = this.releaseChannel;
        if (channel == null) {
            throw new IllegalStateException("release channel must be specified");
        }
        return this.updateChecker.check(this.currentRelease, channel);
    }

    public CompletableFuture<Void> checkUpdates() {
        if (!this.enabled) {
            CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
            assert (future != null);
            return future;
        }
        if (this.currentRelease.flavor.contains("selfbuilt")) {
            this.releaseStatus = ReleaseStatus.CUSTOM;
            CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
            assert (future != null);
            return future;
        }
        if (this.currentRelease.flavor.contains("nightly")) {
            this.releaseStatus = ReleaseStatus.DEVELOPMENT;
            CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
            assert (future != null);
            return future;
        }
        this.state = UpdaterState.CHECKING;
        CompletionStage completed = ((CompletableFuture)this.fetchUpdateManifest().thenAccept(manifest -> {
            Updater updater = this;
            synchronized (updater) {
                if (manifest != null) {
                    this.releaseStatus = ReleaseStatus.OUTDATED;
                    this.updateManifest = manifest;
                } else {
                    this.releaseStatus = ReleaseStatus.LATEST;
                }
                this.state = UpdaterState.INACTIVE;
                long ticks = this.checkFrequency;
                if (ticks > 0L) {
                    new Task((Plugin)Skript.getInstance(), ticks, true){

                        @Override
                        public void run() {
                            Updater.this.checkUpdates();
                        }
                    };
                }
            }
        })).whenComplete((none, e) -> {
            if (e != null) {
                Updater updater = this;
                synchronized (updater) {
                    this.state = UpdaterState.ERROR;
                    Skript.error("Checking for updates failed. Do you have Internet connection?");
                    e.printStackTrace();
                }
            }
        });
        assert (completed != null);
        return completed;
    }

    public ReleaseManifest getCurrentRelease() {
        return this.currentRelease;
    }

    public void setReleaseChannel(ReleaseChannel channel) {
        this.releaseChannel = channel;
    }

    public void setCheckFrequency(long ticks) {
        this.checkFrequency = ticks;
    }

    public UpdaterState getState() {
        return this.state;
    }

    public ReleaseStatus getReleaseStatus() {
        return this.releaseStatus;
    }

    @Nullable
    public UpdateManifest getUpdateManifest() {
        return this.updateManifest;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}

