/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.world.MoonPhase
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.Difficulty
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.GameMode
 *  org.bukkit.GameRule
 *  org.bukkit.Material
 *  org.bukkit.Registry
 *  org.bukkit.SoundCategory
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.WorldBorder
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.block.Biome
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.DoubleChest
 *  org.bukkit.block.banner.Pattern
 *  org.bukkit.block.banner.PatternType
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.command.CommandSender
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.enchantments.EnchantmentOffer
 *  org.bukkit.entity.EntitySnapshot
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Vehicle
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.entity.Villager$Type
 *  org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityRegainHealthEvent$RegainReason
 *  org.bukkit.event.entity.EntityTransformEvent$TransformReason
 *  org.bukkit.event.entity.EntityUnleashEvent$UnleashReason
 *  org.bukkit.event.entity.VillagerCareerChangeEvent$ChangeReason
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryCloseEvent$Reason
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerExpCooldownChangeEvent$ChangeReason
 *  org.bukkit.event.player.PlayerQuitEvent$QuitReason
 *  org.bukkit.event.player.PlayerResourcePackStatusEvent$Status
 *  org.bukkit.event.player.PlayerRespawnEvent$RespawnReason
 *  org.bukkit.event.player.PlayerTeleportEvent$TeleportCause
 *  org.bukkit.generator.WorldInfo
 *  org.bukkit.inventory.BlockInventoryHolder
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.metadata.Metadatable
 *  org.bukkit.util.CachedServerIcon
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.Skript;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.bukkitutil.SkriptTeleportFlag;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.PatternedParser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.classes.data.DefaultChangers;
import ch.njol.skript.classes.registry.RegistryClassInfo;
import ch.njol.skript.expressions.ExprDamageCause;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.BlockUtils;
import ch.njol.yggdrasil.Fields;
import io.papermc.paper.world.MoonPhase;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.regex.Matcher;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerExpCooldownChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.metadata.Metadatable;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.types.EntityClassInfo;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

