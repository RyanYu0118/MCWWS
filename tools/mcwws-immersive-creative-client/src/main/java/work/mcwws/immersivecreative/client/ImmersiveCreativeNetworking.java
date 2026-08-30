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
    /** 槽位新内容 + 操作后的光标内容。缺了光标服务端只能靠额度池猜，会漏扣费。 */
    private static final byte OP_SLOT_WITH_CURSOR = 3;
    /** 只同步光标、不动任何槽位。 */
    public static final int SLOT_CURSOR_ONLY = -2;
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
     * 所以槽位变更走自建通道直接送到插件。{@code slot} 为 -1 表示丢弃，-2 表示只同步光标。
     * <p>
     * 光标必须一起发：创造物品列表里的取放全是客户端本地改 carried，服务端看不到任何槽位变化，
     * 只有拿到光标才能把「挪位置」和「凭空多出一份」区分开。
     */
    public static void sendSlot(int slot, ItemStack stack) {
        sendSlot(slot, stack, currentCarried());
    }

    public static void sendSlot(int slot, ItemStack stack, ItemStack carried) {
        String nbt = encode(stack);
        String carriedNbt = encode(carried);
        if (nbt == null || carriedNbt == null) {
            return;
        }
        try {
            ClientPlayNetworking.send(new SlotPayload(slot, nbt, carriedNbt));
        } catch (Exception ex) {
            McwwsImmersiveCreativeClientMod.LOGGER.warn("发送创造槽位失败 slot={}", slot, ex);
        }
    }

    /** 只把当前光标同步给服务端，用于创造列表里那些不碰任何槽位的取放。 */
    public static void sendCarriedOnly() {
        sendSlot(SLOT_CURSOR_ONLY, ItemStack.EMPTY, currentCarried());
    }

    /** {@code ItemPickerMenu.getCarried()} 直接委托给 inventoryMenu，这里取的就是同一份。 */
    public static ItemStack currentCarried() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        return client.player.inventoryMenu.getCarried();
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
    private record SlotPayload(int slot, String nbt, String carriedNbt) implements CustomPacketPayload {

        static final CustomPacketPayload.Type<SlotPayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final StreamCodec<RegistryFriendlyByteBuf, SlotPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeByte(OP_SLOT_WITH_CURSOR);
                            buf.writeInt(payload.slot());
                            writeString(buf, payload.nbt());
                            writeString(buf, payload.carriedNbt());
                        },
                        buf -> {
                            buf.readByte();
                            int slot = buf.readInt();
                            return new SlotPayload(slot, readString(buf), readString(buf));
                        }
                );

        private static void writeString(RegistryFriendlyByteBuf buf, String value) {
            byte[] data = value.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(data.length);
            buf.writeBytes(data);
        }

        private static String readString(RegistryFriendlyByteBuf buf) {
            byte[] data = new byte[buf.readInt()];
            buf.readBytes(data);
            return new String(data, StandardCharsets.UTF_8);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
