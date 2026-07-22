/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.log;

import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.SkriptLogger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class RedirectingLogHandler
extends LogHandler {
    private final Collection<CommandSender> recipients;
    private int numErrors = 0;
    private final String prefix;

    public RedirectingLogHandler(CommandSender recipient, @Nullable String prefix) {
        this(Collections.singletonList(recipient), prefix);
    }

    public RedirectingLogHandler(Collection<CommandSender> recipients, @Nullable String prefix) {
        this.recipients = new ArrayList<CommandSender>(recipients);
        this.prefix = prefix == null ? "" : prefix;
    }

    @Override
    public LogHandler.LogResult log(LogEntry entry) {
        return this.log(entry, null);
    }

    public LogHandler.LogResult log(LogEntry entry, @Nullable CommandSender ignore) {
        String formattedMessage = this.prefix + entry.toFormattedString();
        for (CommandSender recipient : this.recipients) {
            if (recipient == ignore) continue;
            SkriptLogger.sendFormatted(recipient, formattedMessage);
        }
        if (entry.level == Level.SEVERE) {
            ++this.numErrors;
        }
        return LogHandler.LogResult.DO_NOT_LOG;
    }

    @Override
    public RedirectingLogHandler start() {
        return SkriptLogger.startLogHandler(this);
    }

    public int numErrors() {
        return this.numErrors;
    }
}

