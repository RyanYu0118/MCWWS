/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.audience.Audience
 *  net.kyori.adventure.text.Component
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.text.elements.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.text.TextComponentUtils;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Action Bar")
@Description(value={"Sends an action bar message to an audience."})
@Examples(value={"send action bar \"Hello player!\" to player"})
@Since(value={"2.3", "2.15 (support for sending anything)"})
public class EffActionBar
extends Effect {
    private Expression<? extends Component> message;
    private Expression<Audience> recipients;

    public static void register(SyntaxRegistry syntaxRegistry) {
        syntaxRegistry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffActionBar.class).supplier(EffActionBar::new).addPattern("send [the] action[ ]bar [with text] %object% [to %audiences%]").build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.message = TextComponentUtils.asComponentExpression(expressions[0]);
        if (this.message == null) {
            return false;
        }
        this.recipients = expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Component component = this.message.getSingle(event);
        if (component != null) {
            Audience.audience((Audience[])this.recipients.getArray(event)).sendActionBar(component);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("send the action bar", this.message);
        if (this.recipients != null) {
            builder.append("to", this.recipients);
        }
        return builder.toString();
    }
}

