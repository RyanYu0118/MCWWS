/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Server
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.Skript;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;

public class CommandReloader {
    @Nullable
    private static Method syncCommandsMethod;

    public static boolean syncCommands(Server server) {
        if (syncCommandsMethod == null) {
            return false;
        }
        try {
            syncCommandsMethod.invoke((Object)server, new Object[0]);
            return true;
        }
        catch (Throwable e) {
            if (Skript.debug()) {
                Skript.info("syncCommands failed; stack trace for debugging below");
                e.printStackTrace();
            }
            return false;
        }
    }

    static {
        block3: {
            try {
                syncCommandsMethod = Bukkit.getServer().getClass().getDeclaredMethod("syncCommands", new Class[0]);
                if (syncCommandsMethod != null) {
                    syncCommandsMethod.setAccessible(true);
                }
            }
            catch (NoSuchMethodException e) {
                if (!Skript.debug()) break block3;
                e.printStackTrace();
            }
        }
    }
}

