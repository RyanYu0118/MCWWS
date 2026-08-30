package work.mcwws.immersivecreative.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.immersivecreative.client.ImmersiveCreativeClient;
import work.mcwws.immersivecreative.client.ImmersiveCreativeNetworking;

/**
 * 原版这两个方法发出的 {@code ServerboundSetCreativeModeSlotPacket} 在生存玩家身上会被服务端直接丢弃，
 * 所以沉浸式创造开启时改由自建通道上报，并取消原版发包，避免服务端两条路径重复处理。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"), cancellable = true)
    private void mcwws$reportSlot(ItemStack stack, int slot, CallbackInfo ci) {
        if (ImmersiveCreativeClient.isEnabled()) {
            ImmersiveCreativeNetworking.sendSlot(slot, stack);
            ci.cancel();
        }
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"), cancellable = true)
    private void mcwws$reportDrop(ItemStack stack, CallbackInfo ci) {
        if (ImmersiveCreativeClient.isEnabled()) {
            ImmersiveCreativeNetworking.sendSlot(-1, stack);
            ci.cancel();
        }
    }
}
