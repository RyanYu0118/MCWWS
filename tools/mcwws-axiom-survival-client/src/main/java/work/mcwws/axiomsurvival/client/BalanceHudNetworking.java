package work.mcwws.axiomsurvival.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

/**
 * 余额变动提示通道 {@code mcwws:balance_hud}。
 *
 * <p>注册这个接收器同时也告诉服务端「本客户端能画左下角浮层」，
 * 服务端据此决定是推浮层还是回退到 action bar。
 */
final class BalanceHudNetworking {

    static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("mcwws", "balance_hud");
    private static final Identifier HUD_ELEMENT = Identifier.fromNamespaceAndPath("mcwws", "balance_hud");
    private static boolean registered;

    private BalanceHudNetworking() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.clientboundPlay().register(BalanceHudPayload.TYPE, BalanceHudPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BalanceHudPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        BalanceHudOverlay.push(payload.key(), payload.text(), payload.replace()))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BalanceHudOverlay.clear());
        // 挂在聊天层之后：绘制顺序在聊天之上，压住聊天时靠底衬保持可读
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, HUD_ELEMENT, new BalanceHudOverlay());
    }

    private record BalanceHudPayload(boolean replace, String key, String text) implements CustomPacketPayload {

        static final CustomPacketPayload.Type<BalanceHudPayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final StreamCodec<RegistryFriendlyByteBuf, BalanceHudPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeBytes(encode(payload)),
                        BalanceHudPayload::decode
                );

        private static byte[] encode(BalanceHudPayload payload) {
            String raw = (payload.replace() ? "1" : "0") + "\u0000" + payload.key() + "\u0000" + payload.text();
            return raw.getBytes(StandardCharsets.UTF_8);
        }

        private static BalanceHudPayload decode(RegistryFriendlyByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String[] parts = new String(bytes, StandardCharsets.UTF_8).split("\u0000", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("未知 mcwws:balance_hud 载荷");
            }
            return new BalanceHudPayload("1".equals(parts[0]), parts[1], parts[2]);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
