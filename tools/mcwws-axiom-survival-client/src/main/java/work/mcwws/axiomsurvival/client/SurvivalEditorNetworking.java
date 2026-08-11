package work.mcwws.axiomsurvival.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

final class SurvivalEditorNetworking {

    static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("mcwws", "axiom_survival");
    private static final byte OP_ENTER = 1;
    private static final byte OP_EXIT = 0;
    private static final byte OP_MENU_OPEN = 2;
    private static final byte OP_MENU_CLOSE = 3;
    private static boolean registered;

    private SurvivalEditorNetworking() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.clientboundPlay().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EditorStatePayload.TYPE, EditorStatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(HelloPayload.TYPE, (payload, context) ->
                context.client().execute(SurvivalEditorController::markSupportedFromHello)
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SurvivalEditorController.setServerSupported(false);
            if (SurvivalEditorController.isLocalEditorActive()) {
                SurvivalEditorController.onEditorExit();
            }
        });
    }

    static void sendEditorState(boolean enter) {
        if (!SurvivalEditorController.isServerSupported()) {
            return;
        }
        if (!ClientPlayNetworking.canSend(EditorStatePayload.TYPE)) {
            McwwsAxiomSurvivalClientMod.LOGGER.warn("无法发送 Editor 状态：通道未就绪");
            return;
        }
        ClientPlayNetworking.send(new EditorStatePayload(enter ? OP_ENTER : OP_EXIT));
    }

    /** 请求服务端在开菜单时快照位置、关菜单时传送回该位置 */
    static void sendMenuState(boolean open) {
        if (!SurvivalEditorController.isServerSupported()
                || !ClientPlayNetworking.canSend(EditorStatePayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(new EditorStatePayload(open ? OP_MENU_OPEN : OP_MENU_CLOSE));
    }

    private record HelloPayload() implements CustomPacketPayload {

        static final CustomPacketPayload.Type<HelloPayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> { },
                        buf -> decodeHello(buf)
                );

        private static HelloPayload decodeHello(RegistryFriendlyByteBuf buf) {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if ("hello".equals(text)) {
                return new HelloPayload();
            }
            throw new IllegalArgumentException("未知 mcwws:axiom_survival S2C 载荷: " + text);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private record EditorStatePayload(byte op) implements CustomPacketPayload {

        static final CustomPacketPayload.Type<EditorStatePayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final StreamCodec<RegistryFriendlyByteBuf, EditorStatePayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeByte(payload.op()),
                        buf -> new EditorStatePayload(buf.readByte())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
