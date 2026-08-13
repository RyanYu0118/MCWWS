package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 服务端下发的 abilities 是飞行权限的唯一权威来源，记一份供 Editor 建造阶段压回本地创造污染的 mayfly。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerAbilitiesMixin {

    @Inject(method = "handlePlayerAbilities", at = @At("TAIL"))
    private void mcwws$noteServerAbilities(ClientboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        SurvivalEditorController.noteServerAbilities(packet.canFly(), packet.isFlying());
    }
}
