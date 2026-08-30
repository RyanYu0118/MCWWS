package work.mcwws.immersivecreative.client.mixin;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.immersivecreative.client.ImmersiveCreativeClient;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    /**
     * 生存模式下 {@code init} 会因 {@code hasInfiniteMaterials()==false} 立刻切回
     * {@code InventoryScreen}，而那边的 mixin 又会再打开创造栏，形成死循环崩溃。
     * {@code containerTick} 同样每 tick 检查，必须一起谎报。
     */
    @Redirect(
            method = {"init", "containerTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"
            )
    )
    private boolean mcwws$keepCreativeInventory(LocalPlayer player) {
        if (ImmersiveCreativeClient.isEnabled()) {
            return true;
        }
        return player.hasInfiniteMaterials();
    }
}
