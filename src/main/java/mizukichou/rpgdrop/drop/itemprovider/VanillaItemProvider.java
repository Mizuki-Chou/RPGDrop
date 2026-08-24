package mizukichou.rpgdrop.drop.itemprovider;

import mizukichou.rpgdrop.drop.DropItem;
import mizukichou.rpgdrop.drop.ItemType;
import mizukichou.rpgdrop.drop.VanillaDropItem;
import org.bukkit.inventory.ItemStack;

/**
 * 原版物品 Provider：从 Material 生成全新 ItemStack。
 */
public final class VanillaItemProvider implements ItemProvider {

    @Override
    public ItemType type() {
        return ItemType.VANILLA;
    }

    @Override
    public ItemStack create(DropItem item) {
        if (!(item instanceof VanillaDropItem vanilla)) {
            throw new IllegalArgumentException("VanillaItemProvider can only handle VanillaDropItem");
        }
        return new ItemStack(vanilla.material());
    }
}
