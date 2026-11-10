package com.sworldcraft.waystones;


import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class WaystoneBlock extends Block {
    public WaystoneBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.LOWER));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(BlockStateProperties.DOUBLE_BLOCK_HALF);
    }
    static BlockPos base(BlockState state,BlockPos pos) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.UPPER ? pos.below() : pos;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos=context.getClickedPos(); Level level=context.getLevel();
        return pos.getY()<level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)
            ? defaultBlockState() : null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack) {
        level.setBlock(pos.above(),state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER),3);
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            var store=WaystoneStore.get(server.getServer());
            var stone=store.place(server,pos,player.getUUID());
            store.discover(player.getUUID(),stone);
            WaystoneMenus.openRename(player,stone,true);
        }
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,
            Direction direction,BlockPos neighborPos,BlockState neighborState,RandomSource random) {
        boolean lower=state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER;
        if (direction==(lower ? Direction.UP : Direction.DOWN)
            && (!neighborState.is(this) || neighborState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,level,ticks,pos,direction,neighborPos,neighborState,random);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp && !player.isSpectator()) {
            var store=WaystoneStore.get(server.getServer()); BlockPos base=base(state,pos);
            var stone=store.at(server,base);
            // A command-placed block has no owner yet: first real visitor claims it.
            if (stone==null) stone=store.place(server,base,sp.getUUID());
            if (store.discover(sp.getUUID(),stone)) sp.sendOverlayMessage(Component.literal("Waystone découverte : "+stone.name));
            WaystoneMenus.open(sp,stone,0);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved) {
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        BlockPos base=base(state,pos);
        // Removal also covers explosions and commands; replacing one half invalidates the stone.
        var registry=WaystoneStore.get(level.getServer());
        var stone=registry.at(level,base);
        if(stone!=null) registry.remove(stone.id);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide() && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.UPPER) {
            BlockPos below=pos.below();
            if(level.getBlockState(below).is(this)) level.destroyBlock(below,!player.hasInfiniteMaterials(),player);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER
            ? Block.box(1,0,1,15,16,15) : Block.box(2,0,2,14,14,14);
    }
}
