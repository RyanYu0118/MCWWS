package work.mcwws.multitoolfix;

import me.darkolythe.multitool.Multitool;
import me.darkolythe.multitool.MultitoolToolDetect;
import me.darkolythe.multitool.ToolMap;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * MultitoolPlus 的 ToolMap 停在 1.19 左右，樱桃木、竹制品、苍白橡木等不会切到正确工具。
 * 用原版 mineable 标签补缺，已有条目不覆盖。
 */
final class ToolMapPatcher {

    static final int SLOT_PICKAXE = 1;
    static final int SLOT_AXE = 2;
    static final int SLOT_SHOVEL = 3;

    private ToolMapPatcher() {
    }

    static int patch(Multitool multitool) {
        Map<String, Integer> map = toolMap(multitool);
        int before = map.size();
        putAll(map, Tag.MINEABLE_PICKAXE, SLOT_PICKAXE);
        putAll(map, Tag.MINEABLE_AXE, SLOT_AXE);
        putAll(map, Tag.MINEABLE_SHOVEL, SLOT_SHOVEL);
        return map.size() - before;
    }

    static Map<String, Integer> toolMap(Multitool multitool) {
        try {
            Field field = MultitoolToolDetect.class.getDeclaredField("map");
            field.setAccessible(true);
            ToolMap toolMap = (ToolMap) field.get(multitool.multitooltooldetect);
            return toolMap.map;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取 MultitoolPlus ToolMap", e);
        }
    }

    private static void putAll(Map<String, Integer> map, Tag<Material> tag, int slot) {
        for (Material material : tag.getValues()) {
            map.putIfAbsent(material.name(), slot);
        }
    }
}
