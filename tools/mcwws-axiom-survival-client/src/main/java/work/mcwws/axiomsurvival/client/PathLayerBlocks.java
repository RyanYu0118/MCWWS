package work.mcwws.axiomsurvival.client;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import work.mcwws.axiomsurvival.client.mixin.EditorUIActiveBlockAccessor;

/** 钢笔图层的选块读写（走 Axiom EditorUI ActiveBlockHistory）。 */
public final class PathLayerBlocks {

    private PathLayerBlocks() {
    }

    public static CustomBlockState defaultBlock() {
        return ServerCustomBlocks.getCustomOrVanillaStateFor(Blocks.STONE.defaultBlockState());
    }

    public static CustomBlockState currentActive() {
        ActiveBlockHistory history = EditorUIActiveBlockAccessor.mcwws$activeBlockHistory();
        if (history == null) {
            return defaultBlock();
        }
        CustomBlockState active = history.getActive();
        return active != null ? active : defaultBlock();
    }

    public static void setActive(CustomBlockState state) {
        if (state == null) {
            state = defaultBlock();
        }
        ActiveBlockHistory history = EditorUIActiveBlockAccessor.mcwws$activeBlockHistory();
        if (history != null) {
            history.setActive(state);
        }
    }

    public static String serialize(CustomBlockState state) {
        if (state == null) {
            return "";
        }
        try {
            return ServerCustomBlocks.serialize(state);
        } catch (Exception e) {
            BlockState vanilla = state.getVanillaState();
            return vanilla != null ? vanilla.toString() : "";
        }
    }

    public static CustomBlockState deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultBlock();
        }
        try {
            CustomBlockState state = ServerCustomBlocks.deserialize(raw);
            return state != null ? state : defaultBlock();
        } catch (Exception e) {
            return defaultBlock();
        }
    }
}
