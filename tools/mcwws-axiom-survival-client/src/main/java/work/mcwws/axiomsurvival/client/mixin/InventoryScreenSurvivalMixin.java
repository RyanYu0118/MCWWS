package work.mcwws.axiomsurvival.client.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import work.mcwws.axiomsurvival.client.SurvivalEditorController;

/**
 * 建造阶段本地模式是创造，{@code instabuild} 必须保持 true，否则 Axiom 工具菜单会立刻关掉。
 * E 键打开的 {@code InventoryScreen} 也看 {@code hasInfiniteMaterials()}：
 * 编辑界面打开时谎报 false，避免抢掉 Axiom 工具菜单；
 * 建造阶段若沉浸式创造已开启则谎报 true，否则仍谎报 false 以保持生存背包。
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
        if (SurvivalEditorController.shouldKeepAxiomMenuInventory()) {
            return false;
        }
        if (mcwws$immersiveCreativeEnabled()) {
            return true;
        }
        if (SurvivalEditorController.shouldShowSurvivalHud()) {
            return false;
        }
        return player.hasInfiniteMaterials();
    }

    private static boolean mcwws$immersiveCreativeEnabled() {
        try {
            Class<?> type = Class.forName("work.mcwws.immersivecreative.client.ImmersiveCreativeClient");
            Object value = type.getMethod("isEnabled").invoke(null);
            return Boolean.TRUE.equals(value);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
