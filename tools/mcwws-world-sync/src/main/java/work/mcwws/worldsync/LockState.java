package work.mcwws.worldsync;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Local view of the exclusive write lock. The listen node is coordinator;
 * connect nodes mirror holder from heartbeats.
 */
public final class LockState {
    private final AtomicReference<String> holder = new AtomicReference<>("");
    private final AtomicLong generation = new AtomicLong(0);
    private final AtomicLong leaseUntilMillis = new AtomicLong(0);
    private final AtomicBoolean frozen = new AtomicBoolean(false);
    private final AtomicBoolean handoverPending = new AtomicBoolean(false);
    private final AtomicReference<String> handoverFrom = new AtomicReference<>("");
    private final AtomicBoolean joiningBlocked = new AtomicBoolean(false);
    private volatile String selfId = "";

    public void setSelf(String nodeId) {
        this.selfId = nodeId == null ? "" : nodeId;
    }

    public String self() {
        return selfId;
    }

    public boolean hasLock() {
        return selfId.equals(holder.get()) && !frozen.get();
    }

    public String holder() {
        String h = holder.get();
        return h == null ? "" : h;
    }

    public long generation() {
        return generation.get();
    }

    public boolean frozen() {
        return frozen.get();
    }

    public boolean handoverPending() {
        return handoverPending.get();
    }

    public String handoverFrom() {
        return handoverFrom.get();
    }

    public boolean joiningBlocked() {
        return joiningBlocked.get() || !hasLock();
    }

    public synchronized void setHolder(String nodeId, long gen, long leaseUntil, boolean frozenNow) {
        holder.set(nodeId == null ? "" : nodeId);
        generation.set(gen);
        leaseUntilMillis.set(leaseUntil);
        frozen.set(frozenNow);
        if (frozenNow) {
            joiningBlocked.set(true);
        }
    }

    public synchronized void incrementGeneration() {
        generation.incrementAndGet();
    }

    public void setHandover(boolean pending, String from) {
        handoverPending.set(pending);
        handoverFrom.set(from == null ? "" : from);
    }

    public void setJoiningBlocked(boolean blocked) {
        joiningBlocked.set(blocked);
    }

    public long leaseUntil() {
        return leaseUntilMillis.get();
    }

    public synchronized boolean claimLocal(long leaseMs) {
        if (frozen.get()) {
            return false;
        }
        String h = holder.get();
        if (!h.isEmpty() && !h.equals(selfId) && System.currentTimeMillis() < leaseUntilMillis.get()) {
            return false;
        }
        holder.set(selfId);
        leaseUntilMillis.set(System.currentTimeMillis() + leaseMs);
        frozen.set(false);
        joiningBlocked.set(false);
        return true;
    }

    public synchronized void refreshLease(long leaseMs) {
        if (hasLock()) {
            leaseUntilMillis.set(System.currentTimeMillis() + leaseMs);
        }
    }

    public synchronized void releaseIfSelf() {
        if (selfId.equals(holder.get())) {
            holder.set("");
            joiningBlocked.set(true);
        }
    }

    public synchronized void freeze() {
        frozen.set(true);
        joiningBlocked.set(true);
        holder.set("");
    }

    /** Coordinator: expire lease into frozen, never auto-grant. */
    public synchronized boolean expireIfNeeded() {
        String h = holder.get();
        if (h.isEmpty() || frozen.get()) {
            return false;
        }
        if (System.currentTimeMillis() > leaseUntilMillis.get()) {
            freeze();
            return true;
        }
        return false;
    }
}
