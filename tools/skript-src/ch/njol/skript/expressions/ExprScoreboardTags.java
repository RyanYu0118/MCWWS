/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
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
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Scoreboard Tags")
@Description(value={"Scoreboard tags are simple list of texts stored directly in the data of an <a href='#entity'>entity</a>.", "So this is a Minecraft related thing, not Bukkit, so the tags will not get removed when the server stops. You can visit <a href='https://minecraft.wiki/w/Scoreboard#Tags'>visit Minecraft Wiki</a> for more info.", "This is changeable and valid for any type of entity. Also you can use use the <a href='#CondHasScoreboardTag'>Has Scoreboard Tag</a> condition to check whether an entity has the given tags.", "", "Requires Minecraft 1.11+ (actually added in 1.9 to the game, but added in 1.11 to Spigot)."})
@Example(value="on spawn of a monster:\n\tif the spawn reason is mob spawner:\n\t\tadd \"spawned by a spawner\" to the scoreboard tags of event-entity\n\non death of a monster:\n\tif the attacker is a player:\n\t\tif the victim doesn't have the scoreboard tag \"spawned by a spawner\":\n\t\t\tadd 1$ to attacker's balance\n")
@Since(value={"2.3"})
public class ExprScoreboardTags
extends SimpleExpression<String> {
    private Expression<Entity> entities;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        return true;
    }

    @Nullable
    public String[] get(Event e) {
        return (String[])Stream.of(this.entities.getArray(e)).map(Entity::getScoreboardTags).flatMap(Collection::stream).toArray(String[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        switch (mode) {
            case SET: 
            case ADD: 
            case REMOVE: 
            case DELETE: 
            case RESET: {
                return CollectionUtils.array(String[].class);
            }
        }
        return null;
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        block6: for (Entity entity : this.entities.getArray(e)) {
            switch (mode) {
                case SET: {
                    assert (delta != null);
                    entity.getScoreboardTags().clear();
                    for (Object tag : delta) {
                        entity.addScoreboardTag((String)tag);
                    }
                    continue block6;
                }
                case ADD: {
                    assert (delta != null);
                    for (Object tag : delta) {
                        entity.addScoreboardTag((String)tag);
                    }
                    continue block6;
                }
                case REMOVE: {
                    assert (delta != null);
                    for (Object tag : delta) {
                        entity.removeScoreboardTag((String)tag);
                    }
                    continue block6;
                }
                case DELETE: 
                case RESET: {
                    entity.getScoreboardTags().clear();
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the scoreboard tags of " + this.entities.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprScoreboardTags.class, String.class, ExpressionType.PROPERTY, "[(all [[of] the]|the)] scoreboard tags of %entities%", "%entities%'[s] scoreboard tags");
    }
}

