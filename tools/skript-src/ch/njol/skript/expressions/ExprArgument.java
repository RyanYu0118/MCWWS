/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.server.ServerCommandEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.command.Argument;
import ch.njol.skript.command.Commands;
import ch.njol.skript.command.ScriptCommandEvent;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import java.util.List;
import java.util.regex.MatchResult;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Argument")
@Description(value={"Usable in script commands and command events. Holds the value of an argument given to the command, e.g. if the command \"/tell &lt;player&gt; &lt;text&gt;\" is used like \"/tell Njol Hello Njol!\" argument 1 is the player named \"Njol\" and argument 2 is \"Hello Njol!\".", "One can also use the type of the argument instead of its index to address the argument, e.g. in the above example 'player-argument' is the same as 'argument 1'.", "Please note that specifying the argument type is only supported in script commands."})
@Example.Examples(value={@Example(value="give the item-argument to the player-argument"), @Example(value="damage the player-argument by the number-argument"), @Example(value="give a diamond pickaxe to the argument"), @Example(value="add argument 1 to argument 2"), @Example(value="heal the last argument")})
@Since(value={"1.0, 2.7 (support for command events)"})
public class ExprArgument
extends SimpleExpression<Object>
implements EventRestrictedSyntax {
    private static final int LAST = 0;
    private static final int ORDINAL = 1;
    private static final int SINGLE = 2;
    private static final int ALL = 3;
    private static final int CLASSINFO = 4;
    private int what;
    @Nullable
    private Argument<?> argument;
    private int ordinal = -1;
    private boolean couldCauseArithmeticConfusion = false;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean scriptCommand = this.getParser().isCurrentEvent((Class<? extends Event>)ScriptCommandEvent.class);
        switch (matchedPattern) {
            case 0: {
                this.what = 0;
                break;
            }
            case 1: 
            case 2: {
                this.what = 1;
                break;
            }
            case 3: {
                int n = this.what = parseResult.mark == 1 ? 3 : 2;
                if (this.what != 2 || !parseResult.expr.matches("(the )?arg(ument)?")) break;
                this.couldCauseArithmeticConfusion = true;
                break;
            }
            case 4: 
            case 5: {
                this.what = 4;
                break;
            }
            default: {
                assert (false);
                break;
            }
        }
        if (!scriptCommand && this.what == 4) {
            Skript.error("Command event arguments are strings, meaning type specification is useless");
            return false;
        }
        List<Argument<?>> currentArguments = Commands.currentArguments;
        if (scriptCommand && (currentArguments == null || currentArguments.isEmpty())) {
            Skript.error("This command doesn't have any arguments", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        if (this.what == 1) {
            MatchResult regex = parseResult.regexes.get(0);
            String argMatch = null;
            for (int i = 1; i <= 4 && (argMatch = regex.group(i)) == null; ++i) {
            }
            assert (argMatch != null);
            this.ordinal = Utils.parseInt(argMatch);
            if (scriptCommand && this.ordinal > currentArguments.size()) {
                Skript.error("This command doesn't have a " + StringUtils.fancyOrderNumber(this.ordinal) + " argument", ErrorQuality.SEMANTIC_ERROR);
                return false;
            }
        }
        if (scriptCommand) {
            switch (this.what) {
                case 0: {
                    this.argument = currentArguments.get(currentArguments.size() - 1);
                    break;
                }
                case 1: {
                    this.argument = currentArguments.get(this.ordinal - 1);
                    break;
                }
                case 2: {
                    if (currentArguments.size() == 1) {
                        this.argument = currentArguments.get(0);
                        break;
                    }
                    Skript.error("This command has multiple arguments, meaning it is not possible to get the 'argument'. Use 'argument 1', 'argument 2', etc. instead", ErrorQuality.SEMANTIC_ERROR);
                    return false;
                }
                case 3: {
                    Skript.error("'arguments' cannot be used for script commands. Use 'argument 1', 'argument 2', etc. instead", ErrorQuality.SEMANTIC_ERROR);
                    return false;
                }
                case 4: {
                    ClassInfo c = (ClassInfo)((Literal)exprs[0]).getSingle();
                    if (parseResult.regexes.size() > 0) {
                        this.ordinal = Utils.parseInt(parseResult.regexes.get(0).group());
                        if (this.ordinal > currentArguments.size()) {
                            Skript.error("This command doesn't have a " + StringUtils.fancyOrderNumber(this.ordinal) + " " + String.valueOf(c) + " argument", ErrorQuality.SEMANTIC_ERROR);
                            return false;
                        }
                    }
                    Argument<?> arg = null;
                    int argAmount = 0;
                    for (Argument<?> a : currentArguments) {
                        if (!c.getC().isAssignableFrom(a.getType())) continue;
                        if (this.ordinal == -1 && argAmount == 2) {
                            Skript.error("There are multiple " + String.valueOf(c) + " arguments in this command", ErrorQuality.SEMANTIC_ERROR);
                            return false;
                        }
                        arg = a;
                        if (++argAmount != this.ordinal) continue;
                        break;
                    }
                    if (argAmount == 0) {
                        Skript.error("There is no " + String.valueOf(c) + " argument in this command", ErrorQuality.SEMANTIC_ERROR);
                        return false;
                    }
                    if (this.ordinal > argAmount) {
                        if (argAmount == 1) {
                            Skript.error("There is only one " + String.valueOf(c) + " argument in this command", ErrorQuality.SEMANTIC_ERROR);
                        } else {
                            Skript.error("There are only " + argAmount + " " + String.valueOf(c) + " arguments in this command", ErrorQuality.SEMANTIC_ERROR);
                        }
                        return false;
                    }
                    this.argument = arg;
                    break;
                }
                default: {
                    assert (false) : this.what;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(ScriptCommandEvent.class, PlayerCommandPreprocessEvent.class, ServerCommandEvent.class);
    }

    @Override
    @Nullable
    protected Object[] get(Event e) {
        String fullCommand;
        if (this.argument != null) {
            return this.argument.getCurrent(e);
        }
        if (e instanceof PlayerCommandPreprocessEvent) {
            fullCommand = ((PlayerCommandPreprocessEvent)e).getMessage().substring(1).trim();
        } else if (e instanceof ServerCommandEvent) {
            fullCommand = ((ServerCommandEvent)e).getCommand().trim();
        } else {
            return new Object[0];
        }
        int firstSpace = fullCommand.indexOf(32);
        if (firstSpace == -1) {
            return new String[0];
        }
        fullCommand = fullCommand.substring(firstSpace + 1);
        Object[] arguments = fullCommand.split(" ");
        switch (this.what) {
            case 0: {
                if (arguments.length <= 0) break;
                return new String[]{arguments[arguments.length - 1]};
            }
            case 1: {
                if (arguments.length < this.ordinal) break;
                return new String[]{arguments[this.ordinal - 1]};
            }
            case 2: {
                if (arguments.length != 1) break;
                return new String[]{arguments[arguments.length - 1]};
            }
            case 3: {
                return arguments;
            }
        }
        return new Object[0];
    }

    @Override
    public boolean isSingle() {
        return this.argument != null ? this.argument.isSingle() : this.what != 3;
    }

    public boolean couldCauseArithmeticConfusion() {
        return this.couldCauseArithmeticConfusion;
    }

    @Override
    public Class<?> getReturnType() {
        return this.argument != null ? this.argument.getType() : String.class;
    }

    @Override
    public boolean isLoopOf(String s) {
        return s.equalsIgnoreCase("argument");
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        switch (this.what) {
            case 0: {
                return "the last argument";
            }
            case 1: {
                return "the " + StringUtils.fancyOrderNumber(this.ordinal) + " argument";
            }
            case 2: {
                return "the argument";
            }
            case 3: {
                return "the arguments";
            }
            case 4: {
                assert (this.argument != null);
                ClassInfo<?> ci = Classes.getExactClassInfo(this.argument.getType());
                assert (ci != null);
                return "the " + String.valueOf(ci) + " argument " + String.valueOf(this.ordinal != -1 ? Integer.valueOf(this.ordinal) : "");
            }
        }
        return "argument";
    }

    static {
        Skript.registerExpression(ExprArgument.class, Object.class, ExpressionType.SIMPLE, "[the] last arg[ument]", "[the] arg[ument](-| )<(\\d+)>", "[the] <(\\d*1)st|(\\d*2)nd|(\\d*3)rd|(\\d*[4-90])th> arg[ument][s]", "[(all [[of] the]|the)] arg[ument][(1:s)]", "[the] %*classinfo%( |-)arg[ument][( |-)<\\d+>]", "[the] arg[ument]( |-)%*classinfo%[( |-)<\\d+>]");
    }
}

