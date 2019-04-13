package com.griefcraft.bukkit;

import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class EntityBlock implements Block {
	
    public static final int ENTITY_BLOCK_ID = 5000;
    /**
     * To convert database offsets from Brokkonaut's fork in a foolproof manner, 
     * we're going to just flag any '5000' (unknown entity) as 6000, 
     * then convert them later when we encounter them
     */
    public static final int UNKNOWN_ENTITY_BLOCK_ID = 6000;
    public static final int POSITION_OFFSET = 50000;

    private final Entity entity;
    private final int id, hash;
    private final org.bukkit.World world;

    public EntityBlock(Entity entity) {
        this.entity = entity;
        if(entity != null) {
            id = entity.getType().getTypeId();
            hash = calcHash(entity.getUniqueId().hashCode());
            world = this.entity.getWorld();
        } else {
            id = hash = 0;
            world = null;
        }
    }
    
    public EntityBlock(String world, int id, int hash) {
        this.entity = null;
        this.id = id;
        this.hash = hash;
        this.world = Bukkit.getWorld(world);
    }

    /**
     * Get the entity represented by this protection block <br>
     * NOTE: only works for recently created protections, not loaded protections
     * @return 
     */
    public Entity getEntity() {
        return this.entity;
    }

    public EntityType getEntityType() {
        return entity != null ? entity.getType() : EntityType.fromId(id);
    }

    public static int calcHash(int hash) {
        return (POSITION_OFFSET + Math.abs(hash)) * (hash == 0 ? 1 : Integer.signum(hash));
    }

    @Override
    public int getX() {
        return hash;
    }

    @Override
    public int getY() {
        return hash;
    }

    @Override
    public int getZ() {
        return hash;
    }

    public static int calcTypeId(Entity entity) {
        final int typID = entity == null ? 0 : entity.getType().getTypeId();
        return typID <= 0 ? UNKNOWN_ENTITY_BLOCK_ID : typID + ENTITY_BLOCK_ID;
    }

    public int getTypeId() {
        // don't accidentally shoot ourselves in the foot
        final int typID = entity.getType().getTypeId();
        return typID <= 0 ? UNKNOWN_ENTITY_BLOCK_ID : typID + ENTITY_BLOCK_ID;
    }

    public org.bukkit.World getWorld() {
        return world;
    }
    
    public static Block getEntityBlock(Entity entity) {
        return new EntityBlock(entity);
    }

    public List<MetadataValue> getMetadata(String arg0) {
        return null;
    }

    public boolean hasMetadata(String arg0) {
        return false;
    }

    public void removeMetadata(String arg0, Plugin arg1) {
    }

    public void setMetadata(String arg0, MetadataValue arg1) {
    }

    public boolean breakNaturally() {
        return false;
    }

    public boolean breakNaturally(ItemStack arg0) {
        return false;
    }

    public Biome getBiome() {
        return null;
    }

    public int getBlockPower() {
        return 0;
    }

    public int getBlockPower(BlockFace arg0) {
        return 0;
    }

    public org.bukkit.Chunk getChunk() {
        return null;
    }

    public void setBlockData(BlockData data) {

    }

    public void setBlockData(BlockData data, boolean applyPhysics) {

    }

    public byte getData() {
        return 0;
    }

    public BlockData getBlockData() {
        return null;
    }

    public Collection<ItemStack> getDrops() {
        return null;
    }

    public Collection<ItemStack> getDrops(ItemStack arg0) {
        return null;
    }

    public BlockFace getFace(Block arg0) {
        return null;
    }

    public double getHumidity() {
        return 0.0D;
    }

    public byte getLightFromBlocks() {
        return 0;
    }

    public byte getLightFromSky() {
        return 0;
    }

    public byte getLightLevel() {
        return 0;
    }

    public Location getLocation() {
        return null;
    }

    public Location getLocation(Location arg0) {
        return null;
    }

    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    public Block getRelative(BlockFace arg0) {
        return null;
    }

    public Block getRelative(BlockFace arg0, int arg1) {
        return null;
    }

    public Block getRelative(int arg0, int arg1, int arg2) {
        return null;
    }

    public BlockState getState() {
        return null;
    }

    public double getTemperature() {
        return 0.0D;
    }

    public Material getType() {
        return null;
    }

    public boolean isBlockFaceIndirectlyPowered(BlockFace arg0) {
        return false;
    }

    public boolean isBlockFacePowered(BlockFace arg0) {
        return false;
    }

    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    public boolean isBlockPowered() {
        return false;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isLiquid() {
        return false;
    }

    public void setBiome(Biome arg0) {
    }

    public void setData(byte arg0) {
    }

    public void setData(byte arg0, boolean arg1) {
    }

    public void setType(Material arg0) {
    }

    public void setType(Material arg0, boolean arg1) {
    }

    public boolean setTypeId(int arg0) {
        return false;
    }

    public boolean setTypeId(int arg0, boolean arg1) {
        return false;
    }

    public boolean setTypeIdAndData(int arg0, byte arg1, boolean arg2) {
        return false;
    }
}
