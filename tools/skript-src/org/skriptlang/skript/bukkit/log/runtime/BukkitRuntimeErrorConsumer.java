/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.bukkit.log.runtime;

import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.log.SkriptLogger;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.log.runtime.ErrorSource;
import org.skriptlang.skript.log.runtime.Frame;
import org.skriptlang.skript.log.runtime.RuntimeError;
import org.skriptlang.skript.log.runtime.RuntimeErrorConsumer;

public class BukkitRuntimeErrorConsumer
implements RuntimeErrorConsumer {
    public static final String ERROR_NOTIF_PERMISSION = "skript.see_runtime_errors";
    public static final String CONFIG_NODE = "log.runtime";
    public static final ArgsMessage WARNING_DETAILS = new ArgsMessage("skript command.reload.warning details");
    public static final ArgsMessage ERROR_DETAILS = new ArgsMessage("skript command.reload.error details");
    public static final ArgsMessage OTHER_DETAILS = new ArgsMessage("skript command.reload.other details");
    public static final ArgsMessage ERROR_INFO = new ArgsMessage("log.runtime.error");
    public static final ArgsMessage WARNING_INFO = new ArgsMessage("log.runtime.warning");
    public static final ArgsMessage LINE_INFO = new ArgsMessage("log.runtime.line info");
    public static final ArgsMessage ERROR_SKIPPED = new ArgsMessage("log.runtime.errors skipped");
    public static final ArgsMessage ERROR_TIMEOUT = new ArgsMessage("log.runtime.errors timed out");
    public static final ArgsMessage ERROR_NOTIF = new ArgsMessage("log.runtime.error notification");
    public static final ArgsMessage ERROR_NOTIF_PLURAL = new ArgsMessage("log.runtime.error notification plural");
    public static final ArgsMessage WARNING_SKIPPED = new ArgsMessage("log.runtime.warnings skipped");
    public static final ArgsMessage WARNING_TIMEOUT = new ArgsMessage("log.runtime.warnings timed out");
    public static final ArgsMessage WARNING_NOTIF = new ArgsMessage("log.runtime.warning notification");
    public static final ArgsMessage WARNING_NOTIF_PLURAL = new ArgsMessage("log.runtime.warning notification plural");

    @Override
    public void printError(@NotNull RuntimeError error) {
        ArgsMessage info;
        ArgsMessage details;
        if (error.level().intValue() == Level.WARNING.intValue()) {
            details = WARNING_DETAILS;
            info = WARNING_INFO;
        } else if (error.level().intValue() == Level.SEVERE.intValue()) {
            details = ERROR_DETAILS;
            info = ERROR_INFO;
        } else {
            details = OTHER_DETAILS;
            info = WARNING_INFO;
        }
        String skriptInfo = BukkitRuntimeErrorConsumer.replaceNewline(info.getValue() == null ? info.key : info.getValue());
        String errorInfo = BukkitRuntimeErrorConsumer.replaceNewline(details.getValue() == null ? details.key : details.getValue());
        String lineInfo = BukkitRuntimeErrorConsumer.replaceNewline(LINE_INFO.getValue() == null ? BukkitRuntimeErrorConsumer.LINE_INFO.key : LINE_INFO.getValue());
        ErrorSource source = error.source();
        String code = source.lineText();
        if (error.toHighlight() != null && !error.toHighlight().isEmpty()) {
            code = code.replace("\u00a7", "&").replaceFirst(error.toHighlight(), "\u00a7f\u00a7n$0\u00a77");
        }
        SkriptLogger.sendFormatted((CommandSender)Bukkit.getConsoleSender(), String.format(skriptInfo, source.script(), source.syntaxName(), source.syntaxType()) + String.format(errorInfo, error.error().replace("\u00a7", "&")) + String.format(lineInfo, source.lineNumber(), code));
    }

    @Override
    public void printFrameOutput(@NotNull Frame.FrameOutput output, Level level) {
        if (!output.skippedErrors().isEmpty()) {
            String linesNotDisplayed = output.skippedErrors().entrySet().stream().map(entry -> "'" + ((ErrorSource.Location)entry.getKey()).script() + "' line " + ((ErrorSource.Location)entry.getKey()).lineNumber() + " (" + String.valueOf(entry.getValue()) + ")").collect(Collectors.joining(", "));
            int skipped = output.skippedErrors().values().stream().reduce(0, Integer::sum);
            ArgsMessage message = level == Level.SEVERE ? ERROR_SKIPPED : WARNING_SKIPPED;
            String line = message.getValue() != null ? message.getValue() : message.key;
            SkriptLogger.sendFormatted((CommandSender)Bukkit.getConsoleSender(), String.format(line, skipped, linesNotDisplayed));
        }
        ArgsMessage message = level == Level.SEVERE ? ERROR_TIMEOUT : WARNING_TIMEOUT;
        String timeoutLine = message.getValue() != null ? message.getValue() : message.key;
        for (ErrorSource.Location location : output.newTimeouts()) {
            SkriptLogger.sendFormatted((CommandSender)Bukkit.getConsoleSender(), String.format(timeoutLine, location.lineNumber(), location.script(), output.frameLimits().lineTimeoutLimit(), output.frameLimits().timeoutDuration()));
        }
        if (!output.totalErrors().isEmpty()) {
            ArgsMessage notification;
            Set scripts = output.totalErrors().keySet().stream().map(ErrorSource.Location::script).collect(Collectors.toSet());
            ArgsMessage argsMessage = notification = level == Level.SEVERE ? ERROR_NOTIF : WARNING_NOTIF;
            if (scripts.size() > 1) {
                notification = level == Level.SEVERE ? ERROR_NOTIF_PLURAL : WARNING_NOTIF_PLURAL;
            }
            String notif = notification.getValue() != null ? notification.getValue() : notification.key;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission(ERROR_NOTIF_PERMISSION)) continue;
                SkriptLogger.sendFormatted((CommandSender)player, String.format(notif, scripts.iterator().next(), scripts.size() - 1));
            }
        }
    }

    @NotNull
    private static String replaceNewline(@NotNull String s) {
        return s.replaceAll("\\\\n", "\n");
    }
}

