/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Difficulty
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Difficulty")
@Description(value={"The difficulty of a world."})
@Example(value="set the difficulty of \"world\" to hard")
@Since(value={"2.3"})
public class ExprDifficulty
extends SimplePropertyExpression<World, Difficulty> {
    @Override
    @Nullable
    public Difficulty convert(World world) {
        return world.getDifficulty();
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Difficulty.class);
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            return;
        }
        Difficulty difficulty = (Difficulty)delta[0];
        for (World world : (World[])this.getExpr().getArray(e)) {
            world.setDifficulty(difficulty);
            if (difficulty == Difficulty.PEACEFUL) continue;
            world.setSpawnFlags(true, world.getAllowAnimals());
        }
    }

    @Override
    protected String getPropertyName() {
        return "difficulty";
    }

    @Override
    public Class<Difficulty> getReturnType() {
        return Difficulty.class;
    }

    static {
        ExprDifficulty.register(ExprDifficulty.class, Difficulty.class, "difficult(y|ies)", "worlds");
    }
}

