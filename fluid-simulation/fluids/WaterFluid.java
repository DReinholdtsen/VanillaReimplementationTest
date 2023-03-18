package org.example.FluidSim;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;

public class WaterFluid extends FlowingFluid {
    public WaterFluid() {
        super(Block.WATER, Material.WATER_BUCKET);
    }
    public int getTickRate(Instance instance) {
        return 10;
    }
    public int getLevelDecreasePerBlock(Instance instance) {
        return 1;
    }
    public boolean isInfinite() {
        return true;
    }
    public int getHoleRadius(Instance instance) {
        return 4;
    }
    public boolean isEmpty() {
        return false;
    }
    protected boolean canBeReplacedWith(Instance instance, Point point, Fluid other, Direction direction) {
        return direction == Direction.DOWN && this == other;
    }
}
