/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.sections;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.ContextlessEvent;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.SkriptPattern;
import ch.njol.skript.util.Patterns;
import ch.njol.util.Kleenean;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.lang.condition.Conditional;

@Name(value="Conditionals")
@Description(value={"Conditional sections", "if: executed when its condition is true", "else if: executed if all previous chained conditionals weren't executed, and its condition is true", "else: executed if all previous chained conditionals weren't executed", "", "parse if: a special case of 'if' condition that its code will not be parsed if the condition is not true", "else parse if: another special case of 'else if' condition that its code will not be parsed if all previous chained conditionals weren't executed, and its condition is true"})
@Example.Examples(value={@Example(value="if player's health is greater than or equal to 4:\n\tsend \"Your health is okay so far but be careful!\"\nelse if player's health is greater than 2:\n\tsend \"You need to heal ASAP, your health is very low!\"\nelse: # Less than 2 hearts\n\tsend \"You are about to DIE if you don't heal NOW. You have only %player's health% heart(s)!\"\n"), @Example(value="parse if plugin \"SomePluginName\" is enabled: # parse if %condition%\n\t# This code will only be executed if the condition used is met otherwise Skript will not parse this section therefore will not give any errors/info about this section\n")})
@Since(value={"1.0"})
public class SecConditional
extends Section {
    private static final SkriptPattern THEN_PATTERN = PatternCompiler.compile("then [run]");
    private static final Patterns<ConditionalType> CONDITIONAL_PATTERNS = new Patterns(new Object[][]{{"else", ConditionalType.ELSE}, {"else [:parse] if <.+>", ConditionalType.ELSE_IF}, {"else [:parse] if (:any|any:at least one [of])", ConditionalType.ELSE_IF}, {"else [:parse] if [all]", ConditionalType.ELSE_IF}, {"[:parse] if (:any|any:at least one [of])", ConditionalType.IF}, {"[:parse] if [all]", ConditionalType.IF}, {"[:parse] if <.+>", ConditionalType.IF}, {THEN_PATTERN.toString(), ConditionalType.THEN}, {"implicit:<.+>", ConditionalType.IF}});
    private ConditionalType type;
    private @UnknownNullability Conditional<Event> conditional;
    private boolean ifAny;
    private boolean parseIf;
    private boolean parseIfPassed;
    private boolean multiline;
    @Nullable
    private Kleenean hasDelayBefore;
    @Nullable
    private Kleenean shouldDelayAfter;
    @Nullable
    private ExecutionIntent executionIntent;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        this.type = CONDITIONAL_PATTERNS.getInfo(matchedPattern);
        this.ifAny = parseResult.hasTag("any");
        this.parseIf = parseResult.hasTag("parse");
        this.multiline = parseResult.regexes.isEmpty() && this.type != ConditionalType.ELSE;
        ParserInstance parser = this.getParser();
        if (this.type != ConditionalType.IF) {
            if (this.type == ConditionalType.THEN) {
                SecConditional precedingConditional = SecConditional.getPrecedingConditional(triggerItems, null);
                if (precedingConditional == null || !precedingConditional.multiline || precedingConditional.type == ConditionalType.THEN) {
                    Skript.error("'then' has to placed just after a multiline 'if' or 'else if' section");
                    return false;
                }
            } else {
                SecConditional precedingIf = SecConditional.getPrecedingConditional(triggerItems, ConditionalType.IF);
                if (precedingIf == null) {
                    if (this.type == ConditionalType.ELSE_IF) {
                        Skript.error("'else if' has to be placed just after another 'if' or 'else if' section");
                    } else if (this.type == ConditionalType.ELSE) {
                        Skript.error("'else' has to be placed just after another 'if' or 'else if' section");
                    }
                    return false;
                }
            }
        } else {
            if (this.multiline) {
                Node nextNode = this.getNextNode(sectionNode, parser);
                String error = (this.ifAny ? "'if any'" : "'if all'") + " has to be placed just before a 'then' section";
                if (nextNode instanceof SectionNode && nextNode.getKey() != null) {
                    String nextNodeKey = ScriptLoader.replaceOptions(nextNode.getKey());
                    if (THEN_PATTERN.match(nextNodeKey) == null) {
                        Skript.error(error);
                        return false;
                    }
                } else {
                    Skript.error(error);
                    return false;
                }
            }
            this.hasDelayBefore = parser.getHasDelayBefore();
        }
        if (!parser.getHasDelayBefore().isTrue()) {
            Kleenean wasDelayedBeforeChain;
            Kleenean kleenean = wasDelayedBeforeChain = this.hasDelayBefore != null ? this.hasDelayBefore : SecConditional.getPrecedingConditional(triggerItems, (ConditionalType)ConditionalType.IF).hasDelayBefore;
            assert (wasDelayedBeforeChain != null);
            parser.setHasDelayBefore(wasDelayedBeforeChain);
        }
        if (this.type == ConditionalType.IF || this.type == ConditionalType.ELSE_IF) {
            Class<? extends Event>[] currentEvents = parser.getCurrentEvents();
            String currentEventName = parser.getCurrentEventName();
            ArrayList conditionals = new ArrayList();
            if (this.parseIf) {
                parser.setCurrentEvents(new Class[]{ContextlessEvent.class});
                parser.setCurrentEventName("parse");
            }
            if (this.multiline) {
                int nonEmptyNodeCount = Iterables.size((Iterable)sectionNode);
                if (nonEmptyNodeCount < 2) {
                    Skript.error((this.ifAny ? "'if any'" : "'if all'") + " sections must contain at least two conditions");
                    return false;
                }
                for (Node node : sectionNode) {
                    if (node instanceof SectionNode) {
                        Skript.error((this.ifAny ? "'if any'" : "'if all'") + " sections may not contain other sections");
                        return false;
                    }
                    String childKey = node.getKey();
                    if (childKey == null) continue;
                    childKey = ScriptLoader.replaceOptions(childKey);
                    parser.setNode(node);
                    Condition condition = Condition.parse(childKey, "Can't understand this condition: '" + childKey + "'");
                    if (condition == null) {
                        return false;
                    }
                    conditionals.add(condition);
                }
                parser.setNode(sectionNode);
            } else {
                String expr = parseResult.regexes.get(0).group();
                Condition condition = Condition.parse(expr, parseResult.hasTag("implicit") ? null : "Can't understand this condition: '" + expr + "'");
                if (condition != null) {
                    conditionals.add(condition);
                }
            }
            if (this.parseIf) {
                parser.setCurrentEvents(currentEvents);
                parser.setCurrentEventName(currentEventName);
            }
            if (conditionals.isEmpty()) {
                return false;
            }
            if ((Skript.debug() || sectionNode.debug()) && conditionals.size() > 1) {
                String indentation = parser.getIndentation() + "    ";
                for (Conditional conditional : conditionals) {
                    Skript.debug(indentation + TextComponentParser.instance().escape(conditional.toString(null, true)));
                }
            }
            this.conditional = Conditional.compound(this.ifAny ? Conditional.Operator.OR : Conditional.Operator.AND, conditionals);
        }
        if (this.parseIf) {
            if (!this.checkConditions(ContextlessEvent.get())) {
                return true;
            }
            this.parseIfPassed = true;
        }
        if (!this.multiline || this.type == ConditionalType.THEN) {
            boolean considerDelayUpdate = !parser.getHasDelayBefore().isTrue();
            this.loadCode(sectionNode);
            if (considerDelayUpdate) {
                Kleenean hasDelayAfter = parser.getHasDelayBefore();
                Kleenean preceding = this.getPrecedingShouldDelayAfter(triggerItems);
                this.shouldDelayAfter = preceding == null || preceding == hasDelayAfter ? hasDelayAfter : Kleenean.UNKNOWN;
                if (this.shouldDelayAfter.isTrue()) {
                    parser.setHasDelayBefore(this.type == ConditionalType.ELSE ? Kleenean.TRUE : Kleenean.UNKNOWN);
                } else {
                    parser.setHasDelayBefore(this.shouldDelayAfter);
                }
            }
        }
        if (this.type == ConditionalType.ELSE) {
            List<SecConditional> conditionals = SecConditional.getPrecedingConditionals(triggerItems);
            conditionals.add(0, this);
            for (SecConditional conditional : conditionals) {
                if (conditional.multiline && conditional.type != ConditionalType.THEN) continue;
                ExecutionIntent triggerIntent = conditional.triggerExecutionIntent();
                if (triggerIntent == null) {
                    this.executionIntent = null;
                    break;
                }
                if (this.executionIntent != null && triggerIntent.compareTo(this.executionIntent) >= 0) continue;
                this.executionIntent = triggerIntent;
            }
        }
        return true;
    }

    @Override
    @Nullable
    public TriggerItem getNext() {
        return this.getSkippedNext();
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        if (this.type == ConditionalType.THEN || this.parseIf && !this.parseIfPassed) {
            return this.getActualNext();
        }
        if (this.parseIf || this.checkConditions(event)) {
            SecConditional sectionToRun = this.multiline ? (SecConditional)this.getActualNext() : this;
            TriggerItem skippedNext = this.getSkippedNext();
            if (sectionToRun.last != null) {
                sectionToRun.last.setNext(skippedNext);
            }
            return sectionToRun.first != null ? sectionToRun.first : skippedNext;
        }
        return this.getActualNext();
    }

    @Override
    @Nullable
    public ExecutionIntent executionIntent() {
        return this.executionIntent;
    }

    @Override
    public ExecutionIntent triggerExecutionIntent() {
        if (this.multiline && this.type != ConditionalType.THEN) {
            return null;
        }
        return super.triggerExecutionIntent();
    }

    @Nullable
    private TriggerItem getSkippedNext() {
        TriggerItem next = this.getActualNext();
        while (next instanceof SecConditional) {
            SecConditional nextSecCond = (SecConditional)next;
            if (nextSecCond.type == ConditionalType.IF) break;
            next = next.getActualNext();
        }
        return next;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String parseIf = this.parseIf ? "parse " : "";
        return switch (this.type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 2 -> {
                if (this.multiline) {
                    yield parseIf + "if " + (this.ifAny ? "any" : "all");
                }
                yield parseIf + "if " + this.conditional.toString(event, debug);
            }
            case 1 -> {
                if (this.multiline) {
                    yield "else " + parseIf + "if " + (this.ifAny ? "any" : "all");
                }
                yield "else " + parseIf + "if " + this.conditional.toString(event, debug);
            }
            case 0 -> "else";
            case 3 -> "then";
        };
    }

    @Nullable
    private static SecConditional getPrecedingConditional(List<TriggerItem> triggerItems, @Nullable ConditionalType type) {
        for (int i = triggerItems.size() - 1; i >= 0; --i) {
            TriggerItem triggerItem = triggerItems.get(i);
            if (triggerItem instanceof SecConditional) {
                SecConditional precedingSecConditional = (SecConditional)triggerItem;
                if (precedingSecConditional.type == ConditionalType.ELSE) {
                    return null;
                }
                if (type != null && precedingSecConditional.type != type) continue;
                return precedingSecConditional;
            }
            return null;
        }
        return null;
    }

    private static List<SecConditional> getPrecedingConditionals(List<TriggerItem> triggerItems) {
        TriggerItem triggerItem;
        ArrayList<SecConditional> conditionals = new ArrayList<SecConditional>();
        for (int i = triggerItems.size() - 1; i >= 0 && (triggerItem = triggerItems.get(i)) instanceof SecConditional; --i) {
            SecConditional conditional = (SecConditional)triggerItem;
            conditionals.add(conditional);
            if (conditional.type == ConditionalType.IF) break;
        }
        return conditionals;
    }

    @Nullable
    private Kleenean getPrecedingShouldDelayAfter(List<TriggerItem> triggerItems) {
        TriggerItem triggerItem;
        if (this.type == ConditionalType.IF) {
            return null;
        }
        for (int i = triggerItems.size() - 1; i >= 0 && (triggerItem = triggerItems.get(i)) instanceof SecConditional; --i) {
            SecConditional precedingSecConditional = (SecConditional)triggerItem;
            if (precedingSecConditional.shouldDelayAfter != null) {
                return precedingSecConditional.shouldDelayAfter;
            }
            if (precedingSecConditional.type == ConditionalType.IF) break;
        }
        return null;
    }

    private boolean checkConditions(Event event) {
        return this.conditional == null || this.conditional.evaluate(event).isTrue();
    }

    @Nullable
    private Node getNextNode(Node precedingNode, ParserInstance parser) {
        Node originalCurrentNode = parser.getNode();
        SectionNode parentNode = precedingNode.getParent();
        if (parentNode == null) {
            return null;
        }
        Iterator<Node> parentIterator = parentNode.iterator();
        while (parentIterator.hasNext()) {
            Node current = parentIterator.next();
            if (current != precedingNode) continue;
            Node nextNode = parentIterator.hasNext() ? parentIterator.next() : null;
            parser.setNode(originalCurrentNode);
            return nextNode;
        }
        parser.setNode(originalCurrentNode);
        return null;
    }

    static {
        Skript.registerSection(SecConditional.class, CONDITIONAL_PATTERNS.getPatterns());
    }

    private static enum ConditionalType {
        ELSE,
        ELSE_IF,
        IF,
        THEN;

    }
}

