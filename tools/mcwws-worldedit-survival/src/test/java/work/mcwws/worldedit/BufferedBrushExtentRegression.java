package work.mcwws.worldedit;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Standalone regression checks against the installed FAWE Extent API.
 * No server/world or player balances are modified.
 */
public final class BufferedBrushExtentRegression {
    private static final BaseBlock DIRT = new TestBlock("dirt");
    private static final BaseBlock COARSE = new TestBlock("coarse_dirt");
    private static final BaseBlock ROOTED = new TestBlock("rooted_dirt");
    private static final BlockVector3 A = BlockVector3.at(1, 64, 1);
    private static final BlockVector3 B = BlockVector3.at(2, 64, 1);

    private static final class TestBlock extends BaseBlock {
        private final String name;
        TestBlock(String name) { super((BlockState) null); this.name = name; }
        @Override public boolean equals(Object other) {
            return other instanceof TestBlock test && name.equals(test.name);
        }
        @Override public int hashCode() { return name.hashCode(); }
    }

    private static final class WorldStub {
        final Map<BlockVector3, BaseBlock> blocks = new HashMap<>();
        int writes;
        final Extent extent = (Extent) Proxy.newProxyInstance(Extent.class.getClassLoader(), new Class<?>[]{Extent.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getFullBlock")) {
                        return blocks.getOrDefault((BlockVector3) args[0], DIRT);
                    }
                    if (method.getName().equals("setBlock")) {
                        blocks.put((BlockVector3) args[0], ((BlockStateHolder<?>) args[1]).toBaseBlock());
                        writes++;
                        return true;
                    }
                    if (method.getName().equals("commit")) { return null; }
                    throw new AssertionError("Unexpected delegate call: " + method);
                });
    }

    private static void check(boolean passed, String message) {
        if (!passed) { throw new AssertionError(message); }
    }

    public static void main(String[] args) {
        WorldStub world = new WorldStub();
        int[] charges = {0};
        BufferedBrushExtent stroke = new BufferedBrushExtent(world.extent, 100, (changes, tooLarge) -> {
            check(world.writes == 0, "No world writes before payment");
            check(!tooLarge && changes.size() == 2, "Bill actual unique changed positions");
            check(changes.get(A).before().equals(DIRT), "Preserve original block for salvage");
            check(changes.get(A).after().equals(ROOTED), "Bill final randomized result, not intermediate writes");
            charges[0]++;
            return true;
        });
        stroke.setBlock(A, COARSE);
        stroke.setBlock(A.x(), A.y(), A.z(), ROOTED);
        stroke.setBlock(B, COARSE);
        check(stroke.getFullBlock(A).equals(ROOTED), "Algorithms see buffered writes");
        check(world.writes == 0 && charges[0] == 0, "Brush binding/capture must be free of writes/payments");
        stroke.commit();
        stroke.commit();
        check(world.writes == 2 && charges[0] == 1, "Flush/remember/close cannot double charge");
        check(world.blocks.get(A).equals(ROOTED), "Replay exact captured pattern result");

        WorldStub denied = new WorldStub();
        BufferedBrushExtent insufficient = new BufferedBrushExtent(denied.extent, 100, (changes, tooLarge) -> false);
        insufficient.setBlock(A, COARSE);
        insufficient.setBlock(B, ROOTED);
        insufficient.commit();
        check(denied.writes == 0, "Insufficient balance/Residence denial rejects entire stroke");

        int[] oversizedCalls = {0};
        BufferedBrushExtent oversized = new BufferedBrushExtent(denied.extent, 1, (changes, tooLarge) -> {
            check(tooLarge && changes.isEmpty(), "Oversized stroke discards all buffered writes");
            oversizedCalls[0]++;
            return false;
        });
        oversized.setBlock(A, COARSE);
        oversized.setBlock(B, ROOTED);
        oversized.commit();
        check(denied.writes == 0 && oversizedCalls[0] == 1, "Limit cannot partially paint for free");

        BufferedBrushExtent noop = new BufferedBrushExtent(denied.extent, 100, (changes, tooLarge) -> {
            throw new AssertionError("Unchanged stroke must not bill or add refund history");
        });
        check(!noop.setBlock(A, DIRT), "Ignore unchanged blocks");
        noop.setBlock(A, COARSE);
        noop.setBlock(A, DIRT);
        noop.commit();

        WorldStub protectedWorld = new WorldStub();
        BufferedBrushExtent protectedStroke = new BufferedBrushExtent(protectedWorld.extent, 100, (changes, tooLarge) -> {
            changes.remove(A);
            check(changes.size() == 1, "Exclude protected blocks before billing");
            return true;
        });
        protectedStroke.setBlock(A, COARSE);
        protectedStroke.setBlock(B, ROOTED);
        protectedStroke.commit();
        check(protectedWorld.writes == 1 && !protectedWorld.blocks.containsKey(A), "Protected blocks remain untouched");

        WorldStub failed = new WorldStub();
        BufferedBrushExtent failure = new BufferedBrushExtent(failed.extent, 100, (changes, tooLarge) -> {
            throw new IllegalStateException("Economy service unavailable");
        });
        failure.setBlock(A, COARSE);
        try { failure.commit(); throw new AssertionError("Expected service failure"); }
        catch (IllegalStateException expected) { }
        failure.commit();
        check(failed.writes == 0, "Service failures stay closed even on a later close/flush");
        System.out.println("PASS: buffered brush payment, exact replay, denial, limit, no-op, protection, failure, repeated flush");
    }
}
