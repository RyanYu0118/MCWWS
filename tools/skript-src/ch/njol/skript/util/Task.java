/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.util.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class Task
implements Runnable,
Closeable {
    private final boolean async;
    private final Plugin plugin;
    private boolean useScriptLoaderExecutor;
    private long period = -1L;
    private int taskID = -1;

    public Task(Plugin plugin, long delay, long period) {
        this(plugin, delay, period, false);
    }

    public Task(Plugin plugin, long delay, long period, boolean async) {
        this.plugin = plugin;
        this.period = period;
        this.async = async;
        this.schedule(delay);
    }

    public Task(Plugin plugin, long delay) {
        this(plugin, false, delay, false);
    }

    public Task(Plugin plugin, boolean useScriptLoaderExecutor) {
        this(plugin, useScriptLoaderExecutor, 0L, false);
    }

    public Task(Plugin plugin, long delay, boolean async) {
        this(plugin, delay, -1L, async);
    }

    public Task(Plugin plugin, boolean useScriptLoaderExecutor, long delay) {
        this(plugin, useScriptLoaderExecutor, delay, false);
    }

    private Task(Plugin plugin, boolean useScriptLoaderExecutor, long delay, boolean async) {
        this.useScriptLoaderExecutor = useScriptLoaderExecutor;
        this.plugin = plugin;
        this.async = async;
        this.schedule(delay);
    }

    private void schedule(long delay) {
        assert (!this.isAlive());
        if (!Skript.getInstance().isEnabled()) {
            return;
        }
        if (this.useScriptLoaderExecutor) {
            Executor executor = ScriptLoader.getExecutor();
            if (delay > 0L) {
                this.taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> executor.execute(this), delay);
            } else {
                executor.execute(this);
            }
        } else {
            this.taskID = this.period == -1L ? (this.async ? Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, (Runnable)this, delay).getTaskId() : Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, (Runnable)this, delay)) : (this.async ? Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, (Runnable)this, delay, this.period).getTaskId() : Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, (Runnable)this, delay, this.period));
            assert (this.taskID != -1);
        }
    }

    public final boolean isAlive() {
        if (this.taskID == -1) {
            return false;
        }
        return Bukkit.getScheduler().isQueued(this.taskID) || Bukkit.getScheduler().isCurrentlyRunning(this.taskID);
    }

    public final void cancel() {
        if (this.taskID != -1) {
            Bukkit.getScheduler().cancelTask(this.taskID);
            this.taskID = -1;
        }
    }

    @Override
    public void close() {
        this.cancel();
    }

    public void setNextExecution(long delay) {
        assert (delay >= 0L);
        this.cancel();
        this.schedule(delay);
    }

    public void setPeriod(long period) {
        assert (period == -1L || period > 0L);
        if (period == this.period) {
            return;
        }
        this.period = period;
        if (this.isAlive()) {
            this.cancel();
            if (period != -1L) {
                this.schedule(period);
            }
        }
    }

    @Nullable
    public static <T> T callSync(Callable<T> c) {
        return Task.callSync(c, (Plugin)Skript.getInstance());
    }

    @Nullable
    public static <T> T callSync(Callable<T> c, Plugin p) {
        if (Bukkit.isPrimaryThread()) {
            try {
                return c.call();
            }
            catch (Exception e) {
                Skript.exception((Throwable)e, new String[0]);
            }
        }
        Future f = Bukkit.getScheduler().callSyncMethod(p, c);
        try {
            while (true) {
                try {
                    return (T)f.get();
                }
                catch (InterruptedException interruptedException) {
                    continue;
                }
                break;
            }
        }
        catch (ExecutionException e) {
            Skript.exception((Throwable)e, new String[0]);
        }
        catch (CancellationException cancellationException) {
            // empty catch block
        }
        return null;
    }
}