public class BukkitClasses {
    static {
        ClassInfo patternTypeInfo;
        Classes.registerClass(new ClassInfo<LivingEntity>(LivingEntity.class, "livingentity").user("living ?entit(y|ies)").name("Living Entity").description("A living <a href='#entity'>entity</a>, i.e. a mob or <a href='#player'>player</a>, not inanimate entities like <a href='#projectile'>projectiles</a> or dropped items.").usage("see <a href='#entity'>entity</a>, but ignore inanimate objects").examples("spawn 5 powered creepers", "shoot a zombie from the creeper").since("1.0").defaultExpression(new EventValueExpression<LivingEntity>(LivingEntity.class)).changer(new EntityClassInfo.EntityChanger()));
        Classes.registerClass(new ClassInfo<Projectile>(Projectile.class, "projectile").user("projectiles?").name("Projectile").description("A projectile, e.g. an arrow, snowball or thrown potion.").usage("arrow, fireball, snowball, thrown potion, etc.").examples("projectile is a snowball", "shoot an arrow at speed 5 from the player").since("1.0").defaultExpression(new EventValueExpression<Projectile>(Projectile.class)).changer(DefaultChangers.nonLivingEntityChanger));
        Classes.registerClass(new ClassInfo<BlockData>(BlockData.class, "blockdata").user("block ?datas?").name("Block Data").description("Block data is the detailed information about a block, referred to in Minecraft as BlockStates, allowing for the manipulation of different aspects of the block, including shape, waterlogging, direction the block is facing, and so much more. Information regarding each block's optional data can be found on Minecraft's Wiki. Find the block you're looking for and scroll down to 'Block States'. Different states must be separated by a semicolon (see examples). The 'minecraft:' namespace is optional, as well as are underscores.").examples("set block at player to campfire[lit=false]", "set target block of player to oak stairs[facing=north;waterlogged=true]", "set block at player to grass_block[snowy=true]", "set loop-block to minecraft:chest[facing=north]", "set block above player to oak_log[axis=y]", "set target block of player to minecraft:oak_leaves[distance=2;persistent=false]").after("itemtype").since("2.5").parser(new Parser<BlockData>(){

            @Override
            @Nullable
            public BlockData parse(String input, ParseContext context) {
                return BlockUtils.createBlockData(input);
            }

            @Override
            public String toString(BlockData o, int flags) {
                return o.getAsString().replace(",", ";");
            }

            @Override
            public String toVariableNameString(BlockData o) {
                return "blockdata:" + o.getAsString();
            }
        }).serializer(new Serializer<BlockData>(){

            @Override
            public Fields serialize(BlockData blockData) {
                return Fields.singletonObject("blockdata", blockData.getAsString());
            }

            @Override
            protected BlockData deserialize(Fields fields) throws StreamCorruptedException {
                String data = fields.getObject("blockdata", String.class);
                assert (data != null);
                try {
                    return Bukkit.createBlockData((String)data);
                }
                catch (IllegalArgumentException ex) {
                    throw new StreamCorruptedException("Invalid block data: " + data);
                }
            }

            @Override
            public boolean mustSyncDeserialization() {
                return true;
            }

            @Override
            protected boolean canBeInstantiated() {
                return false;
            }
        }).cloner(BlockData::clone));
        Classes.registerClass(new ClassInfo<World>(World.class, "world").user("worlds?").name("World").description("One of the server's worlds. Worlds can be put into scripts by surrounding their name with double quotes, e.g. \"world_nether\", but this might not work reliably as <a href='#string'>text</a> uses the same syntax.").usage("<code>\"world_name\"</code>, e.g. \"world\"").examples("broadcast \"Hello!\" to the world \"world_nether\"").since("1.0, 2.2 (alternate syntax)").after("string").defaultExpression(new EventValueExpression<World>(World.class)).parser(new Parser<World>(){
            private final java.util.regex.Pattern parsePattern = java.util.regex.Pattern.compile("(?:(?:the )?world )?\"(.+)\"", 2);

            @Override
            @Nullable
            public World parse(String s, ParseContext context) {
                if (context == ParseContext.COMMAND || context == ParseContext.PARSE || context == ParseContext.CONFIG) {
                    return Bukkit.getWorld((String)s);
                }
                Matcher m = this.parsePattern.matcher(s);
                if (m.matches()) {
                    return Bukkit.getWorld((String)m.group(1));
                }
                return null;
            }

            @Override
            public String toString(World world, int flags) {
                return world.getName();
            }

            @Override
            public String toVariableNameString(World world) {
                return world.getName();
            }
        }).serializer(new Serializer<World>(){

            @Override
            public Fields serialize(World world) {
                return Fields.singletonObject("name", world.getName());
            }

            @Override
            public boolean canBeInstantiated() {
                return false;
            }

            @Override
            protected World deserialize(Fields fields) throws StreamCorruptedException {
                String name = fields.getObject("name", String.class);
                assert (name != null);
                World world = Bukkit.getWorld((String)name);
                if (world == null) {
                    throw new StreamCorruptedException("Missing world " + name);
                }
                return world;
            }

            @Override
            @Nullable
            public World deserialize(String s) {
                return Bukkit.getWorld((String)s);
            }

            @Override
            public boolean mustSyncDeserialization() {
                return true;
            }
        }).property(Property.NAME, "A world's name, as text. Cannot be changed.", Skript.instance(), ExpressionPropertyHandler.of(WorldInfo::getName, String.class)));
        Classes.registerClass(new EnumClassInfo<InventoryAction>(InventoryAction.class, "inventoryaction", "inventory actions").user("inventory ?actions?").name("Inventory Action").description("What player just did in inventory event. Note that when in creative game mode, most actions do not work correctly.").examples("").since("2.2-dev16"));
        Classes.registerClass(new EnumClassInfo<ClickType>(ClickType.class, "clicktype", "click types").user("click ?types?").name("Click Type").description("Click type, mostly for inventory events. Tells exactly which keys/buttons player pressed, assuming that default keybindings are used in client side.").examples("").since("2.2-dev16b, 2.2-dev35 (renamed to click type)"));
        Classes.registerClass(new EnumClassInfo<InventoryType>(InventoryType.class, "inventorytype", "inventory types").user("inventory ?types?").name("Inventory Type").description("Minecraft has several different inventory types with their own use cases.").examples("").since("2.2-dev32"));
        Classes.registerClass(new ClassInfo<CommandSender>(CommandSender.class, "commandsender").user("((commands?)? ?)?(sender|executor)s?").name("Command Sender").description("A player or the console.").usage("use <a href='#LitConsole'>the console</a> for the console", "see <a href='#player'>player</a> for players.").examples("command /push [<player>]:", "\ttrigger:", "\t\tif arg-1 is not set:", "\t\t\tif command sender is console:", "\t\t\t\tsend \"You can't push yourself as a console :\\\" to sender", "\t\t\t\tstop", "\t\t\tpush sender upwards with force 2", "\t\t\tsend \"Yay!\"", "\t\telse:", "\t\t\tpush arg-1 upwards with force 2", "\t\t\tsend \"Yay!\" to sender and arg-1").since("1.0").defaultExpression(new EventValueExpression<CommandSender>(CommandSender.class)).parser(new Parser<CommandSender>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(CommandSender s, int flags) {
                return s.getName();
            }

            @Override
            public String toVariableNameString(CommandSender s) {
                return s.getName();
            }
        }).property(Property.NAME, "A command sender's name, as text. Cannot be changed.", Skript.instance(), ExpressionPropertyHandler.of(CommandSender::name, Component.class)));
        Classes.registerClass(new ClassInfo<InventoryHolder>(InventoryHolder.class, "inventoryholder").name(ClassInfo.NO_DOC).defaultExpression(new EventValueExpression<InventoryHolder>(InventoryHolder.class)).after("entity", "block").parser(new Parser<InventoryHolder>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(InventoryHolder holder, int flags) {
                if (holder instanceof BlockState) {
                    return Classes.toString(((BlockState)holder).getBlock());
                }
                if (holder instanceof DoubleChest) {
                    return Classes.toString(holder.getInventory().getLocation().getBlock());
                }
                if (holder instanceof BlockInventoryHolder) {
                    return Classes.toString(((BlockInventoryHolder)holder).getBlock());
                }
                if (Classes.getSuperClassInfo(holder.getClass()).getC() == InventoryHolder.class) {
                    return holder.getClass().getSimpleName();
                }
                return Classes.toString(holder);
            }

            @Override
            public String toVariableNameString(InventoryHolder holder) {
                return this.toString(holder, 0);
            }
        }));
        Classes.registerClass(new EnumClassInfo<GameMode>(GameMode.class, "gamemode", "game modes", new SimpleLiteral<GameMode>(GameMode.SURVIVAL, true)).user("game ?modes?").name("Game Mode").description("The game modes survival, creative, adventure and spectator.").examples("player's gamemode is survival", "set the player argument's game mode to creative").since("1.0"));
        Classes.registerClass(new ClassInfo<Item>(Item.class, "itementity").name(ClassInfo.NO_DOC).since("2.0").changer(DefaultChangers.itemChanger));
        Classes.registerClass(new RegistryClassInfo<Biome>(Biome.class, Registry.BIOME, "biome", "biomes").user("biomes?").name("Biome").description("All possible biomes Minecraft uses to generate a world.", "NOTE: Minecraft namespaces are supported, ex: 'minecraft:basalt_deltas'.").examples("biome at the player is desert").since("1.4.4").after("damagecause"));
        Classes.registerClass(new EnumClassInfo<EntityDamageEvent.DamageCause>(EntityDamageEvent.DamageCause.class, "damagecause", "damage causes", new ExprDamageCause()).user("damage ?causes?").name("Damage Cause").description("The cause/type of a <a href='#damage'>damage event</a>, e.g. lava, fall, fire, drowning, explosion, poison, etc.", "Please note that support for this type is very rudimentary, e.g. lava, fire and burning, as well as projectile and attack are considered different types.").examples("").since("2.0").after("itemtype", "itemstack", "entitydata", "entitytype"));
        Classes.registerClass(new ClassInfo<Chunk>(Chunk.class, "chunk").user("chunks?").name("Chunk").description("A chunk is a cuboid of 16\u00d716\u00d7128 (x\u00d7z\u00d7y) blocks. Chunks are spread on a fixed rectangular grid in their world.").usage("").examples("").since("2.0").parser(new Parser<Chunk>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(Chunk c, int flags) {
                return "chunk (" + c.getX() + "," + c.getZ() + ") of " + c.getWorld().getName();
            }

            @Override
            public String toVariableNameString(Chunk c) {
                return c.getWorld().getName() + ":" + c.getX() + "," + c.getZ();
            }
        }).serializer(new Serializer<Chunk>(){

            @Override
            public Fields serialize(Chunk chunk) {
                Fields f = new Fields();
                f.putObject("world", chunk.getWorld());
                f.putPrimitive("x", chunk.getX());
                f.putPrimitive("z", chunk.getZ());
                return f;
            }

            @Override
            public boolean canBeInstantiated() {
                return false;
            }

            @Override
            protected Chunk deserialize(Fields fields) throws StreamCorruptedException {
                World world = fields.getObject("world", World.class);
                if (world == null) {
                    throw new StreamCorruptedException("Missing world");
                }
                int x = fields.getPrimitive("x", Integer.TYPE);
                int z = fields.getPrimitive("z", Integer.TYPE);
                return world.getChunkAt(x, z);
            }

            @Override
            @Nullable
            public Chunk deserialize(String s) {
                String[] split = s.split("[:,]");
                if (split.length != 3) {
                    return null;
                }
                World w = Bukkit.getWorld((String)split[0]);
                if (w == null) {
                    return null;
                }
                try {
                    int x = Integer.parseInt(split[1]);
                    int z = Integer.parseInt(split[1]);
                    return w.getChunkAt(x, z);
                }
                catch (NumberFormatException e) {
                    return null;
                }
            }

            @Override
            public boolean mustSyncDeserialization() {
                return true;
            }
        }));
        Classes.registerClass(new RegistryClassInfo<Enchantment>(Enchantment.class, Registry.ENCHANTMENT, "enchantment", "enchantments").user("enchantments?").name("Enchantment").description("An enchantment, e.g. 'sharpness' or 'fortune'. Unlike <a href='#enchantmenttype'>enchantment type</a> this type has no level, but you usually don't need to use this type anyway.", "NOTE: Minecraft namespaces are supported, ex: 'minecraft:basalt_deltas'.", "As of Minecraft 1.21 this will also support custom enchantments using namespaces, ex: 'myenchants:explosive'.").examples("").since("1.4.6").before("enchantmenttype"));
        final Material[] allMaterials = Material.values();
        Classes.registerClass(new ClassInfo<Material>(Material.class, "material").name(ClassInfo.NO_DOC).since("aliases-rework").serializer(new Serializer<Material>(){

            @Override
            public Fields serialize(Material material) {
                return Fields.singletonObject("i", material.ordinal());
            }

            @Override
            public Material deserialize(Fields fields) throws StreamCorruptedException {
                return fields.mapPrimitive("i", Integer.TYPE, i -> allMaterials[i]);
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }

            @Override
            protected boolean canBeInstantiated() {
                return false;
            }
        }));
        Classes.registerClass(new ClassInfo<Metadatable>(Metadatable.class, "metadataholder").user("metadata ?holders?").name("Metadata Holder").description("Something that can hold metadata (e.g. an entity or block)").examples("set metadata value \"super cool\" of player to true").since("2.2-dev36"));
        Classes.registerClass(new EnumClassInfo<PlayerTeleportEvent.TeleportCause>(PlayerTeleportEvent.TeleportCause.class, "teleportcause", "teleport causes").user("teleport ?(cause|reason|type)s?").name("Teleport Cause").description("The teleport cause in a <a href='#teleport'>teleport</a> event.").since("2.2-dev35"));
        Classes.registerClass(new EnumClassInfo<CreatureSpawnEvent.SpawnReason>(CreatureSpawnEvent.SpawnReason.class, "spawnreason", "spawn reasons").user("spawn(ing)? ?reasons?").name("Spawn Reason").description("The spawn reason in a <a href='#spawn'>spawn</a> event.").since("2.3"));
        Classes.registerClass(new EnumClassInfo<PlayerRespawnEvent.RespawnReason>(PlayerRespawnEvent.RespawnReason.class, "respawnreason", "respawn reasons").user("respawn(ing)? ?reasons?").name("Respawn Reason").description("The respawn reason in a <a href='#respawn'>respawn</a> event.").since("2.14"));
        if (Skript.classExists("com.destroystokyo.paper.event.server.PaperServerListPingEvent")) {
            Classes.registerClass(new ClassInfo<CachedServerIcon>(CachedServerIcon.class, "cachedservericon").user("server ?icons?").name("Server Icon").description("A server icon that was loaded using the <a href='#EffLoadServerIcon'>load server icon</a> effect.").examples("").since("2.3").parser(new Parser<CachedServerIcon>(){

                @Override
                public boolean canParse(ParseContext context) {
                    return false;
                }

                @Override
                public String toString(CachedServerIcon o, int flags) {
                    return "server icon";
                }

                @Override
                public String toVariableNameString(CachedServerIcon o) {
                    return "server icon";
                }
            }));
        }
        Classes.registerClass(new EnumClassInfo<FireworkEffect.Type>(FireworkEffect.Type.class, "fireworktype", "firework types").user("firework ?types?").name("Firework Type").description("The type of a <a href='#fireworkeffect'>fireworkeffect</a>.").since("2.4").documentationId("FireworkType"));
        Classes.registerClass(new ClassInfo<FireworkEffect>(FireworkEffect.class, "fireworkeffect").user("firework ?effects?").name("Firework Effect").usage("See <a href='/#FireworkType'>Firework Types</a>").description("A configuration of effects that defines the firework when exploded", "which can be used in the <a href='#EffFireworkLaunch'>launch firework</a> effect.", "See the <a href='#ExprFireworkEffect'>firework effect</a> expression for detailed patterns.").defaultExpression(new EventValueExpression<FireworkEffect>(FireworkEffect.class)).examples("launch flickering trailing burst firework colored blue and green at player", "launch trailing flickering star colored purple, yellow, blue, green and red fading to pink at target entity", "launch ball large colored red, purple and white fading to light green and black at player's location with duration 1").since("2.4").parser(new Parser<FireworkEffect>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(FireworkEffect effect, int flags) {
                return "Firework effect " + effect.toString();
            }

            @Override
            public String toVariableNameString(FireworkEffect effect) {
                return "firework effect " + effect.toString();
            }
        }));
        Classes.registerClass(new EnumClassInfo<Difficulty>(Difficulty.class, "difficulty", "difficulties").user("difficult(y|ies)").name("Difficulty").description("The difficulty of a <a href='#world'>world</a>.").since("2.3"));
        Classes.registerClass(new EnumClassInfo<PlayerResourcePackStatusEvent.Status>(PlayerResourcePackStatusEvent.Status.class, "resourcepackstate", "resource pack states").user("resource ?pack ?states?").name("Resource Pack State").description("The state in a <a href='#resource_pack_request_action'>resource pack request response</a> event.").since("2.4"));
        Classes.registerClass(new EnumClassInfo<SoundCategory>(SoundCategory.class, "soundcategory", "sound categories").user("sound ?categor(y|ies)").name("Sound Category").description("The category of a sound, they are used for sound options of Minecraft. See the <a href='#EffPlaySound'>play sound</a> and <a href='#EffStopSound'>stop sound</a> effects.").since("2.4"));
        Classes.registerClass(new EnumClassInfo<EntityRegainHealthEvent.RegainReason>(EntityRegainHealthEvent.RegainReason.class, "healreason", "heal reasons").user("(regen|heal) (reason|cause)").name("Heal Reason").description("The health regain reason in a <a href='#heal'>heal</a> event.").since("2.5"));
        PatternedParser<GameRule> gameRuleParser = new PatternedParser<GameRule>(){
            private final String[] patterns = (String[])Arrays.stream(GameRule.values()).map(GameRule::getName).toArray(String[]::new);

            @Override
            @Nullable
            public GameRule parse(String string, ParseContext context) {
                return GameRule.getByName((String)string);
            }

            @Override
            public String toString(GameRule gameRule, int flags) {
                return gameRule.getName();
            }

            @Override
            public String toVariableNameString(GameRule gameRule) {
                return gameRule.getName();
            }

            @Override
            public String[] getPatterns() {
                return this.patterns;
            }
        };
        Classes.registerClass(new ClassInfo<GameRule>(GameRule.class, "gamerule").user("gamerules?").name("Gamerule").description("A gamerule").usage(gameRuleParser.getCombinedPatterns()).since("2.5").requiredPlugins("Minecraft 1.13 or newer").supplier((GameRule[])GameRule.values()).parser((Parser<GameRule>)gameRuleParser).property(Property.NAME, "A gamerule's name, as text. Cannot be changed.", Skript.instance(), ExpressionPropertyHandler.of(GameRule::getName, String.class)));
        Classes.registerClass(new ClassInfo<EnchantmentOffer>(EnchantmentOffer.class, "enchantmentoffer").user("enchant[ment][ ]offers?").name("Enchantment Offer").description("The enchantmentoffer in an enchant prepare event.").examples("on enchant prepare:", "\tset enchant offer 1 to sharpness 1", "\tset the cost of enchant offer 1 to 10 levels").since("2.5").parser(new Parser<EnchantmentOffer>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(EnchantmentOffer eo, int flags) {
                return Classes.toString(eo.getEnchantment()) + " " + eo.getEnchantmentLevel();
            }

            @Override
            public String toVariableNameString(EnchantmentOffer eo) {
                return "offer:" + Classes.toString(eo.getEnchantment()) + "=" + eo.getEnchantmentLevel();
            }
        }));
        Classes.registerClass(new RegistryClassInfo<Attribute>(Attribute.class, Registry.ATTRIBUTE, "attributetype", "attribute types").user("attribute ?types?").name("Attribute Type").description("Represents the type of an attribute. Note that this type does not contain any numerical values.See <a href='https://minecraft.wiki/w/Attribute#Attributes'>attribute types</a> for more info.", "NOTE: Minecraft namespaces are supported, ex: 'minecraft:generic.attack_damage'.").since("2.5"));
        Classes.registerClass(new EnumClassInfo<World.Environment>(World.Environment.class, "environment", "environments").user("(world ?)?environments?").name("World Environment").description("Represents the environment of a world.").since("2.7"));
        if (Skript.classExists("io.papermc.paper.world.MoonPhase")) {
            Classes.registerClass(new EnumClassInfo<MoonPhase>(MoonPhase.class, "moonphase", "moon phases").user("(lunar|moon) ?phases?").name("Moon Phase").description("Represents the phase of a moon.").since("2.7"));
        }
        if (Skript.classExists("org.bukkit.event.player.PlayerQuitEvent$QuitReason")) {
            Classes.registerClass(new EnumClassInfo<PlayerQuitEvent.QuitReason>(PlayerQuitEvent.QuitReason.class, "quitreason", "quit reasons").user("(quit|disconnect) ?(reason|cause)s?").name("Quit Reason").description("Represents a quit reason from a <a href='/#quit'>player quit server event</a>.").since("2.8.0"));
        }
        if (Skript.classExists("org.bukkit.event.inventory.InventoryCloseEvent$Reason")) {
            Classes.registerClass(new EnumClassInfo<InventoryCloseEvent.Reason>(InventoryCloseEvent.Reason.class, "inventoryclosereason", "inventory close reasons").user("inventory ?close ?reasons?").name("Inventory Close Reasons").description("The inventory close reason in an <a href='/#inventory_close'>inventory close event</a>.").since("2.8.0"));
        }
        Classes.registerClass(new EnumClassInfo<EntityTransformEvent.TransformReason>(EntityTransformEvent.TransformReason.class, "transformreason", "transform reasons").user("(entity)? ?transform ?(reason|cause)s?").name("Transform Reason").description("Represents a transform reason of an <a href='#entity transform'>entity transform event</a>.").since("2.8.0"));
        Classes.registerClass(new EnumClassInfo<EntityUnleashEvent.UnleashReason>(EntityUnleashEvent.UnleashReason.class, "unleashreason", "unleash reasons").user("unleash ?(reason|cause)s?").name("Unleash Reason").description("Represents an unleash reason of an unleash event.").since("2.10"));
        Classes.registerClass(new EnumClassInfo<ItemFlag>(ItemFlag.class, "itemflag", "item flags").user("item ?flags?").name("Item Flag").description("Represents flags that may be applied to hide certain attributes of an item.").since("2.10"));
        Classes.registerClass(new EnumClassInfo<PlayerExpCooldownChangeEvent.ChangeReason>(PlayerExpCooldownChangeEvent.ChangeReason.class, "experiencecooldownchangereason", "experience cooldown change reasons").user("(experience|[e]xp) cooldown change (reason|cause)s?").name("Experience Cooldown Change Reason").description("Represents a change reason of an <a href='#experience cooldown change event'>experience cooldown change event</a>.").since("2.10"));
        Classes.registerClass(new RegistryClassInfo<Villager.Type>(Villager.Type.class, Registry.VILLAGER_TYPE, "villagertype", "villager types").user("villager ?types?").name("Villager Type").description("Represents the different types of villagers. These are usually the biomes a villager can be from.").after("biome").since("2.10"));
        Classes.registerClass(new RegistryClassInfo<Villager.Profession>(Villager.Profession.class, Registry.VILLAGER_PROFESSION, "villagerprofession", "villager professions").user("villager ?professions?").name("Villager Profession").description("Represents the different professions of villagers.").since("2.10"));
        if (Skript.classExists("org.bukkit.entity.EntitySnapshot")) {
            Classes.registerClass(new ClassInfo<EntitySnapshot>(EntitySnapshot.class, "entitysnapshot").user("entity ?snapshots?").name("Entity Snapshot").description("Represents a snapshot of an entity's data.", "This includes all of the data associated with an entity (its name, health, attributes, etc.), at the time this expression is used. Essentially, these are a way to create templates for entities.", "Individual attributes of a snapshot cannot be modified or retrieved.").since("2.10").parser(new Parser<EntitySnapshot>(){

                @Override
                public boolean canParse(ParseContext context) {
                    return false;
                }

                @Override
                public String toString(EntitySnapshot snapshot, int flags) {
                    return EntityUtils.toSkriptEntityData(snapshot.getEntityType()).toString() + " snapshot";
                }

                @Override
                public String toVariableNameString(EntitySnapshot snapshot) {
                    return this.toString(snapshot, 0);
                }
            }));
        }
        Classes.registerClass(new ClassInfo<WorldBorder>(WorldBorder.class, "worldborder").user("world ?borders?").name("World Border").description("Represents the border of a world or player.").since("2.11").parser(new Parser<WorldBorder>(){

            @Override
            public boolean canParse(ParseContext context) {
                return false;
            }

            @Override
            public String toString(WorldBorder border, int flags) {
                if (border.getWorld() == null) {
                    return "virtual world border";
                }
                return "world border of world named '" + border.getWorld().getName() + "'";
            }

            @Override
            public String toVariableNameString(WorldBorder border) {
                return this.toString(border, 0);
            }
        }).defaultExpression(new EventValueExpression<WorldBorder>(WorldBorder.class)));
        Classes.registerClass(new ClassInfo<Pattern>(Pattern.class, "bannerpattern").user("banner ?patterns?").name("Banner Pattern").description("Represents a banner pattern.").since("2.10"));
        Registry patternRegistry = Bukkit.getRegistry(PatternType.class);
        if (patternRegistry != null) {
            patternTypeInfo = new RegistryClassInfo<PatternType>(PatternType.class, patternRegistry, "bannerpatterntype", "banner pattern types");
        } else {
            try {
                Class<?> patternClass = Class.forName("org.bukkit.block.banner.PatternType");
                if (!patternClass.isEnum()) {
                    throw new IllegalStateException("PatternType is neither an enum nor a valid registry.");
                }
                Class<?> enumClass = patternClass;
                patternTypeInfo = new EnumClassInfo(enumClass, "bannerpatterntype", "banner pattern types");
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        Classes.registerClass(patternTypeInfo.user("banner ?pattern ?types?").name("Banner Pattern Type").description("Represents the various banner patterns that can be applied to a banner.").since("2.10"));
        if (Skript.classExists("io.papermc.paper.entity.TeleportFlag")) {
            Classes.registerClass(new EnumClassInfo<SkriptTeleportFlag>(SkriptTeleportFlag.class, "teleportflag", "teleport flags").user("teleport ?flags?").name("Teleport Flag").description("Teleport Flags are settings to retain during a teleport.").since("2.10"));
        }
        Classes.registerClass(new ClassInfo<Vehicle>(Vehicle.class, "vehicle").user("vehicles?").name("Vehicle").description("Represents a vehicle.").since("2.10.2").changer(new EntityClassInfo.EntityChanger()));
        Classes.registerClass(new EnumClassInfo<EquipmentSlot>(EquipmentSlot.class, "equipmentslot", "equipment slots").user("equipment ?slots?").name("Equipment Slot").description("Represents an equipment slot of an entity.").since("2.11"));
        Classes.registerClass(new EnumClassInfo<VillagerCareerChangeEvent.ChangeReason>(VillagerCareerChangeEvent.ChangeReason.class, "villagercareerchangereason", "villager career change reasons").user("(villager )?career ?change ?reasons?").name("Villager Career Change Reason").description("Represents a reason why a villager changed its career.").since("2.12"));
    }
}

