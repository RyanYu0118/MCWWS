package work.mcwws.immersivecreative.client.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.immersivecreative.client.ImmersiveCreativeClient;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Redirect(
            method = {"init", "containerTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"
            )
    )
    private boolean mcwws$openCreativeInventory(LocalPlayer player) {
        if (ImmersiveCreativeClient.isEnabled()) {
            return true;
        }
        return player.hasInfiniteMaterials();
    }
}
