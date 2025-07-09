package com.gregtechceu.gtceu.core.mixins;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GrowingPlantBlock.class)
public interface GrowingPlantBlockAccessor {

    @Accessor("growthDirection")
    Direction getGrowthDirection();

    @Invoker("getHeadBlock")
    GrowingPlantHeadBlock getHeadBlock();

    @Invoker("getBodyBlock")
    Block getBodyBlock();
}
