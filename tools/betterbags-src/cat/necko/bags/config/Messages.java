/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package cat.necko.bags.config;

import cat.necko.bags.Plugin;
import cat.necko.bags.utils.AbstractConfig;
import cat.necko.bags.utils.StringUtil;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Messages
extends AbstractConfig {
    public Messages(Plugin plugin) {
        super(plugin, "messages.yml");
    }

    @NotNull
    public String getLegacyString(@NotNull String path) {
        if (path == null) {
            Messages.$$$reportNull$$$0(0);
        }
        String string = this.getLegacyString(path, null);
        if (string == null) {
            Messages.$$$reportNull$$$0(1);
        }
        return string;
    }

    @NotNull
    public String getLegacyString(@NotNull String path, @Nullable String def) {
        if (path == null) {
            Messages.$$$reportNull$$$0(2);
        }
        Component component = this.getString(path, def);
        String string = LegacyComponentSerializer.legacySection().serialize(component);
        if (string == null) {
            Messages.$$$reportNull$$$0(3);
        }
        return string;
    }

    @NotNull
    public Component getString(@NotNull String path) {
        if (path == null) {
            Messages.$$$reportNull$$$0(4);
        }
        Component component = this.getString(path, null, (String string) -> string);
        if (component == null) {
            Messages.$$$reportNull$$$0(5);
        }
        return component;
    }

    @NotNull
    public Component getString(@NotNull String path, @Nullable String def) {
        if (path == null) {
            Messages.$$$reportNull$$$0(6);
        }
        Component component = this.getString(path, def, (String string) -> string);
        if (component == null) {
            Messages.$$$reportNull$$$0(7);
        }
        return component;
    }

    @NotNull
    public Component getString(@NotNull String path, @NotNull Function<String, String> replacer) {
        if (path == null) {
            Messages.$$$reportNull$$$0(8);
        }
        if (replacer == null) {
            Messages.$$$reportNull$$$0(9);
        }
        Component component = this.getString(path, null, replacer);
        if (component == null) {
            Messages.$$$reportNull$$$0(10);
        }
        return component;
    }

    @NotNull
    public Component getString(@NotNull String path, @Nullable String def, @NotNull Function<String, String> replacer) {
        String message;
        Object object;
        if (path == null) {
            Messages.$$$reportNull$$$0(11);
        }
        if (replacer == null) {
            Messages.$$$reportNull$$$0(12);
        }
        if ((object = this.getConfig().get(path, (Object)def)) instanceof List) {
            StringBuilder builder = new StringBuilder();
            for (String s : (List)object) {
                builder.append(replacer.apply(s)).append("\n");
            }
            message = builder.toString().trim();
        } else {
            message = replacer.apply((String)object);
        }
        Component component = MiniMessage.miniMessage().deserialize((Object)message);
        if (component == null) {
            Messages.$$$reportNull$$$0(13);
        }
        return component;
    }

    @NotNull
    public Component getString(@NotNull UUID uuid, @NotNull String path) {
        if (uuid == null) {
            Messages.$$$reportNull$$$0(14);
        }
        if (path == null) {
            Messages.$$$reportNull$$$0(15);
        }
        Component component = this.getString(uuid, path, null);
        if (component == null) {
            Messages.$$$reportNull$$$0(16);
        }
        return component;
    }

    @NotNull
    public Component getString(@NotNull UUID uuid, @NotNull String path, @Nullable String def) {
        if (uuid == null) {
            Messages.$$$reportNull$$$0(17);
        }
        if (path == null) {
            Messages.$$$reportNull$$$0(18);
        }
        Component component = StringUtil.prepareFor(uuid, this.getConfig().getString(path, def));
        if (component == null) {
            Messages.$$$reportNull$$$0(19);
        }
        return component;
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2;
        Object[] objectArray3 = new Object[switch (n) {
            default -> 3;
            case 1, 3, 5, 7, 10, 13, 16, 19 -> 2;
        }];
        switch (n) {
            default: {
                objectArray2 = objectArray3;
                objectArray3[0] = "path";
                break;
            }
            case 1: 
            case 3: 
            case 5: 
            case 7: 
            case 10: 
            case 13: 
            case 16: 
            case 19: {
                objectArray2 = objectArray3;
                objectArray3[0] = "cat/necko/bags/config/Messages";
                break;
            }
            case 9: 
            case 12: {
                objectArray2 = objectArray3;
                objectArray3[0] = "replacer";
                break;
            }
            case 14: 
            case 17: {
                objectArray2 = objectArray3;
                objectArray3[0] = "uuid";
                break;
            }
        }
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[1] = "cat/necko/bags/config/Messages";
                break;
            }
            case 1: 
            case 3: {
                objectArray = objectArray2;
                objectArray2[1] = "getLegacyString";
                break;
            }
            case 5: 
            case 7: 
            case 10: 
            case 13: 
            case 16: 
            case 19: {
                objectArray = objectArray2;
                objectArray2[1] = "getString";
                break;
            }
        }
        switch (n) {
            default: {
                objectArray = objectArray;
                objectArray[2] = "getLegacyString";
                break;
            }
            case 1: 
            case 3: 
            case 5: 
            case 7: 
            case 10: 
            case 13: 
            case 16: 
            case 19: {
                break;
            }
            case 4: 
            case 6: 
            case 8: 
            case 9: 
            case 11: 
            case 12: 
            case 14: 
            case 15: 
            case 17: 
            case 18: {
                objectArray = objectArray;
                objectArray[2] = "getString";
                break;
            }
        }
        String string = String.format(v0, objectArray);
        throw switch (n) {
            default -> new IllegalArgumentException(string);
            case 1, 3, 5, 7, 10, 13, 16, 19 -> new IllegalStateException(string);
        };
    }
}

