/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Player$Spigot
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Name(value="Language")
@Description(value={"Currently selected game language of a player. The value of the language is not defined properly.", "The vanilla Minecraft client will use lowercase language / country pairs separated by an underscore, but custom resource packs may use any format they wish."})
@Example(value="message player's current language")
@Since(value={"2.3"})
public class ExprLanguage
extends SimplePropertyExpression<Player, String> {
    private static final boolean USE_DEPRECATED_METHOD;
    @Nullable
    private static final MethodHandle getLocaleMethod;

    @Override
    @Nullable
    public String convert(Player p) {
        if (USE_DEPRECATED_METHOD) {
            assert (getLocaleMethod != null);
            try {
                return getLocaleMethod.invoke(p.spigot());
            }
            catch (Throwable e) {
                Skript.exception(e, new String[0]);
                return null;
            }
        }
        return p.getLocale();
    }

    @Override
    protected String getPropertyName() {
        return "language";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    static {
        MethodHandle handle;
        USE_DEPRECATED_METHOD = !Skript.methodExists(Player.class, "getLocale", new Class[0]);
        ExprLanguage.register(ExprLanguage.class, String.class, "[([currently] selected|current)] [game] (language|locale) [setting]", "players");
        try {
            handle = MethodHandles.lookup().findVirtual(Player.Spigot.class, "getLocale", MethodType.methodType(String.class));
        }
        catch (IllegalAccessException | NoSuchMethodException e) {
            handle = null;
        }
        getLocaleMethod = handle;
    }
}

