package work.mcwws.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;

/**
 * 预扫描用的 FAWE EditSession：算法走官方 {@code makeWalls}/{@code makeCylinder} 等，
 * 但 {@link EstimateCountExtent} 接走写块，不落盘。
 */
final class EstimateDrySession {

    @FunctionalInterface
    interface Body {
        void run(EditSession session, Extent counter) throws WorldEditException, Exception;
    }

    private EstimateDrySession() {
    }

    static void run(World world, FeeEstimate.ResultBuilder builder, Body body) {
        EstimateCountExtent counter = new EstimateCountExtent(world, builder, true);
        EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                .world(world)
                .maxBlocks(-1)
                .limitUnlimited()
                .changeSetNull()
                .fastMode(true)
                .checkMemory(false)
                .allowedRegionsEverywhere()
                .build();
        try {
            session.setFastMode(true);
            session.disableHistory(true);
            session.setExtent(counter);
            body.run(session, counter);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                session.close();
            } catch (Throwable ignored) {
                // 干跑会话 close 可能因 actor 为空打印失败信息，不能让预扫描把世界冲掉
            }
        }
    }
}
