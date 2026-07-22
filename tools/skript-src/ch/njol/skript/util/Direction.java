/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Directional
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.localization.GeneralWords;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import ch.njol.util.Kleenean;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Direction
implements YggdrasilSerializable.YggdrasilRobustSerializable {
    public static final Direction ZERO = new Direction(new double[]{0.0, 0.0, 0.0});
    public static final Direction IDENTITY = new Direction(0.0, 0.0, 1.0);
    public static final BlockFace BF_X = Direction.findFace(1, 0, 0);
    public static final BlockFace BF_Y = Direction.findFace(0, 1, 0);
    public static final BlockFace BF_Z = Direction.findFace(0, 0, 1);
    public static final Noun m_meter = new Noun("directions.meter");
    private final double pitchOrX;
    private final double yawOrY;
    private final double lengthOrZ;
    private final boolean relative;
    public static final double IGNORE_PITCH = 61863.0;
    private static final Message m_at = new Message("directions.at");
    private static final Message[] absoluteDirections = new Message[6];
    private static final Message[] relativeDirections = new Message[6];

    private static BlockFace findFace(int x, int y, int z) {
        for (BlockFace f : BlockFace.values()) {
            if (f.getModX() != x || f.getModY() != y || f.getModZ() != z) continue;
            return f;
        }
        assert (false);
        return BlockFace.SELF;
    }

    public Direction(double[] mod) {
        if (mod.length != 3) {
            throw new IllegalArgumentException();
        }
        this.relative = false;
        this.pitchOrX = mod[0];
        this.yawOrY = mod[1];
        this.lengthOrZ = mod[2];
    }

    public Direction() {
        this(0.0, 0.0, 0.0);
    }

    public Direction(double pitch, double yaw, double length) {
        this.relative = true;
        this.pitchOrX = pitch;
        this.yawOrY = yaw;
        this.lengthOrZ = length;
    }

    public Direction(BlockFace f, double length) {
        this(new Vector(f.getModX(), f.getModY(), f.getModZ()).normalize().multiply(length));
    }

    public Direction(Vector v) {
        this.relative = false;
        this.pitchOrX = v.getX();
        this.yawOrY = v.getY();
        this.lengthOrZ = v.getZ();
    }

    public Location getRelative(Location l) {
        return l.clone().add(this.getDirection(l));
    }

    public Location getRelative(Entity e) {
        return e.getLocation().add(this.getDirection(e.getLocation()));
    }

    public Location getRelative(Block b) {
        return b.getLocation().add(this.getDirection(b));
    }

    public Vector getDirection() {
        if (!this.relative) {
            return new Vector(this.pitchOrX, this.yawOrY, this.lengthOrZ);
        }
        return this.getDirection(0.0, 0.0);
    }

    public Vector getDirection(Location l) {
        if (!this.relative) {
            return new Vector(this.pitchOrX, this.yawOrY, this.lengthOrZ);
        }
        return this.getDirection(this.pitchOrX == 61863.0 ? 0.0 : Direction.pitchToRadians(l.getPitch()), Direction.yawToRadians(l.getYaw()));
    }

    public Vector getDirection(Entity e) {
        return this.getDirection(e.getLocation());
    }

    public Vector getDirection(Block b) {
        if (!this.relative) {
            return new Vector(this.pitchOrX, this.yawOrY, this.lengthOrZ);
        }
        BlockFace blockFace = Direction.getFacing(b);
        return this.getDirection(this.pitchOrX == 61863.0 ? 0.0 : (double)blockFace.getModZ() * Math.PI / 2.0, Math.atan2(blockFace.getModZ(), blockFace.getModX()));
    }

    private Vector getDirection(double p, double y) {
        if (this.pitchOrX == 61863.0) {
            return new Vector(Math.cos(y + this.yawOrY) * this.lengthOrZ, 0.0, Math.sin(y + this.yawOrY) * this.lengthOrZ);
        }
        double lxz = Math.cos(p + this.pitchOrX) * this.lengthOrZ;
        return new Vector(Math.cos(y + this.yawOrY) * lxz, Math.sin(p + this.pitchOrX) * Math.cos(this.yawOrY) * this.lengthOrZ, Math.sin(y + this.yawOrY) * lxz);
    }

    public int hashCode() {
        return (this.relative ? 1 : -1) * Arrays.hashCode(new double[]{this.pitchOrX, this.yawOrY, this.lengthOrZ});
    }

    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Direction)) {
            return false;
        }
        Direction other = (Direction)obj;
        return this.relative == other.relative && this.pitchOrX == other.pitchOrX && this.yawOrY == other.yawOrY && other.lengthOrZ == this.lengthOrZ;
    }

    public boolean isRelative() {
        return this.relative;
    }

    public static double pitchToRadians(float pitch) {
        return -Math.toRadians(pitch);
    }

    public static float getPitch(double pitch) {
        return (float)Math.toDegrees(-pitch);
    }

    public static double yawToRadians(float yaw) {
        return Math.toRadians(yaw) + 1.5707963267948966;
    }

    public static float getYaw(double yaw) {
        return (float)Math.toDegrees(yaw - 1.5707963267948966);
    }

    public static BlockFace getFacing(Block b) {
        BlockData blockData = b.getBlockData();
        if (!(blockData instanceof Directional)) {
            return BlockFace.SELF;
        }
        return ((Directional)blockData).getFacing();
    }

    public static BlockFace getFacing(double yaw, double pitch) {
        if (-0.7853981633974483 < pitch && pitch < 0.7853981633974483) {
            if (yaw < 0.7853981633974483 || yaw >= 5.497787143782138) {
                return BF_X;
            }
            if (yaw < 2.356194490192345) {
                return BF_Z;
            }
            if (yaw < 3.9269908169872414) {
                return BF_X.getOppositeFace();
            }
            assert (yaw < 5.497787143782138);
            return BF_Z.getOppositeFace();
        }
        if (pitch > 0.0) {
            return BlockFace.UP;
        }
        return BlockFace.DOWN;
    }

    public static BlockFace getFacing(Location l, boolean horizontal) {
        double yaw = (Direction.yawToRadians(l.getYaw()) + Math.PI * 2) % (Math.PI * 2);
        double pitch = horizontal ? 0.0 : Direction.pitchToRadians(l.getPitch());
        return Direction.getFacing(yaw, pitch);
    }

    public static BlockFace getFacing(Vector v, boolean horizontal) {
        double pitch = horizontal ? 0.0 : Math.atan2(v.getY(), Math.sqrt(Math.pow(v.getX(), 2.0) + Math.pow(v.getZ(), 2.0)));
        double yaw = Math.atan2(v.getZ(), v.getX());
        return Direction.getFacing(yaw, pitch);
    }

    public static Location[] getRelatives(Block[] blocks, Direction[] directions) {
        Location[] r = new Location[blocks.length * directions.length];
        if (r.length == 0) {
            return r;
        }
        for (int i = 0; i < blocks.length; ++i) {
            r[i] = blocks[i].getLocation();
            for (int j = 0; j < directions.length; ++j) {
                r[i].add(directions[j].getDirection(blocks[i]));
            }
        }
        return r;
    }

    public static Location[] getRelatives(Location[] locations, Direction[] directions) {
        Location[] r = new Location[locations.length * directions.length];
        if (r.length == 0) {
            return r;
        }
        for (int i = 0; i < locations.length; ++i) {
            r[i] = locations[i].clone();
            for (int j = 0; j < directions.length; ++j) {
                r[i].add(directions[j].getDirection(locations[i]));
            }
        }
        return r;
    }

    public static BlockFace toNearestBlockFace(Vector vector) {
        double maxDot = -1.0;
        BlockFace nearest = BlockFace.NORTH;
        for (BlockFace face : BlockFace.values()) {
            double dot = face.getDirection().dot(vector);
            if (!(dot > maxDot)) continue;
            maxDot = dot;
            nearest = face;
        }
        return nearest;
    }

    @Nullable
    public static BlockFace toNearestCartesianBlockFace(Vector vector) {
        double maxDot = -1.0;
        BlockFace nearest = null;
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
            double dot = face.getDirection().dot(vector);
            if (!(dot > maxDot)) continue;
            maxDot = dot;
            nearest = face;
        }
        return nearest;
    }

    public String toString() {
        return this.relative ? Direction.toString(this.pitchOrX == 61863.0 ? 0.0 : this.pitchOrX, this.yawOrY, this.lengthOrZ) : Direction.toString(new double[]{this.pitchOrX, this.yawOrY, this.lengthOrZ});
    }

    public static String toString(double pitch, double yaw, double length) {
        double front = Math.cos(pitch) * Math.cos(yaw) * length;
        double left = Math.cos(pitch) * Math.sin(yaw) * length;
        double above = Math.sin(pitch) * length;
        return Direction.toString(new double[]{front, left, above}, relativeDirections);
    }

    public static String toString(double[] mod) {
        if (mod[0] == 0.0 && mod[1] == 0.0 && mod[2] == 0.0) {
            return m_at.toString();
        }
        return Direction.toString(mod, absoluteDirections);
    }

    public static String toString(Vector dir) {
        if (dir.getX() == 0.0 && dir.getY() == 0.0 && dir.getZ() == 0.0) {
            return Language.get("directions.at");
        }
        return Direction.toString(new double[]{dir.getX(), dir.getY(), dir.getZ()}, absoluteDirections);
    }

    private static String toString(double[] mod, Message[] names) {
        assert (mod.length == 3 && names.length == 6);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            Direction.toString(b, mod[i], names[2 * i], names[2 * i + 1], b.length() != 0);
        }
        return b.toString();
    }

    private static void toString(StringBuilder b, double d, Message direction, Message oppositeDirection, boolean prependAnd) {
        if (d == 0.0) {
            return;
        }
        if (prependAnd) {
            b.append(" ").append(GeneralWords.and).append(" ");
        }
        if (d != 1.0 && d != -1.0) {
            b.append(m_meter.withAmount(Math.abs(d)));
            b.append(" ");
        }
        b.append(d > 0.0 ? direction : oppositeDirection);
    }

    @Deprecated(since="2.3.0", forRemoval=true)
    @Nullable
    public static Direction deserialize(String s) {
        String[] split = s.split(":");
        if (split.length != 2) {
            return null;
        }
        boolean relative = Boolean.parseBoolean(split[0]);
        if (relative) {
            String[] split2 = split[1].split(",");
            if (split2.length != 3) {
                return null;
            }
            try {
                return new Direction(Double.parseDouble(split2[0]), Double.parseDouble(split2[1]), Double.parseDouble(split2[2]));
            }
            catch (NumberFormatException e) {
                return null;
            }
        }
        String[] split2 = split[1].split(",");
        if (split2.length != 3) {
            return null;
        }
        try {
            return new Direction(new double[]{Double.parseDouble(split2[0]), Double.parseDouble(split2[1]), Double.parseDouble(split2[2])});
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    public static Expression<Location> combine(final @Nullable Expression<? extends Direction> dirs, final @Nullable Expression<? extends Location> locs) {
        if (dirs == null || locs == null) {
            return null;
        }
        return new SimpleExpression<Location>(){

            protected Location[] get(Event e) {
                Location[] ls;
                Direction[] ds = (Direction[])dirs.getArray(e);
                Location[] r = ls = (Location[])locs.getArray(e);
                for (int i = 0; i < ds.length; ++i) {
                    for (int j = 0; j < ls.length; ++j) {
                        r[j] = ds[i].getRelative(r[j]);
                    }
                }
                return r;
            }

            public Location[] getAll(Event e) {
                Location[] ls;
                Direction[] ds = (Direction[])dirs.getAll(e);
                Location[] r = ls = (Location[])locs.getAll(e);
                for (int i = 0; i < ds.length; ++i) {
                    for (int j = 0; j < ls.length; ++j) {
                        r[j] = ds[i].getRelative(r[j]);
                    }
                }
                return r;
            }

            @Override
            public boolean getAnd() {
                return locs.getAnd();
            }

            @Override
            public boolean isSingle() {
                return locs.isSingle();
            }

            @Override
            public Class<? extends Location> getReturnType() {
                return Location.class;
            }

            @Override
            public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString(@Nullable Event e, boolean debug) {
                return dirs.toString(e, debug) + " " + locs.toString(e, debug);
            }

            @Override
            public Expression<? extends Location> simplify() {
                if (dirs instanceof Literal && dirs.isSingle() && ZERO.equals(((Literal)dirs).getSingle())) {
                    return new SimpleExpression<Location>(this){

                        @Override
                        public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
                            throw new UnsupportedOperationException();
                        }

                        protected Location @Nullable [] get(Event event) {
                            return (Location[])locs.getAll(event);
                        }

                        @Override
                        public boolean getAnd() {
                            return locs.getAnd();
                        }

                        @Override
                        public boolean isSingle() {
                            return locs.isSingle();
                        }

                        @Override
                        public Class<? extends Location> getReturnType() {
                            return Location.class;
                        }

                        @Override
                        public String toString(@Nullable Event event, boolean debug) {
                            return "at " + locs.toString(event, debug);
                        }
                    };
                }
                return this;
            }
        };
    }

    @Override
    public boolean incompatibleField(@NotNull Field f, @NotNull Fields.FieldContext value) throws StreamCorruptedException {
        return false;
    }

    private void set(String field, @Nullable Double value) throws StreamCorruptedException {
        block7: {
            if (value == null) {
                throw new StreamCorruptedException();
            }
            try {
                Field f = Direction.class.getDeclaredField(field);
                f.setAccessible(true);
                f.set(this, value);
            }
            catch (IllegalArgumentException e) {
                assert (false) : e;
            }
            catch (IllegalAccessException e) {
                assert (false) : e;
            }
            catch (NoSuchFieldException e) {
                if ($assertionsDisabled) break block7;
                throw new AssertionError((Object)e);
            }
        }
    }

    @Override
    public boolean excessiveField(@NotNull Fields.FieldContext field) throws StreamCorruptedException {
        if (field.getID().equals("mod")) {
            double[] mod = field.getObject(double[].class);
            if (mod == null) {
                return true;
            }
            if (mod.length != 3) {
                throw new StreamCorruptedException();
            }
            this.set("pitchOrX", mod[0]);
            this.set("yawOrY", mod[1]);
            this.set("lengthOrZ", mod[1]);
            return true;
        }
        if (field.getID().equals("pitch")) {
            this.set("pitchOrX", field.getPrimitive(Double.TYPE));
            return true;
        }
        if (field.getID().equals("yaw")) {
            this.set("yawOrY", field.getPrimitive(Double.TYPE));
            return true;
        }
        if (field.getID().equals("length")) {
            this.set("lengthOrZ", field.getPrimitive(Double.TYPE));
            return true;
        }
        return false;
    }

    @Override
    public boolean missingField(@NotNull Field field) throws StreamCorruptedException {
        return !field.getName().equals("relative");
    }

    static {
        String[] rd = new String[]{"front", "behind", "left", "right", "above", "below"};
        for (int i = 0; i < rd.length; ++i) {
            Direction.relativeDirections[i] = new Message("directions." + rd[i]);
        }
        String[] ad = new String[]{BF_X.name().toLowerCase(Locale.ENGLISH), BF_X.getOppositeFace().name().toLowerCase(Locale.ENGLISH), BF_Y.name().toLowerCase(Locale.ENGLISH), BF_Y.getOppositeFace().name().toLowerCase(Locale.ENGLISH), BF_Z.name().toLowerCase(Locale.ENGLISH), BF_Z.getOppositeFace().name().toLowerCase(Locale.ENGLISH)};
        for (int i = 0; i < ad.length; ++i) {
            Direction.absoluteDirections[i] = new Message("directions." + ad[i]);
        }
    }
}

