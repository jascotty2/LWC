package com.griefcraft.bukkit;

import com.destroystokyo.paper.block.BlockSoundGroup;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;

public class EntityBlock implements Block {

    @Deprecated
    public static final int ENTITY_BLOCK_ID = 5000;
    /**
     * To convert database offsets from Brokkonaut's fork in a foolproof manner,
     * we're going to just flag any '5000' (unknown entity) as 6000, then
     * convert them later when we encounter them
     */
    @Deprecated
    public static final int UNKNOWN_ENTITY_BLOCK_ID = 6000;
    public static final int POSITION_OFFSET = 50000;

    public static final String ENTITY_TYPE_PREFIX = "~ENTITY~";
    public static final String UNKNOWN_ENTITY_TYPE = ENTITY_TYPE_PREFIX + "~";

    private final Entity entity;
    private final int hash;
    private final String type;
    private final org.bukkit.World world;

    public EntityBlock(Entity entity) {
        this.entity = entity;
        if (entity != null) {
            //id = entity.getType().getTypeId();
            type = ENTITY_TYPE_PREFIX + entity.getType().name();
            hash = calcHash(entity.getUniqueId().hashCode());
            world = this.entity.getWorld();
        } else {
            hash = 0;
            type = UNKNOWN_ENTITY_TYPE;
            world = null;
        }
    }

    public EntityBlock(String world, String type, int hash) {
        this.entity = null;
        this.type = type;
        this.hash = hash;
        this.world = Bukkit.getWorld(world);
    }

    /**
     * Get the entity represented by this protection block <br>
     * NOTE: only works for recently created protections, not loaded protections
     *
     * @return
     */
    public Entity getEntity() {
        return this.entity;
    }

    public EntityType getEntityType() {
        return entity != null ? entity.getType()
                : (type.equals(UNKNOWN_ENTITY_TYPE) ? null : EntityType.valueOf(type.substring(ENTITY_TYPE_PREFIX.length())));
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

    public static String calcTypeString(Entity entity) {
        return entity == null ? UNKNOWN_ENTITY_TYPE : ENTITY_TYPE_PREFIX + entity.getType().name();
    }

    public static String calcTypeString(EntityType entity) {
        return entity == null ? UNKNOWN_ENTITY_TYPE : ENTITY_TYPE_PREFIX + entity.name();
    }

    public String getTypeString() {
        return type;
    }

    @Override
    public org.bukkit.World getWorld() {
        return world;
    }

    public static Block getEntityBlock(Entity entity) {
        return new EntityBlock(entity);
    }

    @Override
    public List<MetadataValue> getMetadata(String arg0) {
        return null;
    }

    @Override
    public boolean hasMetadata(String arg0) {
        return false;
    }

    @Override
    public void removeMetadata(String arg0, Plugin arg1) {
    }

    @Override
    public void setMetadata(String arg0, MetadataValue arg1) {
    }

    @Override
    public boolean breakNaturally() {
        return false;
    }

    @Override
    public boolean breakNaturally(ItemStack arg0) {
        return false;
    }

    @Override
    public Biome getBiome() {
        return null;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public int getBlockPower(BlockFace arg0) {
        return 0;
    }

    @Override
    public org.bukkit.Chunk getChunk() {
        return null;
    }

    @Override
    public void setBlockData(BlockData data) {
    }

    @Override
    public void setBlockData(BlockData data, boolean applyPhysics) {
    }

    @Override
    public byte getData() {
        return 0;
    }

    @Override
    public BlockData getBlockData() {
        return null;
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return null;
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack arg0) {
        return null;
    }

    @Override
    public BlockFace getFace(Block arg0) {
        return null;
    }

    @Override
    public double getHumidity() {
        return 0.0D;
    }

    @Override
    public byte getLightFromBlocks() {
        return 0;
    }

    @Override
    public byte getLightFromSky() {
        return 0;
    }

    @Override
    public byte getLightLevel() {
        return 0;
    }

    @Override
    public Location getLocation() {
        return entity != null ? entity.getLocation() : null;
    }

    @Override
    public Location getLocation(Location arg0) {
        return null;
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    @Override
    public Block getRelative(BlockFace arg0) {
        return null;
    }

    @Override
    public Block getRelative(BlockFace arg0, int arg1) {
        return null;
    }

    @Override
    public Block getRelative(int arg0, int arg1, int arg2) {
        return null;
    }

    @Override
    public BlockState getState() {
        return null;
    }

    @Override
    public double getTemperature() {
        return 0.0D;
    }

    @Override
    public Material getType() {
        return null;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace arg0) {
        return false;
    }

    @Override
    public boolean isBlockFacePowered(BlockFace arg0) {
        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    @Override
    public boolean isBlockPowered() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isLiquid() {
        return false;
    }

    @Override
    public void setBiome(Biome arg0) {
    }

    @Override
    public void setType(Material arg0) {
    }

    @Override
    public void setType(Material arg0, boolean arg1) {
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public RayTraceResult rayTrace(Location lctn, Vector vector, double d, FluidCollisionMode fcm) {
        return null;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public VoxelShape getCollisionShape() {
        return null;
    }

    @Override
    public boolean canPlace(BlockData blockData) {
        return false;
    }

    @Override
    public boolean applyBoneMeal(BlockFace arg0) {
        return false;
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack arg0, Entity arg1) {
        return null;
    }

    @Override
    public boolean isPreferredTool(ItemStack itemStack) {
        return false;
    }

    @Override
    public float getBreakSpeed(Player player) {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return null;
    }

    @Override
    public long getBlockKey() {
        return Block.super.getBlockKey();
    }

    @Override
    public boolean isValidTool(ItemStack nnis) {
        return false;
    }

    @Override
    public BlockState getState(boolean bln) {
        return null;
    }

    @Override
    public Biome getComputedBiome() {
        return null;
    }

    @Override
    public boolean isBuildable() {
        return false;
    }

    @Override
    public boolean isBurnable() {
        return false;
    }

    @Override
    public boolean isReplaceable() {
        return false;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean breakNaturally(boolean triggerEffect) {
        return false;
    }

    @Override
    public boolean breakNaturally(boolean bln, boolean bln1) {
        return false;
    }

    @Override
    public boolean breakNaturally(ItemStack tool, boolean triggerEffect) {
        return false;
    }

    @Override
    public boolean breakNaturally(ItemStack nnis, boolean bln, boolean bln1) {
        return false;
    }

    @Override
    public void tick() {
    }

    @Override
    public void fluidTick() {
    }

    @Override
    public void randomTick() {
    }

    @Override
    public BlockSoundGroup getSoundGroup() {
        return null;
    }

    @Override
    public SoundGroup getBlockSoundGroup() {
        return null;
    }

    @Override
    public float getDestroySpeed(ItemStack itemStack) {
        return 1.0f;
    }

    @Override
    public float getDestroySpeed(ItemStack itemStack, boolean considerEnchants) {
        return 1.0f;
    }

    @Override
    public String translationKey() {
        return null;
    }
}
