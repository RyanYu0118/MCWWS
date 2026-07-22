/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.regions.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.hooks.regions.classes.Region;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.iterator.ArrayIterator;
import ch.njol.util.coll.iterator.EmptyIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Blocks in Region")
@Description(value={"All blocks in a <a href='#region'>region</a>.", "This expression requires a supported regions plugin to be installed."})
@Example(value="loop all blocks in the region {arena.%{faction.%player%}%}:\n\tclear the loop-block\n")
@Since(value={"2.1"})
@RequiredPlugins(value={"Supported regions plugin"})
public class ExprBlocksInRegion
extends SimpleExpression<Block> {
    private Expression<Region> regions;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.regions = exprs[0];
        ScriptWarning.printDeprecationWarning("Skript's region syntaxes are deprecated and will be removed in a future release. For WorldGuard support, we recommend using skript-worldguard: https://github.com/SkriptLang/skript-worldguard");
        return true;
    }

    protected Block[] get(Event e) {
        Iterator<Block> iter = this.iterator(e);
        ArrayList<Block> r = new ArrayList<Block>();
        while (iter.hasNext()) {
            r.add(iter.next());
        }
        return r.toArray(new Block[r.size()]);
    }

    @Override
    @NotNull
    public Iterator<Block> iterator(Event e) {
        final Region[] rs = this.regions.getArray(e);
        if (rs.length == 0) {
            return EmptyIterator.get();
        }
        return new Iterator<Block>(){
            private Iterator<Block> current;
            private final Iterator<Region> iter;
            {
                this.current = rs[0].getBlocks();
                this.iter = new ArrayIterator<Region>(rs, 1);
            }

            @Override
            public boolean hasNext() {
                while (!this.current.hasNext() && this.iter.hasNext()) {
                    Region r = this.iter.next();
                    if (r == null) continue;
                    this.current = r.getBlocks();
                }
                return this.current.hasNext();
            }

            @Override
            public Block next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                return this.current.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Block> getReturnType() {
        return Block.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "all blocks in " + this.regions.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprBlocksInRegion.class, Block.class, ExpressionType.COMBINED, "[(all|the)] blocks (in|of) [[the] region[s]] %regions%");
    }
}

