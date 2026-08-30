package work.mcwws.immersivecreative.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.immersivecreative.client.ImmersiveCreativeClient;

/**
 * 中键复制被 {@code mouseClicked} 里的 {@code hasInfiniteMaterials()} 门控住，生存下恒为 false，
 * 于是中键连 {@code ContainerInput.CLONE} 都走不到就被当作无效按键丢弃。只在创造栏界面里放行，
 * 其余容器（箱子、工作台等）保持原版生存行为。
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Redirect(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"
            )
    )
    private boolean mcwws$allowCloneInCreativeScreen(LocalPlayer player) {
        if (ImmersiveCreativeClient.isEnabled() && (Object) this instanceof CreativeModeInventoryScreen) {
            return true;
        }
        return player.hasInfiniteMaterials();
    }
}
