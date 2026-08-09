package work.mcwws.axiomsurvival;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

final class PacketBufs {

    private PacketBufs() {
    }

    static int readerIndex(Object buf) throws ReflectiveOperationException {
        return (int) buf.getClass().getMethod("readerIndex").invoke(buf);
    }

    static void readerIndex(Object buf, int index) throws ReflectiveOperationException {
        buf.getClass().getMethod("readerIndex", int.class).invoke(buf, index);
    }

    static byte[] copyReadable(Object buf) throws ReflectiveOperationException {
        int mark = readerIndex(buf);
        int length = (int) buf.getClass().getMethod("readableBytes").invoke(buf);
        byte[] bytes = new byte[length];
        buf.getClass().getMethod("readBytes", byte[].class).invoke(buf, (Object) bytes);
        readerIndex(buf, mark);
        return bytes;
    }

    static Object fromBytes(Player player, byte[] bytes) throws ReflectiveOperationException {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object registryAccess = handle.getClass().getMethod("registryAccess").invoke(handle);
        Class<?> bufClass = Class.forName("net.minecraft.network.RegistryFriendlyByteBuf");
        Class<?> unpooledClass = Class.forName("io.netty.buffer.Unpooled");
        Object wrapped = unpooledClass.getMethod("wrappedBuffer", byte[].class).invoke(null, (Object) bytes);
        Constructor<?> ctor = bufClass.getConstructor(
                Class.forName("io.netty.buffer.ByteBuf"),
                Class.forName("net.minecraft.core.RegistryAccess")
        );
        return ctor.newInstance(wrapped, registryAccess);
    }
}
