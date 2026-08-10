package work.mcwws.axiomsurvival;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.lang.reflect.Method;

final class NmsBlocks {

    private static Method craftBlockDataFromNms;

    private NmsBlocks() {
    }

    static Material toMaterial(Object nmsBlockState) {
        if (nmsBlockState == null) {
            return Material.AIR;
        }
        try {
            if (isAirState(nmsBlockState)) {
                return Material.AIR;
            }
            BlockData data = toBlockData(nmsBlockState);
            return data == null ? Material.AIR : data.getMaterial();
        } catch (ReflectiveOperationException ex) {
            return Material.AIR;
        }
    }

    static int blockPosX(Object blockPos) throws ReflectiveOperationException {
        return (int) blockPos.getClass().getMethod("getX").invoke(blockPos);
    }

    static int blockPosY(Object blockPos) throws ReflectiveOperationException {
        return (int) blockPos.getClass().getMethod("getY").invoke(blockPos);
    }

    static int blockPosZ(Object blockPos) throws ReflectiveOperationException {
        return (int) blockPos.getClass().getMethod("getZ").invoke(blockPos);
    }

    static BlockData toBlockData(Object nmsBlockState) throws ReflectiveOperationException {
        if (craftBlockDataFromNms == null) {
            Class<?> clazz = Class.forName("org.bukkit.craftbukkit.block.data.CraftBlockData");
            craftBlockDataFromNms = clazz.getMethod("fromBlockData", Class.forName("net.minecraft.world.level.block.state.BlockState"));
        }
        return (BlockData) craftBlockDataFromNms.invoke(null, nmsBlockState);
    }

    private static boolean isAirState(Object nmsBlockState) throws ReflectiveOperationException {
        Object air = Class.forName("com.moulberry.axiom.buffer.BlockBuffer")
                .getField("EMPTY_STATE")
                .get(null);
        return nmsBlockState.equals(air) || (boolean) nmsBlockState.getClass().getMethod("isAir").invoke(nmsBlockState);
    }
}
