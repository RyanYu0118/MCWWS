/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Enforce Whitelist")
@Description(value={"Enforces or un-enforce a server's whitelist.", "All non-whitelisted players will be kicked upon enforcing the whitelist."})
@Example.Examples(value={@Example(value="enforce the whitelist"), @Example(value="unenforce the whitelist")})
@Since(value={"2.9.0"})
@RequiredPlugins(value={"MC 1.17+"})
public class EffEnforceWhitelist
extends Effect {
    private static final Component NOT_WHITELISTED_MESSAGE;
    private boolean enforce;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.enforce = !parseResult.hasTag("un");
        return true;
    }

    @Override
    protected void execute(Event event) {
        Bukkit.setWhitelistEnforced((boolean)this.enforce);
        EffEnforceWhitelist.reloadWhitelist();
    }

    public static void reloadWhitelist() {
        Bukkit.reloadWhitelist();
        if (!Bukkit.hasWhitelist() || !Bukkit.isWhitelistEnforced()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isWhitelisted() || player.isOp()) continue;
            player.kick(NOT_WHITELISTED_MESSAGE);
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (!this.enforce ? "un" : "") + "enforce the whitelist";
    }

    static {
        String whitelistMessage = "You are not whitelisted on this server!";
        try {
            YamlConfiguration spigotYml = YamlConfiguration.loadConfiguration((File)new File("spigot.yml"));
            whitelistMessage = spigotYml.getString("messages.whitelist", whitelistMessage);
        }
        catch (Exception exception) {
            // empty catch block
        }
        NOT_WHITELISTED_MESSAGE = LegacyComponentSerializer.legacyAmpersand().deserialize(whitelistMessage.replaceAll("\\\\n", "\n"));
        Skript.registerEffect(EffEnforceWhitelist.class, "[:un]enforce [the] [server] white[ ]list");
    }
}

