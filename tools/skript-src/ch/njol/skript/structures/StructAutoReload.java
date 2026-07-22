/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralString;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.PluralizingArgsMessage;
import ch.njol.skript.log.LogEntry;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.log.TimingLogHandler;
import ch.njol.skript.util.Task;
import ch.njol.util.OpenCloseable;
import ch.njol.util.StringUtils;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.script.ScriptData;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;

@Name(value="Auto Reload")
@Description(value={"Place at the top of a script file to enable and configure automatic reloading of the script.\nWhen the script is saved, Skript will automatically reload the script.\nThe config.sk node 'script loader thread size' must be set to a positive number (async or parallel loading) for this to be enabled.\n\navailable optional nodes:\n\trecipients: The players to send reload messages to. Defaults to console.\n\tpermission: The permission required to receive reload messages. 'recipients' will override this node.\n"})
@Example.Examples(value={@Example(value="auto reload"), @Example(value="auto reload:\n\trecipients: \"SkriptDev\",  \"61699b2e-d327-4a01-9f1e-0ea8c3f06bc6\" and \"Njol\"\n\tpermission: \"skript.reloadnotify\"\n")})
@Since(value={"2.13"})
public class StructAutoReload
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(10);
    private static final EntryValidator VALIDATOR = EntryValidator.builder().addEntryData(new ExpressionEntryData<String>("recipients", null, true, String.class, 1)).addEntry("permission", "skript.reloadnotify", true).build();
    private Script script;
    private Task task;

    @Override
    public boolean init(Literal<?> @NotNull [] arguments, int pattern, SkriptParser.ParseResult result, EntryContainer container) {
        if (!ScriptLoader.isAsync()) {
            Skript.error(Language.get("log.auto reload.async required"));
            return false;
        }
        String[] recipients = null;
        String permission = "skript.reloadnotify";
        if (container != null) {
            Expression expression = (Expression)container.getOptional("recipients", false);
            ArrayList<String> strings = new ArrayList<String>();
            if (expression instanceof LiteralString) {
                LiteralString literal = (LiteralString)expression;
                strings.add(literal.getSingle());
            } else if (expression instanceof ExpressionList) {
                ExpressionList list = (ExpressionList)expression;
                list.getAllExpressions().forEach(expr -> {
                    if (expr instanceof LiteralString) {
                        LiteralString literalString = (LiteralString)expr;
                        strings.add(literalString.getSingle());
                    }
                });
            }
            if (!strings.isEmpty()) {
                recipients = (String[])strings.toArray(String[]::new);
            }
            permission = (String)container.getOptional("permission", String.class, false);
        }
        this.script = this.getParser().getCurrentScript();
        File file = this.script.getConfig().getFile();
        if (file == null || !file.exists()) {
            Skript.error(Language.get("log.auto reload.file not found"));
            return false;
        }
        this.script.addData(new AutoReload(file.lastModified(), permission, recipients));
        return true;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public boolean postLoad() {
        this.task = new Task((Plugin)Skript.getInstance(), 0L, 40L, true){

            @Override
            public void run() {
                AutoReload data = StructAutoReload.this.script.getData(AutoReload.class);
                File file = StructAutoReload.this.script.getConfig().getFile();
                if (data == null || file == null || !file.exists()) {
                    return;
                }
                long lastModified = file.lastModified();
                if (lastModified <= data.getLastReloadTime()) {
                    return;
                }
                data.setLastReloadTime(lastModified);
                try (RedirectingLogHandler logHandler = new RedirectingLogHandler(data.getRecipients(), "").start();
                     TimingLogHandler timingLogHandler = new TimingLogHandler().start();){
                    StructAutoReload.this.reloading(logHandler);
                    OpenCloseable openCloseable = OpenCloseable.combine(logHandler, timingLogHandler);
                    ScriptLoader.reloadScript(StructAutoReload.this.script, openCloseable).thenRun(() -> StructAutoReload.this.reloaded(logHandler, timingLogHandler));
                }
                catch (Exception e) {
                    Skript.exception((Throwable)e, "Exception occurred while automatically reloading a script", StructAutoReload.this.script.getConfig().getFileName());
                }
            }
        };
        return true;
    }

    @Override
    public void unload() {
        if (this.task != null) {
            this.task.cancel();
        }
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "auto reload";
    }

    private void reloading(RedirectingLogHandler logHandler) {
        String prefix = Language.get("skript.prefix");
        String what = PluralizingArgsMessage.format(Language.format("log.auto reload.script", this.script.getConfig().getFileName()));
        String message = StringUtils.fixCapitalization(PluralizingArgsMessage.format(Language.format("log.auto reload.reloading", what)));
        logHandler.log(new LogEntry(Level.INFO, prefix + message));
    }

    private void reloaded(RedirectingLogHandler logHandler, TimingLogHandler timingLogHandler) {
        String prefix = Language.get("skript.prefix");
        ArgsMessage m_reload_error = new ArgsMessage("log.auto reload.error");
        ArgsMessage m_reloaded = new ArgsMessage("log.auto reload.reloaded");
        String what = PluralizingArgsMessage.format(Language.format("log.auto reload.script", this.script.getConfig().getFileName()));
        String timeTaken = String.valueOf(timingLogHandler.getTimeTaken());
        if (logHandler.numErrors() == 0) {
            String message = StringUtils.fixCapitalization(PluralizingArgsMessage.format(m_reloaded.toString(what, timeTaken)));
            logHandler.log(new LogEntry(Level.INFO, prefix + message));
        } else {
            String message = StringUtils.fixCapitalization(PluralizingArgsMessage.format(m_reload_error.toString(what, logHandler.numErrors(), timeTaken)));
            logHandler.log(new LogEntry(Level.SEVERE, prefix + message));
        }
    }

    static {
        Skript.registerStructure(StructAutoReload.class, VALIDATOR, DefaultSyntaxInfos.Structure.NodeType.BOTH, "auto[matically] reload [(this|the) script]");
    }

    public static final class AutoReload
    implements ScriptData {
        private final Set<String> recipients = new HashSet<String>();
        private final String permission;
        private long lastReload;

        private AutoReload(long lastReload, @Nullable String permission, String ... recipients) {
            if (recipients != null) {
                for (String recipient : recipients) {
                    if (recipient == null) continue;
                    this.recipients.add(recipient.toLowerCase(Locale.ENGLISH));
                }
            }
            this.permission = permission;
            this.lastReload = lastReload;
        }

        public @Unmodifiable List<CommandSender> getRecipients() {
            ArrayList senders = Lists.newArrayList((Object[])new CommandSender[]{Bukkit.getConsoleSender()});
            if (!this.recipients.isEmpty()) {
                Bukkit.getOnlinePlayers().stream().filter(p -> this.recipients.contains(p.getName().toLowerCase(Locale.ENGLISH)) || this.recipients.contains(p.getUniqueId().toString())).forEach(senders::add);
                return Collections.unmodifiableList(senders);
            }
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission(this.permission)).forEach(senders::add);
            return Collections.unmodifiableList(senders);
        }

        public long getLastReloadTime() {
            return this.lastReload;
        }

        public void setLastReloadTime(long lastReload) {
            this.lastReload = lastReload;
        }
    }
}

