/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Version")
@Description(value={"The version of Bukkit, Minecraft or Skript respectively."})
@Example.Examples(value={@Example(value="message \"This server is running Minecraft %minecraft version% on Bukkit %bukkit version%\""), @Example(value="message \"This server is powered by Skript %skript version%\"")})
@Since(value={"2.0"})
public class ExprVersion
extends SimpleExpression<String> {
    private VersionType type;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.type = VersionType.values()[parseResult.mark];
        return true;
    }

    protected String[] get(Event e) {
        return new String[]{this.type.get()};
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return String.valueOf((Object)this.type) + " version";
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    static {
        Skript.registerExpression(ExprVersion.class, String.class, ExpressionType.SIMPLE, "(0\u00a6[craft]bukkit|1\u00a6minecraft|2\u00a6skript)( |-)version");
    }

    private static enum VersionType {
        BUKKIT("Bukkit"){

            @Override
            public String get() {
                return Bukkit.getBukkitVersion();
            }
        }
        ,
        MINECRAFT("Minecraft"){

            @Override
            public String get() {
                return Skript.getMinecraftVersion().toString();
            }
        }
        ,
        SKRIPT("Skript"){

            @Override
            public String get() {
                return Skript.getVersion().toString();
            }
        };

        private final String name;

        private VersionType(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }

        public abstract String get();
    }
}

