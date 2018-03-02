package com.griefcraft.bukkit;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public abstract interface NMS extends Block {

	public static final int ENTITY_BLOCK_ID = 5000;
	public static final int POSITION_OFFSET = 50000;

	public abstract int getX();

	public abstract int getY();

	public abstract int getZ();

	public abstract int getTypeId();

	public abstract World getWorld();

	public abstract Entity getEntity();
}