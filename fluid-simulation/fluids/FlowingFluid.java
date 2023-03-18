package org.example.FluidSim;

import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.gamedata.tags.TagManager;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;

import java.util.EnumMap;
import java.util.Map;

public abstract class FlowingFluid extends Fluid {
    public Block blockType;
    public FlowingFluid(Block block, Material bucket) {
        super(block, bucket);
        this.blockType = block;
    }
    @Override
    public void tick(Instance instance, Point pos, Block block) {
        //System.out.println("ticked");
        if (!block.compare(blockType)) {
            //System.out.println("Fluid changed");
            return;
        }
        if (isSource(block)) {
            //System.out.println("Source block");
        }
        else {
            //System.out.println("Not a source block");
            Block updated = getUpdatedState(instance, pos, block);
            if (FluidManager.getFluid(updated).isEmpty()) {
                instance.setBlock(pos, Block.AIR);
                System.out.println("Fluid removed");
                return;
            } else if (getLevel(updated) != getLevel(block)) {
                block = updated;
                instance.setBlock(pos, updated.withTag(FluidManager.lastScheduled, instance.getWorldAge()));//.withTag(FluidManager.currentlyFlowing, false));
            }
        }
        tryFlow(instance, pos, block);
    }
    protected void tryFlow(Instance instance, Point point, Block block) {
        //System.out.println("Trying to flow");
        Fluid fluid = this;
        if (fluid.isEmpty()) {
            //System.out.println("Empty fluid");
            return;
        }
        Point down = point.add(0, -1, 0);
        Block downBlock = instance.getBlock(down);
        Block updatedDownFluid = getUpdatedState(instance, down, downBlock);
        if (canFlow(instance, point, block, Direction.DOWN, down, downBlock, updatedDownFluid)) {
            //System.out.println("Flowing down");
            flow(instance, down, downBlock, updatedDownFluid);
            if (getAdjacentSourceCount(instance, point) >= 3) {
                flowSides(instance, point, block);
            }
        } else if (isSource(block) || !canFlowDown(instance, updatedDownFluid, point, block, down, downBlock)) {
            //System.out.println("Flowing sides");
            flowSides(instance, point, block);
        }
    }
    protected void flow(Instance instance, Point point, Block block, Block newBlock) {

        if (block.isAir()) instance.setBlock(point, newBlock.withTag(FluidManager.lastScheduled, instance.getWorldAge()));
    }
    private void flowSides(Instance instance, Point point, Block block) {
        int newLevel = getLevel(block) - getLevelDecreasePerBlock(instance);
        if (isFalling(block)) newLevel = 7;
        if (newLevel <= 0) return;

        Map<Direction, Block> map = getSpread(instance, point, block);
        for (Map.Entry<Direction, Block> entry : map.entrySet()) {
            Direction direction = entry.getKey();
            Block newBlock = entry.getValue();
            Point offset = point.add(direction.normalX(), direction.normalY(), direction.normalZ());
            Block currentBlock = instance.getBlock(offset);
            if (!canFlow(instance, point, block, direction, offset, currentBlock, newBlock)) continue;
            flow(instance, offset, currentBlock, newBlock);
        }
    }
    private int getAdjacentSourceCount(Instance instance, Point point) {
        int i = 0;
        for (Direction direction : Direction.HORIZONTAL) {
            Point currentPoint = point.add(direction.normalX(), direction.normalY(), direction.normalZ());
            Block block = instance.getBlock(currentPoint);
            if (!isMatchingAndStill(block)) continue;
            ++i;
        }
        return i;
    }
    protected Block getUpdatedState(Instance instance, Point point, Block block) {
        int highestLevel = 0;
        int stillCount = 0;
        for (Direction direction : Direction.HORIZONTAL) {
            Point directionPos = point.add(direction.normalX(), direction.normalY(), direction.normalZ());
            Block directionBlock = instance.getBlock(directionPos);
            Fluid directionFluid = FluidManager.getFluid(directionBlock);
            if (directionFluid != this)
                continue;

            if (isSource(directionBlock)) {
                stillCount++;
            }
            highestLevel = Math.max(highestLevel, getLevel(directionBlock));
        }

        if (isInfinite() && stillCount >= 2) {
            // If there's 2 or more still fluid blocks around
            // and below is still or a solid block, make this block still
            Block downBlock = instance.getBlock(point.add(0, -1, 0));
            if (downBlock.isSolid() || isMatchingAndStill(downBlock)) {
                return Block.WATER; //return source block
            }
        }

        Point above = point.add(0, 1, 0);
        Block aboveBlock = instance.getBlock(above);
        Fluid aboveFluid = FluidManager.getFluid(aboveBlock);
        if (aboveFluid == this) {
            return getFalling();
        }

        int newLevel = highestLevel - getLevelDecreasePerBlock(instance);
        if (newLevel <= 0) return Block.AIR;
        return getFlowing(newLevel);
    }
    protected Map<Direction, Block> getSpread(Instance instance, Point point, Block block) {
        int weight = 1000;
        EnumMap<Direction, Block> map = new EnumMap<>(Direction.class);
        Short2BooleanOpenHashMap holeMap = new Short2BooleanOpenHashMap();

        for (Direction direction : Direction.HORIZONTAL) {
            Point directionPoint = point.add(direction.normalX(), direction.normalY(), direction.normalZ());
            Block directionBlock = instance.getBlock(directionPoint);
            short id = FlowingFluid.getID(point, directionPoint);

            Block updatedBlock = getUpdatedState(instance, directionPoint, directionBlock);
            if (!canFlowThrough(instance, updatedBlock, point, block, direction, directionPoint, directionBlock))
                continue;

            boolean down = holeMap.computeIfAbsent(id, s -> {
                Point downPoint = directionPoint.add(0, -1, 0);
                return canFlowDown(
                        instance, getFlowing(getLevel(updatedBlock)),
                        directionPoint, directionBlock, downPoint, instance.getBlock(downPoint)
                );
            });

            int newWeight = down ? 0 : getWeight(instance, directionPoint, 1,
                    direction.opposite(), directionBlock, point, holeMap);
            if (newWeight < weight) map.clear();

            if (newWeight <= weight) {
                map.put(direction, updatedBlock);
                weight = newWeight;
            }
        }
        return map;
    }
    private boolean canFlowDown(Instance instance, Block flowing, Point point,
                                Block block, Point fromPoint, Block fromBlock) {
        if (FluidManager.getFluid(fromBlock) == this) return true;
        return this.canFill(instance, fromPoint, fromBlock, flowing);
    }
    private boolean canFlowThrough(Instance instance, Block flowing, Point point, Block block,
                                   Direction face, Point fromPoint, Block fromBlock) {
        return !isMatchingAndStill(fromBlock)
                && canFill(instance, fromPoint, fromBlock, flowing);
    }
    protected int getWeight(Instance instance, Point point, int initialWeight, Direction skipCheck,
                            Block block, Point originalPoint, Short2BooleanMap short2BooleanMap) {
        int weight = 1000;
        for (Direction direction : Direction.HORIZONTAL) {
            if (direction == skipCheck) continue;
            Point directionPoint = point.add(direction.normalX(), direction.normalY(), direction.normalZ());
            Block directionBlock = instance.getBlock(directionPoint);
            short id = FlowingFluid.getID(originalPoint, directionPoint);

            if (!canFlowThrough(instance, getFlowing(getLevel(block)), point, block,
                    direction, directionPoint, directionBlock)) continue;

            boolean down = short2BooleanMap.computeIfAbsent(id, s -> {
                Point downPoint = directionPoint.add(0, -1, 0);
                Block downBlock = instance.getBlock(downPoint);
                return canFlowDown(
                        instance, getFlowing(getLevel(block)),
                        directionPoint, downBlock, downPoint, downBlock
                );
            });
            if (down) return initialWeight;

            if (initialWeight < getHoleRadius(instance)) {
                int newWeight = getWeight(instance, directionPoint, initialWeight + 1,
                        direction.opposite(), directionBlock, originalPoint, short2BooleanMap);
                if (newWeight < weight) weight = newWeight;
            }
        }
        return weight;
    }
    private static short getID(Point point, Point point2) {
        int i = (int) (point2.x() - point.x());
        int j = (int) (point2.z() - point.z());
        return (short) ((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
    }
    private Block getFalling() {
        return blockType.withProperty("level", "8");
    }
    private boolean isMatchingAndStill(Block block) {
        return FluidManager.getFluid(block) == this && isSource(block);
    }
    public Block getFlowing(int level) {
        return defaultBlock.withProperty("level", String.valueOf(8-level));
    }
    protected boolean canFlow(Instance instance, Point fluidPoint, Block flowingBlock,
                              Direction flowDirection, Point flowTo, Block flowToBlock, Block newFlowing) {
        return FluidManager.getFluid(flowToBlock).canBeReplacedWith(instance, flowTo, FluidManager.getFluid(newFlowing), flowDirection)
                && canFill(instance, flowTo, flowToBlock, newFlowing);
    }
    private boolean canFill(Instance instance, Point point, Block block, Block flowing) {
        //TODO check waterloggable
        TagManager tags = MinecraftServer.getTagManager();
        if (block.compare(Block.LADDER)
                || block.compare(Block.SUGAR_CANE)
                || block.compare(Block.BUBBLE_COLUMN)
                || block.compare(Block.NETHER_PORTAL)
                || block.compare(Block.END_PORTAL)
                || block.compare(Block.END_GATEWAY)
                || block.compare(Block.KELP)
                || block.compare(Block.KELP_PLANT)
                || block.compare(Block.SEAGRASS)
                || block.compare(Block.TALL_SEAGRASS)
                || block.compare(Block.SEA_PICKLE)
                || tags.getTag(Tag.BasicType.BLOCKS, "minecraft:signs").contains(block.namespace())
                || block.name().contains("door")
                || block.name().contains("coral")) {
            return false;
        }
        return !block.isSolid();
    }
    public abstract int getTickRate(Instance instance);
    public abstract int getLevelDecreasePerBlock(Instance instance);

    public abstract boolean isInfinite();

    public abstract boolean isEmpty();
}
