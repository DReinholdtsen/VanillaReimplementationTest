package io.github.togar2.fluids;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;

public class EmptyFluid extends FlowingFluid {
    public EmptyFluid() {
        super(Block.AIR, Material.BUCKET);
    }

    @Override
    public int getTickRate(Instance instance) {
        return 5;
    }
    public int getLevelDecreasePerBlock(Instance instance) {
        return 1;
    }
    protected boolean canBeReplacedWith(Instance instance, Point point, Fluid other, Direction direction) {
        return true;
    }
    public int getHoleRadius(Instance instance) {
        return 4;
    }
    public boolean isInfinite() {
        return false;
    }
    public boolean isEmpty() {
        return true;
    }

}
