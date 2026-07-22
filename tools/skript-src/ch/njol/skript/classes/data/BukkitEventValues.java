/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.event.block.BeaconEffectEvent
 *  io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent
 *  io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.event.block.BlockDropItemEvent
 *  org.bukkit.event.entity.VillagerCareerChangeEvent
 *  org.bukkit.event.player.PlayerExpCooldownChangeEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffectType
 */
package ch.njol.skript.classes.data;

import ch.njol.skript.util.BlockStateBlock;
import ch.njol.skript.util.Timespan;
import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.player.PlayerExpCooldownChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.skriptlang.skript.bukkit.lang.eventvalue.EventValueRegistry;

public final class BukkitEventValues {
    private static final ItemStack AIR_IS = new ItemStack(Material.AIR);

    /*
     * Exception decompiling
     */
    public static void register(EventValueRegistry registry) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.UnsupportedOperationException
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:455)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:409)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:167)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.applyExpressionRewriterToArgs(StaticFunctionInvokation.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.applyExpressionRewriter(StaticFunctionInvokation.java:90)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement.rewriteExpressions(StructuredExpressionStatement.java:70)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private static /* synthetic */ Villager.Profession lambda$register$113(VillagerCareerChangeEvent event) {
        return event.getEntity().getProfession();
    }

    private static /* synthetic */ void lambda$register$112(VillagerCareerChangeEvent event, Villager.Profession profession) {
        if (profession == null) {
            return;
        }
        event.setProfession(profession);
    }

    private static /* synthetic */ Timespan lambda$register$111(WorldBorderBoundsChangeFinishEvent event) {
        return new Timespan((long)event.getDuration());
    }

    private static /* synthetic */ Timespan lambda$register$110(WorldBorderBoundsChangeEvent event) {
        return new Timespan(event.getDuration());
    }

    private static /* synthetic */ PotionEffectType lambda$register$109(BeaconEffectEvent event) {
        return event.getEffect().getType();
    }

    private static /* synthetic */ Timespan lambda$register$108(PlayerExpCooldownChangeEvent event) {
        return new Timespan(Timespan.TimePeriod.TICK, event.getPlayer().getExpCooldown());
    }

    private static /* synthetic */ Timespan lambda$register$107(PlayerExpCooldownChangeEvent event) {
        return new Timespan(Timespan.TimePeriod.TICK, event.getNewCooldown());
    }

    private static /* synthetic */ Entity[] lambda$register$105(BlockDropItemEvent event) {
        return (Entity[])event.getItems().toArray(Entity[]::new);
    }

    private static /* synthetic */ ItemStack[] lambda$register$103(BlockDropItemEvent event) {
        return (ItemStack[])event.getItems().stream().map(Item::getItemStack).toArray(ItemStack[]::new);
    }

    private static /* synthetic */ Block lambda$register$102(BlockDropItemEvent event) {
        return new BlockStateBlock(event.getBlockState());
    }
}

