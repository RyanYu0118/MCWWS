package work.mcwws.immersivecreative.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;

public final class ImmersiveCreativeNetworking {

    static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("mcwws", "immersive_creative");
    /** 只带材质 + 数量的旧协议，会让服务端重建物品从而抹掉附魔和 Slimefun 数据，已废弃。 */
    private static final byte OP_SLOT_NBT = 2;
    private static boolean registered;

    private ImmersiveCreativeNetworking() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SlotPayload.TYPE, SlotPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ImmersiveCreativeClient.setEnabled(payload.enabled()))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ImmersiveCreativeClient.setEnabled(false)
        );
    }

    /**
     * 服务端玩家始终是生存，原版的创造槽位包会被丢弃，ProtocolLib 在 26.2 上也拦不到，
     * 所以槽位变更走自建通道直接送到插件。{@code slot} 为 -1 表示丢弃/销毁。
     */
    public static void sendSlot(int slot, ItemStack stack) {
        String nbt = encode(stack);
        if (nbt == null) {
            return;
        }
        try {
            ClientPlayNetworking.send(new SlotPayload(slot, nbt));
        } catch (Exception ex) {
            McwwsImmersiveCreativeClientMod.LOGGER.warn("发送创造槽位失败 slot={}", slot, ex);
        }
    }

    /**
     * 用原版 {@code ItemStack.CODEC} 序列化成 SNBT，服务端再原样还原。
     * 只传材质会把附魔、Slimefun 的 PDC 等组件全部丢掉，绝不能那么干。
     * 返回空串表示空槽位；返回 {@code null} 表示编码失败，调用方应放弃本次上报。
     */
    private static String encode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        try {
            RegistryOps<Tag> ops = client.level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return ItemStack.CODEC.encodeStart(ops, stack).getOrThrow().toString();
        } catch (Exception ex) {
            McwwsImmersiveCreativeClientMod.LOGGER.warn("序列化创造槽位物品失败", ex);
            return null;
        }
    }

    private record StatePayload(boolean enabled) implements CustomPacketPayload {

        static final CustomPacketPayload.Type<StatePayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeByte(payload.enabled() ? 1 : 0),
                        buf -> {
                            boolean enabled = false;
                            if (buf.readableBytes() > 0) {
                                enabled = buf.readByte() != 0;
                            }
                            return new StatePayload(enabled);
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 编码与服务端 {@code ImmersiveChannel} 手写的 DataInputStream 解析一一对应。 */
    private record SlotPayload(int slot, String nbt) implements CustomPacketPayload {

        static final CustomPacketPayload.Type<SlotPayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final StreamCodec<RegistryFriendlyByteBuf, SlotPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            byte[] data = payload.nbt().getBytes(StandardCharsets.UTF_8);
                            buf.writeByte(OP_SLOT_NBT);
                            buf.writeInt(payload.slot());
                            buf.writeInt(data.length);
                            buf.writeBytes(data);
                        },
                        buf -> {
                            buf.readByte();
                            int slot = buf.readInt();
                            byte[] data = new byte[buf.readInt()];
                            buf.readBytes(data);
                            return new SlotPayload(slot, new String(data, StandardCharsets.UTF_8));
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
