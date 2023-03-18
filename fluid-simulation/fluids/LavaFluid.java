package org.example.FluidSim;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;

public class LavaFluid extends FlowingFluid {
    public LavaFluid() {
        super(Block.LAVA, Material.LAVA_BUCKET);
    }

    public int getTickRate(Instance instance) {
        return 30;
    }
    public int getLevelDecreasePerBlock(Instance instance) {
        return 2;
    }
    protected boolean canBeReplacedWith(Instance instance, Point point, Fluid other, Direction direction) {
        return true;
    }
    public int getHoleRadius(Instance instance) {
        return 2;
    }
    public boolean isInfinite() {
        return false;
    }
    public boolean isEmpty() {
        return false;
    }
}
