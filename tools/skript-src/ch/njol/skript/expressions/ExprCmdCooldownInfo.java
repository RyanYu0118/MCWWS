/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.command.ScriptCommand;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Date;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Cooldown Time/Remaining Time/Elapsed Time/Last Usage/Bypass Permission")
@Description(value={"Only usable in command events. Represents the cooldown time, the remaining time, the elapsed time,", "the last usage date, or the cooldown bypass permission."})
@Example(value="command /home:\n\tcooldown: 10 seconds\n\tcooldown message: You last teleported home %elapsed time% ago, you may teleport home again in %remaining time%.\n\ttrigger:\n\t\tteleport player to {home::%player%}\n")
@Since(value={"2.2-dev33"})
public class ExprCmdCooldownInfo
extends SimpleExpression<Object>
implements EventRestrictedSyntax {
    private int pattern;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern;
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(ScriptCommandEvent.class);
    }

    @Override
    @Nullable
    protected Object[] get(Event e) {
        if (!(e instanceof ScriptCommandEvent)) {
            return null;
        }
        ScriptCommandEvent event = (ScriptCommandEvent)e;
        ScriptCommand scriptCommand = event.getScriptCommand();
        CommandSender sender = event.getSender();
        if (scriptCommand.getCooldown() == null || !(sender instanceof Player)) {
            return null;
        }
        Player player = (Player)event.getSender();
        UUID uuid = player.getUniqueId();
        switch (this.pattern) {
            case 0: 
            case 1: {
                long ms = this.pattern != 1 ? scriptCommand.getRemainingMilliseconds(uuid, event) : scriptCommand.getElapsedMilliseconds(uuid, event);
                return new Timespan[]{new Timespan(ms)};
            }
            case 2: {
                return new Timespan[]{scriptCommand.getCooldown()};
            }
            case 3: {
                return new Date[]{scriptCommand.getLastUsage(uuid, event)};
            }
            case 4: {
                return new String[]{scriptCommand.getCooldownBypass()};
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case ADD: 
            case REMOVE: {
                if (this.pattern <= 1) {
                    return new Class[]{Timespan.class};
                }
            }
            case RESET: 
            case SET: {
                if (this.pattern <= 1) {
                    return new Class[]{Timespan.class};
                }
                if (this.pattern != 3) break;
                return new Class[]{Date.class};
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(e instanceof ScriptCommandEvent)) {
            return;
        }
        ScriptCommandEvent commandEvent = (ScriptCommandEvent)e;
        ScriptCommand command = commandEvent.getScriptCommand();
        Timespan cooldown = command.getCooldown();
        CommandSender sender = commandEvent.getSender();
        if (cooldown == null || !(sender instanceof Player)) {
            return;
        }
        long cooldownMs = cooldown.getAs(Timespan.TimePeriod.MILLISECOND);
        UUID uuid = ((Player)sender).getUniqueId();
        if (this.pattern <= 1) {
            Timespan timespan = delta == null ? new Timespan(0L) : (Timespan)delta[0];
            switch (mode) {
                case ADD: 
                case REMOVE: {
                    long change = (long)(mode == Changer.ChangeMode.ADD ? 1 : -1) * timespan.getAs(Timespan.TimePeriod.MILLISECOND);
                    if (this.pattern == 0) {
                        long remaining = command.getRemainingMilliseconds(uuid, commandEvent);
                        long changed = remaining + change;
                        if (changed < 0L) {
                            changed = 0L;
                        }
                        command.setRemainingMilliseconds(uuid, commandEvent, changed);
                        break;
                    }
                    long elapsed = command.getElapsedMilliseconds(uuid, commandEvent);
                    long changed = elapsed + change;
                    if (changed > cooldownMs) {
                        changed = cooldownMs;
                    }
                    command.setElapsedMilliSeconds(uuid, commandEvent, changed);
                    break;
                }
                case RESET: {
                    if (this.pattern == 0) {
                        command.setRemainingMilliseconds(uuid, commandEvent, cooldownMs);
                        break;
                    }
                    command.setElapsedMilliSeconds(uuid, commandEvent, 0L);
                    break;
                }
                case SET: {
                    if (this.pattern == 0) {
                        command.setRemainingMilliseconds(uuid, commandEvent, timespan.getAs(Timespan.TimePeriod.MILLISECOND));
                        break;
                    }
                    command.setElapsedMilliSeconds(uuid, commandEvent, timespan.getAs(Timespan.TimePeriod.MILLISECOND));
                }
            }
        } else if (this.pattern == 3) {
            switch (mode) {
                case RESET: 
                case REMOVE_ALL: {
                    command.setLastUsage(uuid, commandEvent, null);
                    break;
                }
                case SET: {
                    Date date = delta == null ? null : (Date)delta[0];
                    command.setLastUsage(uuid, commandEvent, date);
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        if (this.pattern <= 2) {
            return Timespan.class;
        }
        return this.pattern == 3 ? Date.class : String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the " + this.getExpressionName() + " of the cooldown";
    }

    @Nullable
    private String getExpressionName() {
        switch (this.pattern) {
            case 0: {
                return "remaining time";
            }
            case 1: {
                return "elapsed time";
            }
            case 2: {
                return "cooldown time";
            }
            case 3: {
                return "last usage date";
            }
            case 4: {
                return "bypass permission";
            }
        }
        return null;
    }

    static {
        Skript.registerExpression(ExprCmdCooldownInfo.class, Object.class, ExpressionType.SIMPLE, "[the] remaining [time] [of [the] (cooldown|wait) [(of|for) [the] [current] command]]", "[the] elapsed [time] [of [the] (cooldown|wait) [(of|for) [the] [current] command]]", "[the] ((cooldown|wait) time|[wait] time of [the] (cooldown|wait) [(of|for) [the] [current] command])", "[the] last usage [date] [of [the] (cooldown|wait) [(of|for) [the] [current] command]]", "[the] [cooldown] bypass perm[ission] [of [the] (cooldown|wait) [(of|for) [the] [current] command]]");
    }
}

