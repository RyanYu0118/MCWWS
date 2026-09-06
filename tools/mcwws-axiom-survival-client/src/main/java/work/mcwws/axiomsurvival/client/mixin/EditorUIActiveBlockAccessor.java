package work.mcwws.axiomsurvival.client.mixin;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EditorUI.class, remap = false)
public interface EditorUIActiveBlockAccessor {

    @Accessor(value = "activeBlockHistory", remap = false)
    static ActiveBlockHistory mcwws$activeBlockHistory() {
        throw new AssertionError();
    }
}
