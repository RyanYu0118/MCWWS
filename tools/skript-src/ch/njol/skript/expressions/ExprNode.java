/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Iterator;
import java.util.LinkedHashSet;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Node")
@Description(value={"Returns a node inside a config (or another section-node).", "Nodes in Skript configs are written in the format `key: value`.", "Section nodes can contain other nodes."})
@Example.Examples(value={@Example(value="set {_node} to node \"language\" in the skript config\nif text value of {_node} is \"french\":\n\tbroadcast \"Bonjour!\"\n"), @Example(value="set {_script} to the current script\nloop nodes of the current script:\n\tbroadcast name of loop-value\n")})
@Since(value={"2.10"})
public class ExprNode
extends PropertyExpression<Node, Node>
implements ReflectionExperimentSyntax {
    private boolean isPath;
    private Expression<String> pathExpression;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isPath = pattern < 2;
        switch (pattern) {
            case 0: {
                this.pathExpression = expressions[0];
                this.setExpr(expressions[1]);
                break;
            }
            case 1: {
                this.pathExpression = expressions[1];
                this.setExpr(expressions[0]);
                break;
            }
            default: {
                this.setExpr(expressions[0]);
            }
        }
        return true;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return null;
    }

    protected Node[] get(Event event, Node[] source) {
        if (source.length == 0) {
            return CollectionUtils.array(new Node[0]);
        }
        if (this.isPath) {
            String path = this.pathExpression.getSingle(event);
            Node node = source[0];
            if (node != null && (path == null || path.isBlank())) {
                return CollectionUtils.array(node);
            }
            if (path == null || node == null) {
                return CollectionUtils.array(new Node[0]);
            }
            if ((node = node.getNodeAt(path)) == null) {
                return CollectionUtils.array(new Node[0]);
            }
            return CollectionUtils.array(node);
        }
        LinkedHashSet<Node> nodes = new LinkedHashSet<Node>();
        for (Node node : source) {
            for (Node inner : node) {
                nodes.add(inner);
            }
        }
        return nodes.toArray(new Node[0]);
    }

    @Override
    @Nullable
    public Iterator<? extends Node> iterator(Event event) {
        if (this.isPath) {
            return super.iterator(event);
        }
        Object f = this.getExpr().getSingle(event);
        if (f instanceof SectionNode) {
            SectionNode node = (SectionNode)f;
            return node.iterator();
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return this.isPath;
    }

    @Override
    public Class<? extends Node> getReturnType() {
        return Node.class;
    }

    @Override
    public Class<? extends Node>[] possibleReturnTypes() {
        return new Class[]{Node.class, EntryNode.class};
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.isPath) {
            return "the node " + this.pathExpression.toString(event, debug) + " of " + this.getExpr().toString(event, debug);
        }
        return "the nodes of " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprNode.class, Node.class, ExpressionType.PROPERTY, "[the] node %string% (of|in) %node%", "%node%'[s] node %string%", "[the] nodes (of|in) %nodes%", "%node%'[s] nodes");
    }
}

