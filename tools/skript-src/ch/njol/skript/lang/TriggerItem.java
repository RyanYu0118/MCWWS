/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Debuggable;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.util.StringUtils;
import java.io.File;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.lang.script.Script;

public abstract class TriggerItem
implements Debuggable {
    @Nullable
    protected TriggerSection parent = null;
    @Nullable
    private TriggerItem next = null;
    private static final String INDENT = "  ";
    @Nullable
    private String indentation = null;

    protected TriggerItem() {
    }

    protected TriggerItem(TriggerSection parent) {
        this.parent = parent;
    }

    @Nullable
    protected TriggerItem walk(Event event) {
        if (this.run(event)) {
            this.debug(event, true);
            return this.next;
        }
        this.debug(event, false);
        TriggerSection parent = this.parent;
        return parent == null ? null : parent.getNext();
    }

    protected abstract boolean run(Event var1);

    public static boolean walk(TriggerItem start, Event event) {
        TriggerItem triggerItem;
        try {
            for (triggerItem = start; triggerItem != null; triggerItem = triggerItem.walk(event)) {
            }
            return true;
        }
        catch (StackOverflowError err) {
            File scriptFile;
            Script script;
            Trigger trigger = start.getTrigger();
            String scriptName = "<unknown>";
            if (trigger != null && (script = trigger.getScript()) != null && (scriptFile = script.getConfig().getFile()) != null) {
                scriptName = scriptFile.getName();
            }
            Skript.adminBroadcast("<red>The script '<gold>" + scriptName + "<red>' infinitely (or excessively) repeated itself!");
            if (Skript.debug()) {
                err.printStackTrace();
            }
        }
        catch (Exception ex) {
            if (ex.getStackTrace().length != 0) {
                Skript.exception((Throwable)ex, triggerItem, new String[0]);
            }
        }
        catch (Throwable throwable) {
            Skript.markErrored();
            throw throwable;
        }
        return false;
    }

    @Nullable
    public ExecutionIntent executionIntent() {
        return null;
    }

    public String getIndentation() {
        if (this.indentation == null) {
            int level = 0;
            TriggerItem triggerItem = this;
            while ((triggerItem = triggerItem.parent) != null) {
                ++level;
            }
            this.indentation = StringUtils.multiply(INDENT, level);
        }
        return this.indentation;
    }

    protected final void debug(Event event, boolean run) {
        if (!Skript.debug()) {
            return;
        }
        Skript.debug(TextComponentParser.instance().escape(this.getIndentation() + (run ? "" : "-") + this.toString(event, true)));
    }

    @Override
    public final String toString() {
        return this.toString(null, false);
    }

    public TriggerItem setParent(@Nullable TriggerSection parent) {
        this.parent = parent;
        return this;
    }

    @Nullable
    public final TriggerSection getParent() {
        return this.parent;
    }

    @Nullable
    public final Trigger getTrigger() {
        TriggerItem triggerItem;
        for (triggerItem = this; triggerItem != null && !(triggerItem instanceof Trigger); triggerItem = triggerItem.getParent()) {
        }
        return (Trigger)triggerItem;
    }

    public TriggerItem setNext(@Nullable TriggerItem next) {
        this.next = next;
        return this;
    }

    @Nullable
    public TriggerItem getNext() {
        return this.next;
    }

    @Nullable
    public TriggerItem getActualNext() {
        return this.next;
    }
}

