/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package cat.necko.bags.common;

import cat.necko.bags.Plugin;
import cat.necko.bags.bag.data.PlayerData;
import cat.necko.bags.bag.inventory.BagInventory;
import cat.necko.bags.config.bags.BagsData;
import cat.necko.bags.utils.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Command
implements TabCompleter,
CommandExecutor {
    private final Plugin plugin;
    public static final List<String> SUB_COMMANDS = List.of("clear", "give", "level", "reload", "save", "open", "multiplier", "sell-all");
    public static final List<String> SELECTORS = List.of("@a", "@e", "@n", "@p", "@r", "@s");

    public Command(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (sender == null) {
            Command.$$$reportNull$$$0(0);
        }
        if (label == null) {
            Command.$$$reportNull$$$0(1);
        }
        if (command == null) {
            Command.$$$reportNull$$$0(2);
        }
        if (args == null) {
            Command.$$$reportNull$$$0(3);
        }
        if (args.length < 1) {
            return false;
        }
        String subCommand = args[0];
        if (!SUB_COMMANDS.contains(subCommand.toLowerCase())) {
            sender.sendMessage(this.plugin.getMessages().getString("help-message"));
            return true;
        }
        switch (subCommand.toLowerCase()) {
            case "reload": {
                if (!sender.hasPermission("betterbags.reload")) {
                    return false;
                }
                this.plugin.reloadConfig();
                sender.sendMessage(this.plugin.getMessages().getString("config-reloaded"));
                break;
            }
            case "save": {
                if (!sender.hasPermission("betterbags.save")) {
                    return false;
                }
                this.plugin.saveAll();
                sender.sendMessage(this.plugin.getMessages().getString("data-saved"));
                break;
            }
            case "clear": {
                if (!sender.hasPermission("betterbags.clear") || args.length < 2) {
                    return false;
                }
                String targetArg = args[1];
                return this.consumeFor(sender, targetArg, player -> this.plugin.getPlayerData(player.getUniqueId()).clearBag(), (result, type) -> {
                    String key = switch (type.ordinal()) {
                        default -> throw new MatchException(null, null);
                        case 0 -> "bag-clear-result.single";
                        case 1 -> "bag-clear-result.multiple";
                    };
                    Component message = this.plugin.getMessages().getString(key, s -> s.replace("%target%", (CharSequence)result));
                    sender.sendMessage(message);
                });
            }
            case "level": {
                int level;
                if (!sender.hasPermission("betterbags.setlevel") || args.length < 3) {
                    return false;
                }
                String targetArg = args[1];
                try {
                    level = Integer.parseInt(args[2]);
                }
                catch (NumberFormatException e) {
                    return false;
                }
                return this.consumeFor(sender, targetArg, player -> this.plugin.getPlayerData(player.getUniqueId()).setBagLevel(level), (result, type) -> {
                    int normalizedLevel = this.plugin.getBagsData().getBagLevels().normalizeLevel(level);
                    String key = switch (type.ordinal()) {
                        default -> throw new MatchException(null, null);
                        case 0 -> "bag-level-result.single";
                        case 1 -> "bag-level-result.multiple";
                    };
                    Component message = this.plugin.getMessages().getString(key, s -> s.replace("%target%", (CharSequence)result).replace("%level%", String.valueOf(normalizedLevel)));
                    sender.sendMessage(message);
                });
            }
            case "give": {
                if (!sender.hasPermission("betterbags.give") || args.length < 2) {
                    return false;
                }
                String targetArg = args[1];
                return this.consumeFor(sender, targetArg, BagsData::giveBagToPlayer, (result, type) -> {
                    String key = switch (type.ordinal()) {
                        default -> throw new MatchException(null, null);
                        case 0 -> "give-bag-result.single";
                        case 1 -> "give-bag-result.multiple";
                    };
                    Component message = this.plugin.getMessages().getString(key, s -> s.replace("%target%", (CharSequence)result));
                    sender.sendMessage(message);
                });
            }
            case "open": {
                UUID targetUuid;
                if (!sender.hasPermission("betterbags.open") || !(sender instanceof Player)) {
                    return false;
                }
                Player player2 = (Player)sender;
                UUID uUID = targetUuid = args.length > 1 ? Bukkit.getPlayerUniqueId((String)args[1]) : player2.getUniqueId();
                if (targetUuid == null) {
                    return false;
                }
                PlayerData target = this.plugin.getPlayerData(targetUuid);
                new BagInventory(target).openFor(player2);
                break;
            }
            case "multiplier": {
                float multiplier;
                if (!sender.hasPermission("betterbags.multiplier") || args.length < 3) {
                    return false;
                }
                String targetArg = args[1];
                try {
                    multiplier = Float.parseFloat(args[2]);
                }
                catch (NumberFormatException e) {
                    return false;
                }
                return this.consumeFor(sender, targetArg, player -> this.plugin.getPlayerData(player.getUniqueId()).setMultiplier(multiplier), (result, type) -> {
                    String key = switch (type.ordinal()) {
                        default -> throw new MatchException(null, null);
                        case 0 -> "multiplier-set-result.single";
                        case 1 -> "multiplier-set-result.multiple";
                    };
                    Component message = this.plugin.getMessages().getString(key, s -> s.replace("%target%", (CharSequence)result).replace("%multiplier%", String.valueOf(multiplier)));
                    sender.sendMessage(message);
                });
            }
            case "sell-all": {
                boolean ignoreItemValue;
                if (!sender.hasPermission("betterbags.sell-all") || args.length < 3) {
                    return false;
                }
                String targetArg = args[1];
                try {
                    ignoreItemValue = Boolean.parseBoolean(args[2]);
                }
                catch (Exception e) {
                    ignoreItemValue = false;
                }
                if (targetArg.startsWith("@")) {
                    List entities = this.plugin.getServer().selectEntities(sender, targetArg);
                    int count = 0;
                    int amount = 0;
                    int cost = 0;
                    for (Entity entity : entities) {
                        if (!(entity instanceof Player)) continue;
                        Player player3 = (Player)entity;
                        Tuple<Float, Integer> result2 = this.plugin.getPlayerData(player3.getUniqueId()).sellAndDeposit(ignoreItemValue);
                        amount += result2.b().intValue();
                        cost = (int)((float)cost + result2.a().floatValue());
                        if (result2.b() > 0) {
                            player3.sendMessage(Plugin.getInstance().getMessages().getString("sell-all.something", s -> s.replace("%amount%", String.valueOf(result2.b())).replace("%cost%", String.valueOf(result2.a()))));
                        }
                        ++count;
                    }
                    int finalCount = count;
                    int finalAmount = amount;
                    int finalCost = cost;
                    sender.sendMessage(Plugin.getInstance().getMessages().getString("sell-all-command-result.multiple", s -> s.replace("%target%", String.valueOf(finalCount)).replace("%amount%", String.valueOf(finalAmount)).replace("%cost%", String.valueOf(finalCost))));
                    return true;
                }
                Player player4 = this.plugin.getServer().getPlayer(targetArg);
                if (player4 == null) {
                    return false;
                }
                Tuple<Float, Integer> result3 = this.plugin.getPlayerData(player4.getUniqueId()).sellAndDeposit(ignoreItemValue);
                if (result3.b() > 0) {
                    player4.sendMessage(Plugin.getInstance().getMessages().getString("sell-all.something", s -> s.replace("%amount%", String.valueOf(result3.b())).replace("%cost%", String.valueOf(result3.a()))));
                }
                sender.sendMessage(Plugin.getInstance().getMessages().getString("sell-all-command-result.single", s -> s.replace("%target%", player4.getName()).replace("%amount%", String.valueOf(result3.b())).replace("%cost%", String.valueOf(result3.a()))));
            }
        }
        return true;
    }

    private boolean consumeFor(CommandSender sender, String selector, Consumer<Player> consumer, BiConsumer<String, ResultType> whenDone) {
        if (selector.startsWith("@")) {
            List entities = this.plugin.getServer().selectEntities(sender, selector);
            int count = 0;
            for (Entity entity : entities) {
                if (!(entity instanceof Player)) continue;
                Player player = (Player)entity;
                consumer.accept(player);
                ++count;
            }
            whenDone.accept(String.valueOf(count), ResultType.MULTIPLE);
            return true;
        }
        Player player = this.plugin.getServer().getPlayer(selector);
        if (player == null) {
            return false;
        }
        consumer.accept(player);
        whenDone.accept(player.getName(), ResultType.SINGLE);
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender == null) {
            Command.$$$reportNull$$$0(4);
        }
        if (label == null) {
            Command.$$$reportNull$$$0(5);
        }
        if (cmd == null) {
            Command.$$$reportNull$$$0(6);
        }
        if (args == null) {
            Command.$$$reportNull$$$0(7);
        }
        ArrayList<String> result = new ArrayList<String>();
        block0 : switch (args.length) {
            case 1: {
                result.addAll(SUB_COMMANDS);
                break;
            }
            case 2: {
                List<String> onlinePlayers = this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
                switch (args[0].toLowerCase()) {
                    case "clear": 
                    case "level": 
                    case "give": 
                    case "multiplier": 
                    case "sell-all": {
                        result.addAll(SELECTORS);
                        result.addAll(onlinePlayers);
                        break;
                    }
                    case "open": {
                        result.addAll(onlinePlayers);
                    }
                }
                break;
            }
            case 3: {
                switch (args[0].toLowerCase()) {
                    case "level": {
                        result.addAll(this.plugin.getBagsData().getBagLevels().getLevelsString());
                        break block0;
                    }
                    case "multiplier": {
                        result.add("<multiplier>");
                        break block0;
                    }
                    case "sell-all": {
                        result.addAll(List.of("<ignore-item-value>", "true", "false"));
                    }
                }
            }
        }
        return result;
    }

    private static /* synthetic */ void $$$reportNull$$$0(int n) {
        Object[] objectArray;
        Object[] objectArray2;
        Object[] objectArray3 = new Object[3];
        switch (n) {
            default: {
                objectArray2 = objectArray3;
                objectArray3[0] = "sender";
                break;
            }
            case 1: 
            case 5: {
                objectArray2 = objectArray3;
                objectArray3[0] = "label";
                break;
            }
            case 2: {
                objectArray2 = objectArray3;
                objectArray3[0] = "command";
                break;
            }
            case 3: 
            case 7: {
                objectArray2 = objectArray3;
                objectArray3[0] = "args";
                break;
            }
            case 6: {
                objectArray2 = objectArray3;
                objectArray3[0] = "cmd";
                break;
            }
        }
        objectArray2[1] = "cat/necko/bags/common/Command";
        switch (n) {
            default: {
                objectArray = objectArray2;
                objectArray2[2] = "onCommand";
                break;
            }
            case 4: 
            case 5: 
            case 6: 
            case 7: {
                objectArray = objectArray2;
                objectArray2[2] = "onTabComplete";
                break;
            }
        }
        throw new IllegalArgumentException(String.format("Argument for @NotNull parameter '%s' of %s.%s must not be null", objectArray));
    }

    private static enum ResultType {
        SINGLE,
        MULTIPLE;

    }
}

