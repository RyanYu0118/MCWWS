package work.mcwws.axiomsurvival.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

final class SurvivalEditorNetworking {

    static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("mcwws", "axiom_survival");
    private static final byte OP_ENTER = 1;
    private static final byte OP_EXIT = 0;
    private static boolean registered;

    private SurvivalEditorNetworking() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.playS2C().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorStatePayload.TYPE, EditorStatePayload.CODEC);
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

    private record HelloPayload() implements CustomPacketPayload {

        static final CustomPacketPayload.Type<HelloPayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL);

        static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, HelloPayload> CODEC =
                net.minecraft.network.codec.StreamCodec.of(
                        (payload, buf) -> { },
                        buf -> decodeHello(buf)
                );

        private static HelloPayload decodeHello(FriendlyByteBuf buf) {
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

        static final net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, EditorStatePayload> CODEC =
                net.minecraft.network.codec.StreamCodec.of(
                        (payload, buf) -> buf.writeByte(payload.op),
                        buf -> new EditorStatePayload(buf.readByte())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
