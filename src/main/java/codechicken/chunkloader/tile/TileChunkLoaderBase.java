package codechicken.chunkloader.tile;

import codechicken.chunkloader.ChickenChunks;
import codechicken.chunkloader.api.IChickenChunkLoader;
import codechicken.chunkloader.client.TileChunkLoaderRenderer.RenderInfo;
import codechicken.chunkloader.init.ModBlocks;
import codechicken.chunkloader.manager.ChunkLoaderManager;
import codechicken.lib.vec.BlockCoord;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public abstract class TileChunkLoaderBase extends TileEntity implements ITickable, IChickenChunkLoader {

    public String owner;
    protected boolean loaded = false;
    protected boolean powered = false;
    public RenderInfo renderInfo;
    public boolean active = false;

    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("powered", powered);
        if (owner != null) {
            tag.setString("owner", owner);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("owner")) {
            owner = tag.getString("owner");
        }
        if (tag.hasKey("powered")) {
            powered = tag.getBoolean("powered");
        }
        loaded = true;
    }

    public void validate() {
        super.validate();
        if (!worldObj.isRemote && loaded && !powered) {
            activate();
        }

        if (worldObj.isRemote) {
            renderInfo = new RenderInfo();
        }
    }

    public boolean isPowered() {
        for (EnumFacing face : EnumFacing.VALUES) {
            boolean isPowered = isPoweringTo(worldObj, getPos().offset(face), face);
            if (isPowered) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPoweringTo(World world, BlockPos pos, EnumFacing side) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().getWeakPower(world, pos, state, side) > 0;
    }

    public void invalidate() {
        super.invalidate();
        if (!worldObj.isRemote) {
            deactivate();
        }
    }

    public void destroyBlock() {
        ModBlocks.blockChunkLoader.dropBlockAsItem(worldObj, getPos(), worldObj.getBlockState(pos), 0);
        worldObj.setBlockToAir(getPos());
    }

    public ChunkCoordIntPair getChunkPosition() {
        return new ChunkCoordIntPair(getPos().getX() >> 4, getPos().getZ() >> 4);
    }

    public void onBlockPlacedBy(EntityLivingBase entityliving) {
        if (entityliving instanceof EntityPlayer) {
            owner = entityliving.getName();
        }
        if (owner.equals("")) {
            owner = null;
        }
        activate();
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public Object getMod() {
        return ChickenChunks.instance;
    }

    @Override
    public World getWorld() {
        return worldObj;
    }

    @Override
    public BlockCoord getPosition() {
        return new BlockCoord(this);
    }

    @Override
    public void deactivate() {
        loaded = true;
        active = false;
        ChunkLoaderManager.remChunkLoader(this);
        worldObj.markBlockForUpdate(getPos());
    }

    public void activate() {
        loaded = true;
        active = true;
        ChunkLoaderManager.addChunkLoader(this);
        worldObj.markBlockForUpdate(getPos());
    }

    @Override
    public void update() {
        if (!worldObj.isRemote) {
            boolean nowPowered = isPowered();
            if (powered != nowPowered) {
                powered = nowPowered;
                if (powered) {
                    deactivate();
                } else {
                    activate();
                }
            }
        } else {
            renderInfo.update(this);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}