/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.entity.EntityBreedEvent
 *  org.bukkit.event.player.PlayerExpChangeEvent
 *  org.bukkit.event.player.PlayerFishEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.events.bukkit.ExperienceSpawnEvent;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Experience;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.Nullable;

@Name(value="Experience")
@Description(value={"How much experience was spawned in an experience spawn or block break event. Can be changed."})
@Example.Examples(value={@Example(value="on experience spawn:\n\tadd 5 to the spawned experience\n"), @Example(value="on break of coal ore:\n\tclear dropped experience\n"), @Example(value="on break of diamond ore:\n\tif tool of player = diamond pickaxe:\n\t\tadd 100 to dropped experience\n"), @Example(value="on breed:\n\tbreeding father is a cow\n\tset dropped experience to 10\n"), @Example(value="on fish catch:\n\tadd 70 to dropped experience\n")})
@Since(value={"2.1, 2.5.3 (block break event), 2.7 (experience change event), 2.10 (breeding, fishing)"})
@Events(value={"experience spawn", "break / mine", "experience change", "entity breed"})
public class ExprExperience
extends SimpleExpression<Experience>
implements EventRestrictedSyntax {
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(ExperienceSpawnEvent.class, BlockBreakEvent.class, PlayerExpChangeEvent.class, EntityBreedEvent.class, PlayerFishEvent.class);
    }

    protected Experience @Nullable [] get(Event event) {
        Experience[] exp;
        if (event instanceof ExperienceSpawnEvent) {
            ExperienceSpawnEvent experienceSpawnEvent = (ExperienceSpawnEvent)event;
            exp = new Experience[]{new Experience(experienceSpawnEvent.getSpawnedXP())};
        } else if (event instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent)event;
            exp = new Experience[]{new Experience(blockBreakEvent.getExpToDrop())};
        } else if (event instanceof PlayerExpChangeEvent) {
            PlayerExpChangeEvent playerExpChangeEvent = (PlayerExpChangeEvent)event;
            exp = new Experience[]{new Experience(playerExpChangeEvent.getAmount())};
        } else if (event instanceof EntityBreedEvent) {
            EntityBreedEvent entityBreedEvent = (EntityBreedEvent)event;
            exp = new Experience[]{new Experience(entityBreedEvent.getExperience())};
        } else if (event instanceof PlayerFishEvent) {
            PlayerFishEvent fishEvent = (PlayerFishEvent)event;
            exp = new Experience[]{new Experience(fishEvent.getExpToDrop())};
        } else {
            exp = new Experience[]{};
        }
        return exp;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Experience.class, Integer.class);
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Experience[].class, Integer[].class);
            case Changer.ChangeMode.RESET -> CollectionUtils.array(new Class[0]);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        EntityBreedEvent entityBreedEvent;
        int exp;
        Object experienceSpawnEvent;
        if (event instanceof ExperienceSpawnEvent) {
            experienceSpawnEvent = (Object[])event;
            exp = ((ExperienceSpawnEvent)((Object)experienceSpawnEvent)).getSpawnedXP();
        } else if (event instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent)event;
            exp = blockBreakEvent.getExpToDrop();
        } else if (event instanceof PlayerExpChangeEvent) {
            PlayerExpChangeEvent playerExpChangeEvent = (PlayerExpChangeEvent)event;
            exp = playerExpChangeEvent.getAmount();
        } else if (event instanceof EntityBreedEvent) {
            entityBreedEvent = (EntityBreedEvent)event;
            exp = entityBreedEvent.getExperience();
        } else if (event instanceof PlayerFishEvent) {
            PlayerFishEvent fishEvent = (PlayerFishEvent)event;
            exp = fishEvent.getExpToDrop();
        } else {
            return;
        }
        if (delta != null) {
            block5: for (Object object : delta) {
                int n;
                if (object instanceof Experience) {
                    Experience experience = (Experience)object;
                    n = experience.getXP();
                } else {
                    n = (Integer)object;
                }
                int value = n;
                switch (mode) {
                    case ADD: {
                        exp += value;
                        continue block5;
                    }
                    case SET: {
                        exp = value;
                        continue block5;
                    }
                    case REMOVE: 
                    case REMOVE_ALL: {
                        exp -= value;
                    }
                }
            }
        } else {
            exp = 0;
        }
        exp = Math.max(0, exp);
        if (event instanceof ExperienceSpawnEvent) {
            experienceSpawnEvent = (ExperienceSpawnEvent)event;
            ((ExperienceSpawnEvent)((Object)experienceSpawnEvent)).setSpawnedXP(exp);
        } else if (event instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent)event;
            blockBreakEvent.setExpToDrop(exp);
        } else if (event instanceof PlayerExpChangeEvent) {
            PlayerExpChangeEvent playerExpChangeEvent = (PlayerExpChangeEvent)event;
            playerExpChangeEvent.setAmount(exp);
        } else if (event instanceof EntityBreedEvent) {
            entityBreedEvent = (EntityBreedEvent)event;
            entityBreedEvent.setExperience(exp);
        } else if (event instanceof PlayerFishEvent) {
            PlayerFishEvent fishEvent = (PlayerFishEvent)event;
            fishEvent.setExpToDrop(exp);
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Experience> getReturnType() {
        return Experience.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the experience";
    }

    static {
        Skript.registerExpression(ExprExperience.class, Experience.class, ExpressionType.SIMPLE, "[the] (spawned|dropped|) [e]xp[erience] [orb[s]]");
    }
}

