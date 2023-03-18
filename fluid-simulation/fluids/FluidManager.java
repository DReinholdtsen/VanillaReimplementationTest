package org.example.FluidSim;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.timer.TaskSchedule;

import java.util.HashSet;

public class FluidManager {
    public static FluidPlacementRule waterPlacementRule = new FluidPlacementRule(Block.WATER);
    public static FluidPlacementRule lavaPlacementRule = new FluidPlacementRule(Block.LAVA);
    public static final Tag<Long> lastScheduled = Tag.Long("f");
    public static int tickCount = 0;
    public static int checkCount = 0;
    public static final SchedulerManager scheduler = MinecraftServer.getSchedulerManager();
    public static final Fluid waterFluid = new WaterFluid();
    public static final Fluid lavaFluid = new LavaFluid();
    public static final Fluid emptyFluid = new EmptyFluid();

    public static HashSet<Point> flowingPoints = new HashSet<>();
    public static void createFluid(Point pos, Block block, Instance instance) {
        instance.setBlock(pos, block);
        //TODO: this is literally useless btw, just too lazy to remove it :)
    }
    public static Fluid getFluid(Block block) {
        switch (block.name()) {
            case "minecraft:water":
                return waterFluid;
            case "minecraft:lava":
                return lavaFluid;
            default:
                return emptyFluid;
        }
    }
    public static void init(ServerProcess process) {
        process.block().registerBlockPlacementRule(waterPlacementRule);
        process.block().registerBlockPlacementRule(lavaPlacementRule);
    }
    public static void scheduleTick(Instance instance, Point pos, Block block) {
        Fluid fluid = getFluid(block);
        flowingPoints.add(pos);
        scheduler.buildTask(() -> {
            flowingPoints.remove(pos);
            getFluid(block).tick(instance, pos, block);
        }).delay(TaskSchedule.tick(fluid.getTickRate(instance))).schedule();
    }
}
