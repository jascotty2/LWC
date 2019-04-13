package com.griefcraft.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;

public class LegacyMaterials {
	private static Map<Integer, Material> idToMaterial = new HashMap<>();
	private static Map<Material, Integer> materialToId = new HashMap<>();

	static {
		for (Material material : Material.values()) {
			if (material.isLegacy()) {
				idToMaterial.put(material.getId(), Bukkit.getUnsafe().fromLegacy(material));
			} else {
				Material legacy = Bukkit.getUnsafe().toLegacy(material);
				if (legacy != null) {
					materialToId.put(material, legacy.getId());
				}
			}
		}
	}

	public static Material getNewMaterial(int oldId) {
		return idToMaterial.get(oldId);
	}
	
	public static int getOldId(Material newMaterial) {
		return materialToId.getOrDefault(newMaterial, -1);
	}
}
