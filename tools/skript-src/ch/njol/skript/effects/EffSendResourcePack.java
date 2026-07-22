/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Send Resource Pack")
@Description(value={"Request that the player's client download and switch resource packs. The client will download ", "the resource pack in the background, and will automatically switch to it once the download is complete. ", "The URL must be a direct download link.", "", "The hash is used for caching, the player won't have to re-download the resource pack that way. ", "The hash must be SHA-1, you can get SHA-1 hash of your resource pack using ", "<a href=\"https://emn178.github.io/online-tools/sha1_checksum.html\">this online tool</a>.", "", "The <a href='#resource_pack_request_action'>resource pack request action</a> can be used to check ", "status of the sent resource pack request."})
@Example(value="on join:\n\tsend the resource pack from \"URL\" with hash \"hash\" to the player\n")
@Since(value={"2.4"})
public class EffSendResourcePack
extends Effect {
    private static final boolean PAPER_METHOD_EXISTS;
    private Expression<String> url;
    @Nullable
    private Expression<String> hash;
    private Expression<Player> recipients;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.url = exprs[0];
        if (matchedPattern == 0) {
            this.recipients = exprs[1];
        } else {
            this.hash = exprs[1];
            this.recipients = exprs[2];
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        String address;
        assert (this.url != null);
        String hash = null;
        if (this.hash != null) {
            hash = this.hash.getSingle(e);
        }
        if ((address = this.url.getSingle(e)) == null) {
            return;
        }
        for (Player p : this.recipients.getArray(e)) {
            try {
                if (hash == null) {
                    p.setResourcePack(address);
                    continue;
                }
                if (PAPER_METHOD_EXISTS) {
                    p.setResourcePack(address, hash);
                    continue;
                }
                p.setResourcePack(address, StringUtils.hexStringToByteArray(hash));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "send the resource pack from " + this.url.toString(e, debug) + (String)(this.hash != null ? " with hash " + this.hash.toString(e, debug) : "") + " to " + this.recipients.toString(e, debug);
    }

    static {
        Skript.registerEffect(EffSendResourcePack.class, "send [the] resource pack [from [[the] URL]] %string% to %players%", "send [the] resource pack [from [[the] URL]] %string% with hash %string% to %players%");
        PAPER_METHOD_EXISTS = Skript.methodExists(Player.class, "setResourcePack", String.class, String.class);
    }
}

