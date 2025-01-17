package io.github.togar2.fluids;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.Direction;

public abstract class Fluid {
    protected final Block defaultBlock;
    private final ItemStack bucket;
    public Fluid(Block block, Material bucket) {
        this.defaultBlock = block;
        this.bucket = ItemStack.of(bucket);
    }
    public abstract int getTickRate(Instance instance);

    public static boolean isSource(Block block) {
        String levelStr = block.getProperty("level");
        return levelStr == null || Integer.parseInt(levelStr) == 0;
    }
    public static int getLevel(Block block) {
        String levelStr = block.getProperty("level");
        if (levelStr == null) return 8;
        int level = 8-Integer.parseInt(levelStr);
        if (level == 0) return 8;
        return level;
    }
    public static boolean isFalling(Block block) {
        String levelStr = block.getProperty("level");
        if (levelStr == null) return false;
        return Integer.parseInt(levelStr) >= 8;
    }
    public abstract int getLevelDecreasePerBlock(Instance instance);
    public abstract int getHoleRadius(Instance instance);
    public abstract boolean isInfinite();
    public abstract boolean isEmpty();
    protected abstract boolean canBeReplacedWith(Instance instance, Point point,
                                                 Fluid other, Direction direction);
    public void tick(Instance instance, Point point, Block block) {
    }
}
