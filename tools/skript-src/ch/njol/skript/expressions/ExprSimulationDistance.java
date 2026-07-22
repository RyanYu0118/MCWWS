/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Simulation Distance")
@Description(value={"The simulation distance of a world or a player.", "Simulation distance is the minimum distance in chunks for entities to tick.", "Simulation distance is capped to the current view distance of the world or player.", "The view distance is capped between 2 and 32 chunks."})
@Example.Examples(value={@Example(value="set simulation distance of player to 10"), @Example(value="add 50 to the simulation distance of world \"world\""), @Example(value="reset the simulation distance of player"), @Example(value="clear the simulation distance of world \"world\"")})
@Since(value={"2.11"})
public class ExprSimulationDistance
extends SimplePropertyExpression<Object, Integer> {
    private static final boolean SUPPORTS_PLAYER = Skript.methodExists(Player.class, "getSimulationDistance", new Class[0]);
    private static final boolean SUPPORTS_SETTER = Skript.methodExists(World.class, "setSimulationDistance", Integer.TYPE);
    private static final boolean RUNNING_1_21 = Skript.isRunningMinecraft(1, 21, 0);

    @Override
    @Nullable
    public Integer convert(Object object) {
        if (object instanceof World) {
            World world = (World)object;
            return world.getSimulationDistance();
        }
        if (SUPPORTS_PLAYER && object instanceof Player) {
            Player player = (Player)object;
            return player.getSimulationDistance();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (SUPPORTS_SETTER) {
            return switch (mode) {
                case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Integer.class);
                default -> null;
            };
        }
        Skript.error("'simulation distance' requires a Paper server to change.");
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int value = 2;
        if (mode == Changer.ChangeMode.RESET) {
            value = Bukkit.getViewDistance();
        } else if (delta != null) {
            value = (Integer)delta[0];
            if (mode == Changer.ChangeMode.REMOVE) {
                value = -value;
            }
        }
        for (Object object : this.getExpr().getArray(event)) {
            if (object instanceof Player) {
                Player player = (Player)object;
                this.changeSimulationDistance(mode, value, () -> ((Player)player).getSimulationDistance(), arg_0 -> ((Player)player).setSimulationDistance(arg_0));
                continue;
            }
            if (!RUNNING_1_21 || !(object instanceof World)) continue;
            World world = (World)object;
            this.changeSimulationDistance(mode, value, () -> ((World)world).getSimulationDistance(), arg_0 -> ((World)world).setSimulationDistance(arg_0));
        }
    }

    private void changeSimulationDistance(Changer.ChangeMode mode, int value, Supplier<Integer> getter, Consumer<Integer> setter) {
        setter.accept(Math2.fit(2, switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> value;
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> getter.get() + value;
            default -> throw new IllegalArgumentException("Unexpected mode: " + String.valueOf((Object)mode));
        }, 32));
    }

    @Override
    public Class<Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return "simulation distance";
    }

    static {
        String property = "worlds";
        if (SUPPORTS_PLAYER) {
            property = "worlds/players";
        }
        ExprSimulationDistance.register(ExprSimulationDistance.class, Integer.class, "simulation distance[s]", property);
    }
}

