package work.mcwws.immersivecreative.client;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ImmersiveMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * Axiom 生存配套模组对 {@code InventoryScreen} 有等价的 Redirect，两边同时注入会冲突。
     * 只能跳过这一个 mixin：{@code CreativeModeInventoryScreenMixin} 必须始终生效，
     * 否则创造栏会因 {@code hasInfiniteMaterials()==false} 切回背包并与背包互相打开，栈溢出崩溃。
     */
    private static final String INVENTORY_SCREEN_MIXIN =
            "work.mcwws.immersivecreative.client.mixin.InventoryScreenMixin";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (INVENTORY_SCREEN_MIXIN.equals(mixinClassName)) {
            return !FabricLoader.getInstance().isModLoaded("mcwws_axiom_survival_client");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
