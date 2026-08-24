package mizukichou.nekodrop.drop;

import org.bukkit.Material;

/**
 * 原版物品掉落描述：只保存 Material，不保存 ItemStack。
 */
public record VanillaDropItem(Material material) implements DropItem {

    public VanillaDropItem {
        if (material == null) {
            throw new IllegalArgumentException("material must not be null");
        }
        if (material.isAir()) {
            throw new IllegalArgumentException("material must not be air");
        }
    }

    @Override
    public ItemType type() {
        return ItemType.VANILLA;
    }
}
