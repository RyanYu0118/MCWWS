package work.mcwws.immersivecreative.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mcwws.immersivecreative.client.ImmersiveCreativeClient;
import work.mcwws.immersivecreative.client.ImmersiveCreativeNetworking;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    @Shadow
    private Slot destroyItemSlot;

    @Shadow
    @Final
    static SimpleContainer CONTAINER;

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

    /**
     * 点 ❌ 不销毁：光标物品退回它被拿出的那个背包槽；来自分类页的则留在光标上。
     * 数字键 1～9、右键、中键全部放行，由 RETURN 注入负责上报计费。
     */
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void mcwws$blockDestroySlot(Slot slot, int slotId, int buttonNum,
                                        ContainerInput containerInput, CallbackInfo ci) {
        if (!ImmersiveCreativeClient.isEnabled()) {
            return;
        }
        rememberSource(slot);
        if (slot != null && slot == destroyItemSlot) {
            restoreCarriedToSource();
            ci.cancel();
        }
    }

    /**
     * 创造物品列表里的取放、数字键塞快捷栏、中键拿一组，有的根本不走
     * {@code handleCreativeModeItemAdd}。每次点击结束后把变更补报给服务端。
     */
    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void mcwws$syncAfterClick(Slot slot, int slotId, int buttonNum,
                                      ContainerInput containerInput, CallbackInfo ci) {
        if (!ImmersiveCreativeClient.isEnabled()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && containerInput == ContainerInput.SWAP
                && buttonNum >= 0 && buttonNum < 9) {
            ItemStack hotbar = player.getInventory().getItem(buttonNum);
            ImmersiveCreativeNetworking.sendSlot(36 + buttonNum, hotbar);
        }
        ImmersiveCreativeNetworking.sendCarriedOnly();
    }

    /** 记录光标即将拿走的来源，供 ❌ 退回。 */
    private void rememberSource(Slot slot) {
        if (slot == null || slot == destroyItemSlot) {
            return;
        }
        ItemStack carried = ImmersiveCreativeNetworking.currentCarried();
        if (isCatalogSlot(slot)) {
            if (carried.isEmpty() && slot.hasItem()) {
                ImmersiveCreativeClient.setLastSourceCatalog();
            }
            return;
        }
        if (carried.isEmpty() && slot.hasItem()) {
            int protocol = protocolSlot(slot);
            if (protocol >= 0) {
                ImmersiveCreativeClient.setLastSourceSlot(protocol);
            }
        }
    }

    /**
     * 把光标物品放回上次拿出的背包槽；槽位被占了就找空位；实在没地儿就留在光标上。
     */
    private void restoreCarriedToSource() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack carried = ImmersiveCreativeNetworking.currentCarried();
        if (carried.isEmpty()) {
            return;
        }
        int dest = ImmersiveCreativeClient.lastSourceSlot();
        if (dest == ImmersiveCreativeClient.SOURCE_CATALOG
                || dest == ImmersiveCreativeClient.SOURCE_NONE) {
            return;
        }
        AbstractContainerMenu menu = player.inventoryMenu;
        if (!canPlace(menu, dest, carried)) {
            dest = firstEmptyProtocolSlot(menu);
        }
        if (dest < 0) {
            return;
        }
        ItemStack toPlace = carried.copy();
        Slot target = menu.getSlot(dest);
        if (target == null) {
            return;
        }
        ItemStack existing = target.getItem();
        if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toPlace)
                && existing.getCount() + toPlace.getCount() <= existing.getMaxStackSize()) {
            existing.grow(toPlace.getCount());
            target.set(existing);
        } else {
            target.set(toPlace);
        }
        menu.setCarried(ItemStack.EMPTY);
        ImmersiveCreativeNetworking.sendSlot(dest, target.getItem(), ItemStack.EMPTY);
        ImmersiveCreativeClient.clearLastSourceSlot();
    }

    private static boolean canPlace(AbstractContainerMenu menu, int protocol, ItemStack stack) {
        if (protocol < 0 || protocol >= menu.slots.size()) {
            return false;
        }
        Slot slot = menu.getSlot(protocol);
        if (slot == null) {
            return false;
        }
        ItemStack existing = slot.getItem();
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(existing, stack)
                && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
    }

    private static int firstEmptyProtocolSlot(AbstractContainerMenu menu) {
        int[] order = new int[37];
        int n = 0;
        for (int i = 9; i <= 35; i++) {
            order[n++] = i;
        }
        for (int i = 36; i <= 44; i++) {
            order[n++] = i;
        }
        order[n++] = 45;
        for (int i = 0; i < n; i++) {
            int protocol = order[i];
            if (protocol < menu.slots.size() && menu.getSlot(protocol).getItem().isEmpty()) {
                return protocol;
            }
        }
        return -1;
    }

    private boolean isCatalogSlot(Slot slot) {
        return slot != null && slot.container == CONTAINER;
    }

    /** 分类页槽位没有对应的生存背包格；背包/盔甲/副手走原版菜单槽位号。 */
    private static int protocolSlot(Slot slot) {
        if (slot instanceof SlotWrapperAccessor wrapper) {
            Slot target = wrapper.mcwws$target();
            return target == null ? -1 : target.index;
        }
        int index = slot.index;
        if (index >= 45 && index <= 53) {
            return 36 + (index - 45);
        }
        return index;
    }
}
