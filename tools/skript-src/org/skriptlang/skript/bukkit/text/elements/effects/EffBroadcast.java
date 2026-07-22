/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  net.kyori.adventure.audience.Audience
 *  net.kyori.adventure.audience.ForwardingAudience
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.bukkit.event.server.BroadcastMessageEvent
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentUtils;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Broadcast")
@Description(value={"Broadcasts a message to the server."})
@Example.Examples(value={@Example(value="broadcast \"Welcome %player% to the server!\""), @Example(value="broadcast \"Woah! It's a message!\"")})
@Since(value={"1.0", "2.6 (support for broadcasting anything)", "2.6.1 (using advanced formatting)"})
public class EffBroadcast
extends Effect {
    private Expression<? extends Component> messages;
    @Nullable
    private Expression<World> worlds;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffBroadcast.class).supplier(EffBroadcast::new).addPattern("broadcast %objects% [(to|in) %-worlds%]").build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.messages = TextComponentUtils.asComponentExpression(expressions[0]);
        if (this.messages == null) {
            return false;
        }
        if (expressions[1] != null) {
            this.worlds = expressions[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (this.worlds == null) {
            for (Component component : this.messages.getArray(event)) {
                Bukkit.broadcast((Component)component);
            }
            return;
        }
        ImmutableSet.Builder recipientsBuilder = ImmutableSet.builder();
        for (World world : this.worlds.getArray(event)) {
            recipientsBuilder.addAll((Iterable)world.getPlayers());
        }
        ImmutableSet recipients = recipientsBuilder.build();
        ForwardingAudience audience = Audience.audience((Iterable)recipients);
        boolean isAsync = !Bukkit.isPrimaryThread();
        for (Component component : this.messages.getArray(event)) {
            if (!new BroadcastMessageEvent(isAsync, component, (Set)recipients).callEvent()) continue;
            audience.sendMessage(component);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("broadcast", this.messages);
        if (this.worlds != null) {
            builder.append("in", this.worlds);
        }
        return builder.toString();
    }
}

