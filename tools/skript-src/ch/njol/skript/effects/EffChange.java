/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.ExprParse;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionList;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.KeyReceiverExpression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.CountingLogHandler;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.variables.HintManager;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Change: Set/Add/Remove/Remove All/Delete/Reset")
@Description(value={"A general effect that can be used for changing many <a href='./expressions'>expressions</a>.", "Some expressions can only be set and/or deleted, while others can also have things added to or removed from them."})
@Example.Examples(value={@Example(value="set the player's display name to \"<red>%name of player%\"\nset the block above the victim to lava\n"), @Example(value="add 2 to the player's health # preferably use '<a href='#EffHealth'>heal</a>' for this\nadd argument to {blacklist::*}\ngive a diamond pickaxe of efficiency 5 to the player\nincrease the data value of the clicked block by 1\n"), @Example(value="remove 2 pickaxes from the victim\nsubtract 2.5 from {points::%uuid of player%}\n"), @Example(value="remove every iron tool from the player\nremove all minecarts from {entitylist::*}\n"), @Example(value="delete the block below the player\nclear drops\ndelete {variable}\n"), @Example(value="reset walk speed of player\nreset chunk at the targeted block\n")})
@Since(value={"1.0 (set, add, remove, delete), 2.0 (remove all)"})
public class EffChange
extends Effect {
    private static final Patterns<Changer.ChangeMode> PATTERNS = new Patterns(new Object[][]{{"(add|give) %objects% to %~objects%", Changer.ChangeMode.ADD}, {"increase %~objects% by %objects%", Changer.ChangeMode.ADD}, {"give %~objects% %objects%", Changer.ChangeMode.ADD}, {"set %~objects% to %objects%", Changer.ChangeMode.SET}, {"remove (all|every) %objects% from %~objects%", Changer.ChangeMode.REMOVE_ALL}, {"(remove|subtract) %objects% from %~objects%", Changer.ChangeMode.REMOVE}, {"(reduce|decrease) %~objects% by %objects%", Changer.ChangeMode.REMOVE}, {"(delete|clear) %~objects%", Changer.ChangeMode.DELETE}, {"reset %~objects%", Changer.ChangeMode.RESET}});
    private Expression<?> changed;
    @Nullable
    private Expression<?> changer;
    private Changer.ChangeMode mode;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Expression<?> type;
        String what;
        Object[] acceptedTypes;
        CountingLogHandler changeLog;
        this.mode = PATTERNS.getInfo(matchedPattern);
        switch (this.mode) {
            case ADD: 
            case REMOVE: {
                if (matchedPattern == 0 || matchedPattern == 5) {
                    this.changer = exprs[0];
                    this.changed = exprs[1];
                    break;
                }
                this.changer = exprs[1];
                this.changed = exprs[0];
                break;
            }
            case SET: {
                this.changer = exprs[1];
                this.changed = exprs[0];
                break;
            }
            case REMOVE_ALL: {
                this.changer = exprs[0];
                this.changed = exprs[1];
                break;
            }
            case DELETE: 
            case RESET: {
                this.changed = exprs[0];
            }
        }
        try (CountingLogHandler countingLogHandler = changeLog = new CountingLogHandler(Level.SEVERE);){
            acceptedTypes = this.changed.acceptChange(this.mode);
            ClassInfo<?> changedInfo = Classes.getSuperClassInfo(this.changed.getReturnType());
            Changer<?> changer = changedInfo.getChanger();
            what = changer != null && Arrays.equals(changer.acceptChange(this.mode), acceptedTypes) ? changedInfo.getName().withIndefiniteArticle() : this.changed.toString(null, Skript.debug());
        }
        if (acceptedTypes == null) {
            if (changeLog.getCount() > 0) {
                return false;
            }
            Skript.error(switch (this.mode) {
                default -> throw new MatchException(null, null);
                case Changer.ChangeMode.ADD -> what + " can't have anything added to it";
                case Changer.ChangeMode.SET -> what + " can't be set to anything";
                case Changer.ChangeMode.REMOVE, Changer.ChangeMode.REMOVE_ALL -> {
                    if (this.mode == Changer.ChangeMode.REMOVE_ALL && this.changed.acceptChange(Changer.ChangeMode.REMOVE) != null) {
                        yield what + " can't have 'all of something' removed from it. However, it does support regular removal which could be used to achieve the desired effect";
                    }
                    yield what + " can't have anything removed from it";
                }
                case Changer.ChangeMode.RESET -> {
                    String error = what + " can't be reset";
                    if (this.changed.acceptChange(Changer.ChangeMode.DELETE) != null) {
                        error = error + ". However, it can be deleted which might result in the desired effect";
                    }
                    yield error;
                }
                case Changer.ChangeMode.DELETE -> {
                    String error = what + " can't be deleted";
                    if (this.changed.acceptChange(Changer.ChangeMode.RESET) != null) {
                        error = error + ". However, it can be reset which might result in the desired effect";
                    }
                    yield error;
                }
            });
            return false;
        }
        Class[] flatAcceptedTypes = new Class[acceptedTypes.length];
        for (int i = 0; i < flatAcceptedTypes.length; ++i) {
            type = acceptedTypes[i];
            if (((Class)((Object)type)).isArray()) {
                type = ((Class)((Object)type)).getComponentType();
            }
            flatAcceptedTypes[i] = type;
        }
        type = this.changed;
        if (type instanceof Variable) {
            Variable variable = (Variable)type;
            if (this.mode == Changer.ChangeMode.DELETE && HintManager.canUseHints(variable)) {
                this.getParser().getHintManager().delete(variable);
            }
        }
        if (this.changer == null) {
            return true;
        }
        Expression<Object> validatedChanger = null;
        try (ParseLogHandler log = new ParseLogHandler().start();){
            if (LiteralUtils.canInitSafely(this.changer)) {
                Class<?> changerReturnType = this.changer.getReturnType();
                for (Class type2 : flatAcceptedTypes) {
                    if (!type2.isAssignableFrom(changerReturnType)) continue;
                    validatedChanger = this.changer;
                    break;
                }
            }
            if (validatedChanger == null) {
                validatedChanger = this.changer.getConvertedExpression(flatAcceptedTypes);
            }
            if (validatedChanger == null) {
                if (!log.hasError()) {
                    String changerString = this.changer.toString(null, Skript.debug());
                    if (flatAcceptedTypes.length == 1 && flatAcceptedTypes[0] == Object.class) {
                        Skript.error("Can't understand this expression: " + changerString, ErrorQuality.NOT_AN_EXPRESSION);
                    } else {
                        String not = "is " + SkriptParser.notOfType(flatAcceptedTypes);
                        Skript.error(switch (this.mode) {
                            case Changer.ChangeMode.ADD -> changerString + " can't be added to " + what + " because the former " + not;
                            case Changer.ChangeMode.SET -> what + " can't be set to " + changerString + " because the latter " + not;
                            case Changer.ChangeMode.REMOVE, Changer.ChangeMode.REMOVE_ALL -> changerString + " can't be removed from " + what + " because the former " + not;
                            default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)this.mode));
                        });
                    }
                }
                log.printError();
                boolean changerString = false;
                return changerString;
            }
            log.printLog();
        }
        this.changer = validatedChanger;
        ArrayList<Class> singleTypes = new ArrayList<Class>();
        boolean expectingSingle = true;
        for (int i = 0; i < acceptedTypes.length; ++i) {
            if (!validatedChanger.canReturn(flatAcceptedTypes[i])) continue;
            if (((Class)acceptedTypes[i]).isArray()) {
                expectingSingle = false;
                break;
            }
            singleTypes.add(flatAcceptedTypes[i]);
        }
        if (expectingSingle && !this.changer.canBeSingle()) {
            String changedString = this.changed.toString(null, Skript.debug());
            String types = Classes.toString(singleTypes.toArray(), false);
            Skript.error(switch (this.mode) {
                case Changer.ChangeMode.ADD -> "Only one " + types + " can be added to " + changedString + ", not more";
                case Changer.ChangeMode.SET -> changedString + " can only be set to one " + types + ", not more";
                case Changer.ChangeMode.REMOVE, Changer.ChangeMode.REMOVE_ALL -> "Only one " + types + " can be removed from " + changedString + ", not more";
                default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)this.mode));
            });
            return false;
        }
        Expression<?> types = this.changed;
        if (types instanceof Variable) {
            Variable variable = (Variable)types;
            if (!this.changed.isSingle() && this.mode == Changer.ChangeMode.SET) {
                Expression<?> expression = this.changer;
                if (expression instanceof ExprParse) {
                    ExprParse exprParse = (ExprParse)expression;
                    exprParse.flatten = false;
                } else {
                    expression = this.changer;
                    if (expression instanceof ExpressionList) {
                        ExpressionList exprList = (ExpressionList)expression;
                        for (Expression expression2 : exprList.getAllExpressions()) {
                            if (!(expression2 instanceof ExprParse)) continue;
                            ExprParse exprParse = (ExprParse)expression2;
                            exprParse.flatten = false;
                        }
                    }
                }
            }
            if (this.mode == Changer.ChangeMode.SET || variable.isList() && this.mode == Changer.ChangeMode.ADD) {
                ClassInfo<?> changerInfo;
                if (HintManager.canUseHints(variable)) {
                    HintManager hintManager = this.getParser().getHintManager();
                    Class<?>[] hints = this.changer.possibleReturnTypes();
                    if (this.mode == Changer.ChangeMode.SET) {
                        hintManager.set(variable, hints);
                    } else {
                        hintManager.add(variable, hints);
                    }
                }
                if (!(variable.isLocal() || variable.isEphemeral() || (changerInfo = Classes.getSuperClassInfo(this.changer.getReturnType())).getC() == Object.class || changerInfo.getSerializer() != null || changerInfo.getSerializeAs() != null || SkriptConfig.disableObjectCannotBeSavedWarnings.value().booleanValue() || !this.getParser().isActive() || this.getParser().getCurrentScript().suppressesWarning(ScriptWarning.VARIABLE_SAVE))) {
                    Skript.warning(changerInfo.getName().withIndefiniteArticle() + " cannot be saved. That is, the contents of the variable " + this.changed.toString(null, Skript.debug()) + " will be lost when the server stops.");
                }
            }
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Object[] delta = null;
        if (this.changer != null) {
            Expression<?> expression;
            delta = this.changer.getArray(event);
            if ((delta = this.changer.beforeChange(this.changed, delta)) == null || delta.length == 0) {
                if (this.mode == Changer.ChangeMode.SET && this.changed.acceptChange(Changer.ChangeMode.DELETE) != null) {
                    this.changed.change(event, null, Changer.ChangeMode.DELETE);
                }
                return;
            }
            if (this.mode.supportsKeyedChange() && KeyProviderExpression.areKeysRecommended(this.changer) && (expression = this.changed) instanceof KeyReceiverExpression) {
                KeyReceiverExpression receiver = (KeyReceiverExpression)expression;
                receiver.change(event, delta, this.mode, ((KeyProviderExpression)this.changer).getArrayKeys(event));
                return;
            }
        }
        this.changed.change(event, delta, this.mode);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        switch (this.mode) {
            case ADD: {
                assert (this.changer != null);
                builder.append("add", this.changer, "to", this.changed);
                break;
            }
            case SET: {
                assert (this.changer != null);
                builder.append("set", this.changed, "to", this.changer);
                break;
            }
            case REMOVE: {
                assert (this.changer != null);
                builder.append("remove", this.changer, "from", this.changed);
                break;
            }
            case REMOVE_ALL: {
                assert (this.changer != null);
                builder.append("remove all", this.changer, "from", this.changed);
                break;
            }
            case DELETE: {
                builder.append("delete", this.changed);
                break;
            }
            case RESET: {
                builder.append("reset", this.changed);
            }
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffChange.class, PATTERNS.getPatterns());
    }
}

