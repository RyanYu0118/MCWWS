package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 建造阶段本地模式是创造，{@code instabuild} 必须保持 true，否则 Axiom 工具菜单会立刻关掉。
 * E 键打开的 {@code InventoryScreen} 也看 {@code hasInfiniteMaterials()}，这里单独谎报 false，
 * 这样按 E 仍是生存背包，不影响工具菜单。
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenSurvivalMixin {

    @Redirect(
            method = {"init", "containerTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"
            )
    )
    private boolean mcwws$keepSurvivalInventory(LocalPlayer player) {
        if (SurvivalEditorController.shouldShowSurvivalHud()) {
            return false;
        }
        return player.hasInfiniteMaterials();
    }
}
